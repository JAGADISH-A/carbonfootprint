FROM python:3.10-slim

# Install system dependencies for OpenCV, OpenMP, and Poppler (PDF processing)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    poppler-utils \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy requirements and install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Pre-download and cache the PaddleOCR models during build time
# (Keeps container startup offline-ready and instant)


# Copy application code
COPY app.py .

# Expose port for FastAPI
EXPOSE 8001

# Docker Healthcheck
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

# Start the service
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8001"]
