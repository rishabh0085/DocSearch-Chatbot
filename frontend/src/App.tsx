import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './layouts/AppShell'
import { ChatPage } from './pages/ChatPage'
import { DocumentsPage } from './pages/DocumentsPage'
import { UploadPage } from './pages/UploadPage'
import { SearchPage } from './pages/SearchPage'
import { HistoryPage } from './pages/HistoryPage'
import { DocumentSummaryPage } from './pages/DocumentSummaryPage'
import { DocumentViewerPage } from './pages/DocumentViewerPage'

export default function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<Navigate replace to="/chat" />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="documents/:id/summary" element={<DocumentSummaryPage />} />
        <Route path="documents/:id/view" element={<DocumentViewerPage />} />
        <Route path="upload" element={<UploadPage />} />
        <Route path="search" element={<SearchPage />} />
        <Route path="history" element={<HistoryPage />} />
      </Route>
      <Route path="*" element={<Navigate replace to="/chat" />} />
    </Routes>
  )
}
