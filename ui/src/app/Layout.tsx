import React from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard,
  Server,
  Layers,
  PlayCircle,
  AlertTriangle,
  Lock,
  FileText,
  Settings,
  LogOut,
  ChevronRight,
  Boxes,
} from 'lucide-react'
import { useAuth } from './AuthContext'
import { clsx } from 'clsx'

interface NavItem {
  label: string
  to: string
  icon: React.ReactNode
  children?: { label: string; to: string }[]
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard', to: '/', icon: <LayoutDashboard size={16} /> },
  {
    label: 'Inventory',
    to: '/inventory/nodes',
    icon: <Server size={16} />,
    children: [
      { label: 'Nodes', to: '/inventory/nodes' },
      { label: 'Groups', to: '/inventory/groups' },
    ],
  },
  { label: 'Forges', to: '/forges', icon: <Layers size={16} /> },
  { label: 'Runs', to: '/runs', icon: <PlayCircle size={16} /> },
  { label: 'Drift', to: '/drift', icon: <AlertTriangle size={16} /> },
  { label: 'Vault', to: '/vault', icon: <Lock size={16} /> },
  { label: 'Audit', to: '/audit', icon: <FileText size={16} /> },
  { label: 'Settings', to: '/settings', icon: <Settings size={16} /> },
]

function NavItemComponent({ item }: { item: NavItem }) {
  const [open, setOpen] = React.useState(false)

  if (item.children) {
    return (
      <div>
        <button
          onClick={() => setOpen(o => !o)}
          className="flex items-center gap-2.5 w-full px-3 py-2 rounded-md text-text-secondary hover:text-text-primary hover:bg-surface-elevated transition-colors text-sm"
        >
          {item.icon}
          <span className="flex-1 text-left">{item.label}</span>
          <ChevronRight size={12} className={clsx('transition-transform', open && 'rotate-90')} />
        </button>
        {open && (
          <div className="ml-6 mt-1 flex flex-col gap-0.5">
            {item.children.map(child => (
              <NavLink
                key={child.to}
                to={child.to}
                className={({ isActive }) =>
                  clsx(
                    'block px-3 py-1.5 rounded-md text-sm transition-colors',
                    isActive
                      ? 'text-primary-500 bg-blue-950'
                      : 'text-text-secondary hover:text-text-primary hover:bg-surface-elevated'
                  )
                }
              >
                {child.label}
              </NavLink>
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <NavLink
      to={item.to}
      end={item.to === '/'}
      className={({ isActive }) =>
        clsx(
          'flex items-center gap-2.5 px-3 py-2 rounded-md text-sm transition-colors',
          isActive
            ? 'text-primary-500 bg-blue-950'
            : 'text-text-secondary hover:text-text-primary hover:bg-surface-elevated'
        )
      }
    >
      {item.icon}
      <span>{item.label}</span>
    </NavLink>
  )
}

export default function Layout({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      {/* Sidebar */}
      <aside className="w-60 flex-shrink-0 flex flex-col bg-surface border-r border-border">
        {/* Logo */}
        <div className="flex items-center gap-2.5 px-4 py-4 border-b border-border">
          <div className="w-7 h-7 bg-primary-500 rounded-md flex items-center justify-center">
            <Boxes size={16} className="text-white" />
          </div>
          <span className="font-mono font-semibold text-text-primary tracking-tight">ForgeOps</span>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto px-2 py-3 flex flex-col gap-0.5">
          {NAV_ITEMS.map(item => (
            <NavItemComponent key={item.to} item={item} />
          ))}
        </nav>

        {/* User */}
        <div className="px-2 py-3 border-t border-border">
          <div className="flex items-center gap-2.5 px-3 py-2 rounded-md">
            <div className="w-7 h-7 rounded-full bg-primary-500 flex items-center justify-center text-xs font-semibold text-white">
              {user?.username?.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm text-text-primary truncate">{user?.username}</p>
              <p className="text-xs text-text-secondary truncate">{user?.roles?.[0]}</p>
            </div>
            <button
              onClick={handleLogout}
              className="text-text-secondary hover:text-danger transition-colors"
              title="Logout"
            >
              <LogOut size={14} />
            </button>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        {children}
      </main>
    </div>
  )
}
