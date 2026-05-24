import { auth } from '../firebase'
import { getConfigValue } from '../config'

const API_BASE_URL = getConfigValue('VITE_API_BASE_URL', { allowEmpty: true }) ?? 'http://localhost:8080'

export const getUserToken = async () => {
  const user = auth.currentUser

  if (!user) {
    throw new Error('No authenticated user is available.')
  }

  return user.getIdToken()
}

export const buildApiUrl = (path: string) => {
  return `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`
}

export const fetchWithAuth = async (path: string, init: RequestInit = {}) => {
  const token = await getUserToken()
  const headers = new Headers(init.headers)
  headers.set('Authorization', `Bearer ${token}`)

  return fetch(buildApiUrl(path), {
    ...init,
    headers,
  })
}
