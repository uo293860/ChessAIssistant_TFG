import { useEffect, useState } from 'react'
import { fetchWithAuth } from '../api/apiClient'

type ProfilePageProps = {
  fallbackEmail?: string | null
  onBackToBoard: () => void
  onSignOut: () => Promise<void>
}

type UserProfileDTO = {
  firebaseUid: string
  username: string
  email: string
  eloRating: number
  eloHistory: EloHistoryPointDTO[]
}

type EloHistoryPointDTO = {
  attemptId: number
  attemptDate: string
  puzzleRating: number
  eloChange: number
  resultingElo: number
}

const CHART_WIDTH = 320
const CHART_HEIGHT = 140
const CHART_PADDING = 18

const buildEloChartPolyline = (history: EloHistoryPointDTO[]) => {
  if (history.length === 0) {
    return ''
  }

  const ratings = history.map((point) => point.resultingElo)
  const minRating = Math.min(...ratings)
  const maxRating = Math.max(...ratings)
  const ratingRange = Math.max(1, maxRating - minRating)
  const innerWidth = CHART_WIDTH - CHART_PADDING * 2
  const innerHeight = CHART_HEIGHT - CHART_PADDING * 2
  const stepCount = Math.max(1, history.length - 1)

  return history
    .map((point, index) => {
      const x = CHART_PADDING + (innerWidth * index) / stepCount
      const y = CHART_PADDING + innerHeight - ((point.resultingElo - minRating) / ratingRange) * innerHeight
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

export function ProfilePage({ fallbackEmail, onBackToBoard, onSignOut }: ProfilePageProps) {
  const [profile, setProfile] = useState<UserProfileDTO | null>(null)
  const [isProfileLoading, setIsProfileLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    const loadProfile = async () => {
      setIsProfileLoading(true)
      setError('')

      try {
        const response = await fetchWithAuth('/api/users/me')

        if (!response.ok) {
          throw new Error('Unable to load your profile.')
        }

        setProfile((await response.json()) as UserProfileDTO)
      } catch (profileError) {
        setError(profileError instanceof Error ? profileError.message : 'Unable to load your profile.')
      } finally {
        setIsProfileLoading(false)
      }
    }

    void loadProfile()
  }, [])

  const username = profile?.username ?? fallbackEmail?.split('@')[0] ?? 'Player'
  const email = profile?.email ?? fallbackEmail ?? 'No email available'
  const eloRating = profile?.eloRating ?? 1000
  const eloHistory = profile?.eloHistory ?? []
  const chartPolyline = buildEloChartPolyline(eloHistory)
  const latestAttempt = eloHistory.length > 0 ? eloHistory[eloHistory.length - 1] : null
  const latestChange = latestAttempt?.eloChange ?? 0

  return (
    <main className="profile-shell">
      <section className="profile-header">
        <div>
          <p className="panel-title">Player profile</p>
          <h1>{username}</h1>
        </div>
        <div className="profile-actions">
          <button type="button" className="secondary-action compact-action" onClick={onBackToBoard}>
            Board
          </button>
          <button type="button" className="secondary-action compact-action" onClick={() => void onSignOut()}>
            Sign out
          </button>
        </div>
      </section>

      <section className="profile-layout">
        <div className="profile-summary">
          <span className="profile-avatar" aria-hidden="true">
            {username.slice(0, 1).toUpperCase()}
          </span>
          <div>
            <p className="panel-title">Username</p>
            <strong>{username}</strong>
          </div>
        </div>

        <div className="profile-stat">
          <p className="panel-title">Elo rating</p>
          <strong>{isProfileLoading ? 'Loading...' : eloRating}</strong>
        </div>

        <div className="profile-stat">
          <p className="panel-title">Email</p>
          <strong>{email}</strong>
        </div>

        <div className="profile-stat profile-chart">
          <div className="profile-chart-header">
            <div>
              <p className="panel-title">Elo progression</p>
              <strong>{eloHistory.length === 0 ? 'No attempts yet' : `${eloHistory.length} solved puzzles`}</strong>
            </div>
            {latestAttempt ? (
              <span className={latestChange >= 0 ? 'elo-delta positive' : 'elo-delta negative'}>
                {latestChange >= 0 ? '+' : ''}
                {latestChange}
              </span>
            ) : null}
          </div>

          <div className="elo-chart" aria-label="Elo progression chart">
            {chartPolyline ? (
              <svg viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} role="img">
                <polyline className="elo-chart-line" points={chartPolyline} />
              </svg>
            ) : (
              <p className="panel-copy">Solve puzzles to build your Elo history.</p>
            )}
          </div>
        </div>

        {error ? <p className="feedback error profile-feedback">{error}</p> : null}
      </section>
    </main>
  )
}
