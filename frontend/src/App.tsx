import { Routes, Route } from 'react-router-dom'
import AppShell from '@/components/layout/AppShell'
import Home from '@/pages/Home'
import UploadPage from '@/pages/Upload'
import Coach from '@/pages/Coach'
import Analytics from '@/pages/Analytics'
import Activities from '@/pages/Activities'

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Home />} />
        <Route path="/upload" element={<UploadPage />} />
        <Route path="/coach" element={<Coach />} />
        <Route path="/insights" element={<Analytics />} />
        <Route path="/history" element={<Activities />} />
        <Route path="/settings" element={<div className="page-container"><h1 className="text-2xl font-bold text-ink">Settings</h1><p className="text-ink-muted mt-2">Coming soon.</p></div>} />
      </Route>
    </Routes>
  )
}

export default App
