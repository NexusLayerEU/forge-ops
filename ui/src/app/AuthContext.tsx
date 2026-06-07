import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { apiClient } from '@/lib/api'

interface User {
  id: string
  username: string
  email: string
  fullName?: string
  roles: string[]
  mustChangePassword?: boolean
}

interface AuthContextValue {
  user: User | null
  token: string | null
  isLoading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  hasRole: (role: string) => boolean
  isAdmin: () => boolean
  isOperator: () => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('forgeops_token'))
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    setUser(null)
    setToken(null)
    localStorage.removeItem('forgeops_token')
    delete apiClient.defaults.headers.common['Authorization']
  }, [])

  useEffect(() => {
    const savedToken = localStorage.getItem('forgeops_token')
    if (!savedToken) {
      setIsLoading(false)
      return
    }
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`
    apiClient.get('/auth/me')
      .then(res => {
        setUser(res.data)
        setToken(savedToken)
      })
      .catch(() => logout())
      .finally(() => setIsLoading(false))
  }, [logout])

  const login = async (username: string, password: string) => {
    const res = await apiClient.post('/auth/login', { username, password })
    const { token: newToken, user: newUser } = res.data
    localStorage.setItem('forgeops_token', newToken)
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${newToken}`
    setToken(newToken)
    setUser(newUser)
  }

  const hasRole = (role: string) => user?.roles?.includes(role) ?? false
  const isAdmin = () => hasRole('ADMIN')
  const isOperator = () => hasRole('ADMIN') || hasRole('OPERATOR')

  return (
    <AuthContext.Provider value={{ user, token, isLoading, login, logout, hasRole, isAdmin, isOperator }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
