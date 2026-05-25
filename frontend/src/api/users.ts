import { fetchWithAuth } from './apiClient'

export type UserLeaderboardEntryDTO = {
  username: string
  eloRating: number
}

export const fetchLeaderboard = async () => {
  const response = await fetchWithAuth('/api/users')

  if (!response.ok) {
    throw new Error('Unable to load the leaderboard.')
  }

  return (await response.json()) as UserLeaderboardEntryDTO[]
}
