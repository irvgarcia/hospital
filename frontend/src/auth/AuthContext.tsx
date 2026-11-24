import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getTokens, logout as apiLogout, onTokensChange, setTokens } from '../api/client'
import { accountApi, authApi } from '../api/endpoints'
import type { Account, Role } from '../api/types'

interface AuthState {
  account: Account | null
  loading: boolean
  isAuthenticated: boolean
  hasRole: (...roles: Role[]) => boolean
  login: (email: string, password: string) => Promise<Account>
  register: (data: { email: string; firstName: string; lastName: string; password: string }) => Promise<Account>
  logout: () => Promise<void>
  refreshAccount: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [hasTokens, setHasTokens] = useState(() => getTokens() !== null)

  useEffect(() => onTokensChange((tokens) => {
    setHasTokens(tokens !== null)
    if (!tokens) queryClient.clear()
  }), [queryClient])

  const accountQuery = useQuery({
    queryKey: ['account'],
    queryFn: accountApi.get,
    enabled: hasTokens,
    staleTime: 5 * 60 * 1000,
    retry: false,
  })

  const loadAccount = useCallback(async () => {
    const account = await queryClient.fetchQuery({ queryKey: ['account'], queryFn: accountApi.get, staleTime: 0 })
    return account
  }, [queryClient])

  const value = useMemo<AuthState>(() => {
    const account = hasTokens ? accountQuery.data ?? null : null
    return {
      account,
      loading: hasTokens && accountQuery.isPending,
      isAuthenticated: account !== null,
      hasRole: (...roles) => !!account && roles.some((role) => account.roles.includes(role)),
      login: async (email, password) => {
        setTokens(await authApi.login(email, password))
        return loadAccount()
      },
      register: async (data) => {
        setTokens(await authApi.register(data))
        return loadAccount()
      },
      logout: async () => {
        await apiLogout()
        queryClient.clear()
      },
      refreshAccount: async () => {
        await loadAccount()
      },
    }
  }, [hasTokens, accountQuery.data, accountQuery.isPending, loadAccount, queryClient])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return context
}

/** Pagina de inicio segun el rol principal del usuario. */
// eslint-disable-next-line react-refresh/only-export-components
export function homePathFor(account: Account | null) {
  if (!account) return '/login'
  if (account.roles.includes('PATIENT') && !account.profileCompleted) return '/completar-perfil'
  return '/'
}
