import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  changePasswordRequest,
  loginRequest,
  logoutRequest,
  refreshRequest,
  registerRequest,
  updateProfileRequest,
} from '../api/auth'
import { tokenStore } from '../api/client'
import { AuthContext } from './auth-context'

let sessionBootstrap = null

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const sessionVersion = useRef(0)

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
      sessionVersion.current += 1
      const result = await loginRequest(username, password)
      sessionBootstrap = null
      applyAuth(result)
      return result
    },
    [applyAuth],
  )

  const register = useCallback(
    async (payload) => {
      sessionVersion.current += 1
      const result = await registerRequest(payload)
      sessionBootstrap = null
      applyAuth(result)
      return result
    },
    [applyAuth],
  )

  const logout = useCallback(async () => {
    sessionVersion.current += 1
    try {
      await logoutRequest()
    } finally {
      sessionBootstrap = null
      clearAuth()
    }
  }, [clearAuth])

  const updateProfile = useCallback(async (payload) => {
    const updatedUser = await updateProfileRequest(payload)
    setUser(updatedUser)
    return updatedUser
  }, [])

  const changePassword = useCallback(async (payload) => {
    sessionVersion.current += 1
    await changePasswordRequest(payload)
    sessionBootstrap = null
    clearAuth()
  }, [clearAuth])

  const hasRole = useCallback(
    (...roles) =>
      user !== null && roles.includes(user.role),
    [user],
  )

  useEffect(() => {
    let cancelled = false

    const bootstrap = async () => {
      const version = sessionVersion.current
      try {
        if (!sessionBootstrap) {
          sessionBootstrap = refreshRequest()
        }
        const result = await sessionBootstrap
        if (!cancelled && version === sessionVersion.current) {
          applyAuth(result)
        }
      } catch {
        if (!cancelled && version === sessionVersion.current) {
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
      sessionVersion.current += 1
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
      updateProfile,
      changePassword,
      hasRole,
    }),
    [user, isLoading, login, register, logout, updateProfile, changePassword, hasRole],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
