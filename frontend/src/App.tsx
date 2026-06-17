import { Routes, Route } from 'react-router-dom'
import Dashboard from '@/pages/Dashboard'

/**
 * Root application component.
 * Routes are defined here and will expand as pages are built.
 */
function App() {
  return (
    <Routes>
      <Route path="/" element={<Dashboard />} />
      <Route path="/dashboard" element={<Dashboard />} />
      {/* Phase 1: <Route path="/upload" element={<UploadReceipt />} /> */}
      {/* Phase 2: <Route path="/gmail" element={<GmailSync />} /> */}
    </Routes>
  )
}

export default App
