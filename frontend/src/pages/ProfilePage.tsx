import { useEffect, useState } from 'react'
import type { CSSProperties } from 'react'
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
  puzzlesAttempted: number
  puzzlesSolved: number
  eloHistory: EloHistoryPointDTO[]
}

type EloHistoryPointDTO = {
  attemptId: number
  attemptDate: string
  puzzleRating: number
  eloChange: number
  resultingElo: number
}

type SuccessRateDonutProps = {
  solved: number
  failed: number
}

function SuccessRateDonut({ solved, failed }: SuccessRateDonutProps) {
  const totalAttempts = Math.max(solved + failed, 0)
  const solvedPercent = totalAttempts > 0 ? (solved / totalAttempts) * 100 : 0
  const chartStyle = {
    '--success-rate': `${solvedPercent}%`,
  } as CSSProperties

  return (
    <div
      className={`success-rate-donut ${totalAttempts === 0 ? 'empty' : ''}`}
      style={chartStyle}
      role="img"
      aria-label={`${solved} wins and ${failed} losses`}
    >
      <div className="success-rate-center">
        <strong>{solved} W</strong>
        <span>{failed} L</span>
      </div>
    </div>
  )
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
  const puzzlesAttempted = Math.max(profile?.puzzlesAttempted ?? 0, 0)
  const puzzlesSolved = Math.min(Math.max(profile?.puzzlesSolved ?? 0, 0), puzzlesAttempted)
  const puzzlesFailed = Math.max(puzzlesAttempted - puzzlesSolved, 0)
  const eloHistory = profile?.eloHistory ?? []
  const eloChartPolyline = buildEloChartPolyline(eloHistory)
  const latestEloChange = eloHistory.at(-1)?.eloChange ?? 0

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

        <div className="profile-stat success-rate-stat">
          <p className="panel-title">Success Rate</p>
          {isProfileLoading ? (
            <strong>Loading...</strong>
          ) : (
            <SuccessRateDonut solved={puzzlesSolved} failed={puzzlesFailed} />
          )}
        </div>

        <div className="profile-stat profile-chart">
          <div className="profile-chart-header">
            <div>
              <p className="panel-title">Elo trend</p>
              <strong>{isProfileLoading ? 'Loading...' : `${eloHistory.length} solved puzzles`}</strong>
            </div>
            {!isProfileLoading && eloHistory.length > 0 ? (
              <span className={`elo-delta ${latestEloChange >= 0 ? 'positive' : 'negative'}`}>
                {latestEloChange >= 0 ? `+${latestEloChange}` : latestEloChange}
              </span>
            ) : null}
          </div>

          <div className="elo-chart">
            {isProfileLoading ? (
              <span>Loading chart...</span>
            ) : eloChartPolyline ? (
              <svg viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`} role="img" aria-label="Elo rating history">
                <polyline className="elo-chart-line" points={eloChartPolyline} />
              </svg>
            ) : (
              <span>No solved puzzles yet.</span>
            )}
          </div>
        </div>

        {error ? <p className="feedback error profile-feedback">{error}</p> : null}
      </section>
    </main>
  )
}
