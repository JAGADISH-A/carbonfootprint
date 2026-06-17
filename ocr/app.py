import io
import os
import time
import uuid
import logging
from typing import Dict, Any, Optional, Generator
import numpy as np
from PIL import Image
import fitz  # PyMuPDF
import pdf2image

from fastapi import FastAPI, File, UploadFile, HTTPException, Request
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from paddleocr import PaddleOCR



# Configure logging to never output document text
logger = logging.getLogger("ocr-sidecar")
logger.setLevel(logging.INFO)
logger.handlers.clear()
handler = logging.StreamHandler()
handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s"))
logger.addHandler(handler)

# Global model state
ocr_model: Optional[PaddleOCR] = None
model_loaded = False

app = FastAPI(title="PaddleOCR Sidecar Service", version="1.0.0")

@app.middleware("http")
async def log_request(request: Request, call_next):
    print("=" * 60)
    print("METHOD :", request.method)
    print("PATH   :", request.url.path)
    print("CTYPE  :", request.headers.get("content-type"))
    response = await call_next(request)
    print("STATUS :", response.status_code)
    print("=" * 60)
    return response

@app.on_event("startup")
def startup_event():
    global ocr_model, model_loaded
    logger.info("Loading PaddleOCR models...")
    try:
        lang = os.getenv("PADDLE_OCR_LANG", "en")
        # Initialize PaddleOCR (cached globally so we don't load the model on every request)
        ocr_model = PaddleOCR(use_angle_cls=True, lang=lang, show_log=False)
        model_loaded = True
        logger.info("PaddleOCR initialized successfully.")
    except Exception as e:
        logger.error(f"Failed to initialize PaddleOCR models: {e}", exc_info=True)

# 1. Health Endpoint
@app.get("/health")
def health_endpoint():
    """Simple health check that does not run OCR."""
    if not model_loaded or ocr_model is None:
        return JSONResponse(
            status_code=503,
            content={
                "status": "DOWN",
                "modelLoaded": False,
                "version": "1.0.0"
            }
        )
    return {
        "status": "UP",
        "modelLoaded": True,
        "version": "1.0.0"
    }

# 2. Error Handlers
@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    logger.error(f"HTTP error occurred: {exc.status_code} - {exc.detail}")
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail}
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    logger.error(exc.errors())

    return JSONResponse(
        status_code=422,
        content={
            "detail": exc.errors()
        }
    )
@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled error occurred: {str(exc)}", exc_info=True)
    # Hide internal file paths and system details
    return JSONResponse(
        status_code=500,
        content={"detail": "An internal error occurred during OCR processing."}
    )

# 3. PDF page generators (memory-efficient, in-memory)
def extract_pdf_pages_fitz(file_bytes: bytes) -> Generator[Image.Image, None, None]:
    """Render PDF pages to PIL Images using PyMuPDF (fitz) entirely in memory."""
    doc = fitz.open(stream=file_bytes, filetype="pdf")
    for page_num in range(len(doc)):
        page = doc.load_page(page_num)
        pix = page.get_pixmap()
        img_data = pix.tobytes("png")
        img = Image.open(io.BytesIO(img_data)).convert("RGB")
        yield img

def extract_pdf_pages_pdf2image(file_bytes: bytes) -> Generator[Image.Image, None, None]:
    """Render PDF pages to PIL Images using pdf2image as a fallback."""
    images = pdf2image.convert_from_bytes(file_bytes)
    for img in images:
        yield img.convert("RGB")

