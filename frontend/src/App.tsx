import { Routes, Route } from 'react-router-dom'
import AppShell from '@/components/layout/AppShell'
import Home from '@/pages/Home'
import UploadPage from '@/pages/Upload'
import Coach from '@/pages/Coach'
import Analytics from '@/pages/Analytics'
import Activities from '@/pages/Activities'

import Settings from '@/pages/Settings'

function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route path="/" element={<Home />} />
        <Route path="/upload" element={<UploadPage />} />
        <Route path="/coach" element={<Coach />} />
        <Route path="/insights" element={<Analytics />} />
        <Route path="/history" element={<Activities />} />
        <Route path="/settings" element={<Settings />} />
      </Route>
    </Routes>
  )
}

export default App
