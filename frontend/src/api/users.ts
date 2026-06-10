import { fetchWithAuth } from './apiClient'

export type EloHistoryPointDTO = {
  attemptId: number
  attemptDate: string
  puzzleRating: number
  eloChange: number
  resultingElo: number
}

export type UserProfileDTO = {
  firebaseUid: string
  username: string
  email: string
  eloRating: number
  puzzlesAttempted: number
  puzzlesSolved: number
  eloHistory: EloHistoryPointDTO[]
}

export type UserLeaderboardEntryDTO = {
  username: string
  eloRating: number
  dailyRankChange: number
  currentUser: boolean
}

type ApiErrorBody = {
  detail?: unknown
  message?: unknown
}

const isApiErrorBody = (value: unknown): value is ApiErrorBody => {
  return typeof value === 'object' && value !== null
}

const readApiErrorMessage = async (response: Response, fallbackMessage: string) => {
  try {
    if (response.headers.get('content-type')?.includes('application/json')) {
      const body = (await response.json()) as unknown

      if (isApiErrorBody(body)) {
        if (typeof body.detail === 'string' && body.detail.trim()) {
          return body.detail
        }

        if (typeof body.message === 'string' && body.message.trim()) {
          return body.message
        }
      }
    } else {
      const message = await response.text()

      if (message.trim()) {
        return message
      }
    }
  } catch {
    return fallbackMessage
  }

  return fallbackMessage
}

export const fetchCurrentUserProfile = async () => {
  const response = await fetchWithAuth('/api/users/me')

  if (!response.ok) {
    throw new Error(await readApiErrorMessage(response, 'Unable to load your profile.'))
  }

  return (await response.json()) as UserProfileDTO
}

export const updateCurrentUsername = async (username: string) => {
  const response = await fetchWithAuth('/api/users/me/username', {
    method: 'PATCH',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ username }),
  })

  if (!response.ok) {
    throw new Error(await readApiErrorMessage(response, 'Unable to update your username.'))
  }

  return (await response.json()) as UserProfileDTO
}

export const fetchLeaderboard = async () => {
  const response = await fetchWithAuth('/api/users')

  if (!response.ok) {
    throw new Error(await readApiErrorMessage(response, 'Unable to load the leaderboard.'))
  }

  return (await response.json()) as UserLeaderboardEntryDTO[]
}
