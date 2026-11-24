import type { ProblemDetail, TokenResponse } from './types'

// Cliente HTTP: agrega el access token, renueva el par con el refresh token cuando el access vence (401)
// y convierte las respuestas de error (ProblemDetail) en ApiError.

const BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')
const STORAGE_KEY = 'hospital.tokens'

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail || problem.title || 'Error inesperado')
    this.status = problem.status
    this.problem = problem
  }

  /** Mensaje por campo (errores de validacion del backend). */
  fieldErrors(): Record<string, string> {
    const map: Record<string, string> = {}
    for (const error of this.problem.errors ?? []) {
      if (!map[error.field]) map[error.field] = error.message
    }
    return map
  }
}

let tokens: TokenResponse | null = readTokens()
let refreshing: Promise<boolean> | null = null
const listeners = new Set<(tokens: TokenResponse | null) => void>()

function readTokens(): TokenResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as TokenResponse) : null
  } catch {
    return null
  }
}

export function getTokens() {
  return tokens
}

export function setTokens(next: TokenResponse | null) {
  tokens = next
  try {
    if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // almacenamiento no disponible (modo privado): la sesion dura lo que la pestana
  }
  listeners.forEach((listener) => listener(next))
}

export function onTokensChange(listener: (tokens: TokenResponse | null) => void) {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail = { status: response.status, title: response.statusText }
  try {
    const body = await response.json()
    problem = { ...problem, ...body }
  } catch {
    // respuesta sin cuerpo JSON
  }
  if (response.status >= 500 && !problem.detail) {
    problem.detail = 'El servidor no respondió correctamente; inténtalo más tarde'
  }
  return new ApiError(problem)
}

async function refreshTokens(): Promise<boolean> {
  if (!tokens?.refreshToken) return false
  if (!refreshing) {
    refreshing = (async () => {
      try {
        const response = await fetch(`${BASE_URL}/auth/refresh-token`, {
          method: 'POST',
          headers: { Authorization: `Bearer ${tokens?.refreshToken}` },
        })
        if (!response.ok) {
          setTokens(null)
          return false
        }
        setTokens((await response.json()) as TokenResponse)
        return true
      } catch {
        return false
      } finally {
        refreshing = null
      }
    })()
  }
  return refreshing
}

type Query = Record<string, string | number | boolean | null | undefined>

export interface RequestOptions {
  method?: string
  body?: unknown
  query?: Query
  /** No intenta renovar el token (login, registro). */
  anonymous?: boolean
}

function buildUrl(path: string, query?: Query) {
  const params = new URLSearchParams()
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null && value !== '') params.append(key, String(value))
  }
  const qs = params.toString()
  return `${BASE_URL}${path}${qs ? `?${qs}` : ''}`
}

async function rawRequest(path: string, options: RequestOptions, retry = true): Promise<Response> {
  const headers: Record<string, string> = {}
  const isForm = options.body instanceof FormData
  if (options.body !== undefined && !isForm) headers['Content-Type'] = 'application/json'
  if (!options.anonymous && tokens?.accessToken) headers.Authorization = `Bearer ${tokens.accessToken}`

  let response: Response
  try {
    response = await fetch(buildUrl(path, options.query), {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : isForm ? (options.body as FormData) : JSON.stringify(options.body),
    })
  } catch {
    throw new ApiError({ status: 0, title: 'Sin conexión', detail: 'No se pudo conectar con el servidor' })
  }

  if (response.status === 401 && retry && !options.anonymous && tokens?.refreshToken) {
    if (await refreshTokens()) return rawRequest(path, options, false)
  }
  if (response.status === 401 && !options.anonymous) {
    setTokens(null)
  }
  if (!response.ok) throw await toApiError(response)
  return response
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await rawRequest(path, options)
  if (response.status === 204) return undefined as T
  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

/** Descarga un archivo protegido (PDF, adjuntos) y lo guarda con el nombre que indica el servidor. */
export async function download(path: string, fallbackName = 'archivo') {
  const response = await rawRequest(path, {})
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const match = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disposition)
  const name = match ? decodeURIComponent(match[1] ?? match[2]) : fallbackName
  const blob = await response.blob()
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** Cierra la sesion en el backend (revoca los tokens) y la borra localmente. */
export async function logout() {
  const current = tokens
  setTokens(null)
  if (current?.accessToken) {
    try {
      await fetch(`${BASE_URL}/auth/logout`, { method: 'POST', headers: { Authorization: `Bearer ${current.accessToken}` } })
    } catch {
      // sin conexion: la sesion local ya se borro
    }
  }
}
