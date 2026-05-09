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

        {error ? <p className="feedback error profile-feedback">{error}</p> : null}
      </section>
    </main>
  )
}