# 4. OCR Endpoint
@app.post("/ocr")
async def ocr_endpoint(request: Request, file: UploadFile = File(...)):
    start_time = time.time()
    
    # Resolve Request ID for logging and tracing
    request_id = request.headers.get("x-request-id") or request.headers.get("x-correlation-id") or str(uuid.uuid4())
    
    mime_type = file.content_type or "application/octet-stream"
    
    try:
        file_bytes = await file.read()
        file_size = len(file_bytes)
    except Exception as e:
        logger.error(f"[{request_id}] Failed to read file bytes: {e}")
        raise HTTPException(status_code=400, detail="Failed to read uploaded file.")

    # Log Request Start
    logger.info(
        f"Request Start - Request ID: {request_id}, Filename: {file.filename}, Size: {file_size} bytes, MIME: {mime_type}"
    )

    # Validate File Size (Max 20MB)
    if file_size > 20 * 1024 * 1024:
        logger.error(f"[{request_id}] File size {file_size} exceeds 20MB limit.")
        raise HTTPException(status_code=413, detail="File too large. Maximum size is 20 MB.")
        
    SUPPORTED_MIME_TYPES = {
        "application/pdf",
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/webp"
    }
    if mime_type not in SUPPORTED_MIME_TYPES:
        logger.error(f"[{request_id}] Unsupported MIME type: {mime_type}")
        raise HTTPException(status_code=415, detail="Unsupported media type format.")
        
    filename = file.filename or ""
    ext = os.path.splitext(filename.lower())[1]
    SUPPORTED_EXTENSIONS = {".pdf", ".png", ".jpg", ".jpeg", ".webp"}
    if ext not in SUPPORTED_EXTENSIONS:
        logger.error(f"[{request_id}] Unsupported file extension: {ext}")
        raise HTTPException(status_code=415, detail="Unsupported file extension.")
        
    # Verify Model Readiness
    if not model_loaded or ocr_model is None:
        logger.error(f"[{request_id}] OCR model is not loaded or unavailable.")
        raise HTTPException(status_code=503, detail="OCR service model is loading or unavailable.")

    # Process PDF or Images sequentially
    pages = []
    is_pdf = (mime_type == "application/pdf" or ext == ".pdf")
    
    try:
        if is_pdf:
            try:
                pages = list(extract_pdf_pages_fitz(file_bytes))
            except Exception as fitz_err:
                logger.warning(f"[{request_id}] PyMuPDF failed: {fitz_err}. Falling back to pdf2image.")
                try:
                    pages = list(extract_pdf_pages_pdf2image(file_bytes))
                except Exception as fallback_err:
                    logger.error(f"[{request_id}] Both PDF renderers failed: {fallback_err}")
                    raise HTTPException(status_code=422, detail="Corrupted or unreadable PDF document.")
        else:
            try:
                img = Image.open(io.BytesIO(file_bytes)).convert("RGB")
                pages = [img]
            except Exception as img_err:
                logger.error(f"[{request_id}] Failed to open image: {img_err}")
                raise HTTPException(status_code=422, detail="Corrupted or unreadable image file.")
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"[{request_id}] Unexpected error rendering file: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal error rendering file.")

    page_count = len(pages)
    if page_count == 0:
        logger.error(f"[{request_id}] Document has 0 pages.")
        raise HTTPException(status_code=422, detail="Document contains no readable pages.")

    # Execute OCR
    page_texts = []
    total_confidence = 0.0
    confidence_count = 0
    
    for page_idx, img in enumerate(pages):
        img_array = np.array(img)
        try:
            res = ocr_model.ocr(img_array, cls=True)
        except Exception as ocr_err:
            logger.error(f"[{request_id}] PaddleOCR execution failure on page {page_idx + 1}: {ocr_err}")
            raise HTTPException(status_code=500, detail="OCR engine failure during processing.")
            
        if not res or res[0] is None:
            continue
            
        page_lines = []
        for line in res[0]:
            try:
                text_info = line[1]
                text = text_info[0]
                conf = float(text_info[1])
                page_lines.append(text)
                total_confidence += conf
                confidence_count += 1
            except (IndexError, TypeError, ValueError) as parse_err:
                logger.warning(f"[{request_id}] Error parsing OCR output line: {parse_err}")
                
        if page_lines:
            page_texts.append(" ".join(page_lines))

    # Calculate statistics
    processing_time = round((time.time() - start_time) * 1000, 2)
    
    if not page_texts:
        # Empty OCR Handling
        logger.info(
            f"Request Completion - Request ID: {request_id}, Processing Time: {processing_time} ms, Pages: {page_count}, Characters: 0, Average Confidence: 0.0, Status: SUCCESS (Empty)"
        )
        return {
            "text": "",
            "confidence": 0.0,
            "language": None,
            "pageCount": page_count
        }

    combined_text = "\n".join(page_texts)
    avg_confidence = round(total_confidence / confidence_count, 4) if confidence_count > 0 else 0.0
    lang = os.getenv("PADDLE_OCR_LANG", "en")

    # Log Request Completion
    logger.info(
        f"Request Completion - Request ID: {request_id}, Processing Time: {processing_time} ms, Pages: {page_count}, Characters: {len(combined_text)}, Average Confidence: {avg_confidence}, Status: SUCCESS"
    )

    return {
        "text": combined_text,
        "confidence": avg_confidence,
        "language": lang,
        "pageCount": page_count
    }
