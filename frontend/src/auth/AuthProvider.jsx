import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  loginRequest,
  logoutRequest,
  refreshRequest,
  registerRequest,
} from '../api/auth'
import { tokenStore } from '../api/client'
import { AuthContext } from './auth-context'

let sessionBootstrap = null

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)

  const applyAuth = useCallback((result) => {
    tokenStore.set(result.accessToken)
    setUser(result.user)
  }, [])

  const clearAuth = useCallback(() => {
    tokenStore.set(null)
    setUser(null)
  }, [])

  const login = useCallback(
    async (username, password) => {
      const result = await loginRequest(username, password)
      sessionBootstrap = null
      applyAuth(result)
    },
    [applyAuth],
  )

  const register = useCallback(
    async (payload) => {
      const result = await registerRequest(payload)
      sessionBootstrap = null
      applyAuth(result)
    },
    [applyAuth],
  )

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } finally {
      sessionBootstrap = null
      clearAuth()
    }
  }, [clearAuth])

  const hasRole = useCallback(
    (...roles) =>
      user !== null && roles.includes(user.role),
    [user],
  )

  useEffect(() => {
    let cancelled = false

    const bootstrap = async () => {
      try {
        if (!sessionBootstrap) {
          sessionBootstrap = refreshRequest()
        }
        const result = await sessionBootstrap
        if (!cancelled) {
          applyAuth(result)
        }
      } catch {
        if (!cancelled) {
          clearAuth()
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false)
        }
      }
    }

    void bootstrap()

    const onForcedLogout = () => {
      sessionBootstrap = null
      clearAuth()
    }
    window.addEventListener('auth:logout', onForcedLogout)

    return () => {
      cancelled = true
      window.removeEventListener('auth:logout', onForcedLogout)
    }
  }, [applyAuth, clearAuth])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: user !== null,
      isLoading,
      login,
      register,
      logout,
      hasRole,
    }),
    [user, isLoading, login, register, logout, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
