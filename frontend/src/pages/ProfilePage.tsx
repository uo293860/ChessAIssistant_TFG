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

        {error ? <p className="feedback error profile-feedback">{error}</p> : null}
      </section>
    </main>
  )
}
