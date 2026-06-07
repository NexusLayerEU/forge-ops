import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './app/AuthContext'
import Layout from './app/Layout'

// Auth
import LoginPage from './features/auth/LoginPage'

// Dashboard
import DashboardPage from './features/dashboard/DashboardPage'

// Inventory
import NodesPage from './features/inventory/NodesPage'
import NodeDetailPage from './features/inventory/NodeDetailPage'
import GroupsPage from './features/inventory/GroupsPage'

// Forges
import ForgesPage from './features/forges/ForgesPage'
import ForgeEditorPage from './features/forges/ForgeEditorPage'
import ForgeDetailPage from './features/forges/ForgeDetailPage'

// Runs
import RunsPage from './features/runs/RunsPage'
import RunDetailPage from './features/runs/RunDetailPage'

// Drift
import DriftPage from './features/drift/DriftPage'
import DriftReportDetailPage from './features/drift/DriftReportDetailPage'

// Vault
import VaultPage from './features/vault/VaultPage'

// Audit
import AuditPage from './features/audit/AuditPage'

// Settings
import SettingsPage from './features/settings/SettingsPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth()
  if (isLoading) return <div className="flex items-center justify-center h-screen bg-background"><div className="text-text-secondary">Loading...</div></div>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function AppRoutes() {
  const { user } = useAuth()

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <Layout>
              <Routes>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/inventory/nodes" element={<NodesPage />} />
                <Route path="/inventory/nodes/:id" element={<NodeDetailPage />} />
                <Route path="/inventory/groups" element={<GroupsPage />} />
                <Route path="/forges" element={<ForgesPage />} />
                <Route path="/forges/new" element={<ForgeEditorPage />} />
                <Route path="/forges/:id" element={<ForgeDetailPage />} />
                <Route path="/forges/:id/edit" element={<ForgeEditorPage />} />
                <Route path="/runs" element={<RunsPage />} />
                <Route path="/runs/:id" element={<RunDetailPage />} />
                <Route path="/drift" element={<DriftPage />} />
                <Route path="/drift/reports/:id" element={<DriftReportDetailPage />} />
                <Route path="/vault" element={<VaultPage />} />
                <Route path="/audit" element={<AuditPage />} />
                <Route path="/settings" element={<SettingsPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Layout>
          </ProtectedRoute>
        }
      />
    </Routes>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  )
}
