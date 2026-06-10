import type {FormEvent} from 'react'
import {useEffect, useState} from 'react'
import {CartesianGrid, Cell, Line, LineChart, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis} from 'recharts'
import {fetchCurrentUserProfile, updateCurrentUsername} from '../api/users'
import type {EloHistoryPointDTO, UserProfileDTO} from '../api/users'
import appMark from '../assets/logo.png'
import {AboutLink} from '../components/AboutLink'

type ProfilePageProps = {
  fallbackEmail?: string | null
  onBackToBoard: () => void
  onOpenAbout: () => void
  onSignOut: () => Promise<void>
}

type SuccessRateDonutProps = {
  solved: number
  failed: number
}

type EloChartPoint = {
  attemptId: number
  attemptNumber: number
  elo: number
}

type SuccessRateChartPoint = {
  name: string
  value: number
  color: string
}

function SuccessRateDonut({ solved, failed }: SuccessRateDonutProps) {
  const totalAttempts = Math.max(solved + failed, 0)
  const solvedPercent = totalAttempts > 0 ? (solved / totalAttempts) * 100 : 0
  const roundedSolvedPercent = Math.round(solvedPercent)
  const chartData: SuccessRateChartPoint[] = totalAttempts > 0
    ? [
        { name: 'Solved', value: solved, color: '#1f9d55' },
        { name: 'Failed', value: failed, color: '#dc2626' },
      ]
    : [{ name: 'No attempts', value: 1, color: '#3a3a3a' }]

  return (
    <div
      className={`success-rate-donut ${totalAttempts === 0 ? 'empty' : ''}`}
      role="img"
      aria-label={`${roundedSolvedPercent}% success rate`}
    >
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={chartData}
            dataKey="value"
            nameKey="name"
            innerRadius="58%"
            outerRadius="100%"
            startAngle={90}
            endAngle={-270}
            stroke="none"
            isAnimationActive={false}
          >
            {chartData.map((entry) => (
              <Cell key={entry.name} fill={entry.color} />
            ))}
          </Pie>
        </PieChart>
      </ResponsiveContainer>
      <div className="success-rate-center">
        <strong>{roundedSolvedPercent}%</strong>
      </div>
    </div>
  )
}

const buildEloChartData = (history: EloHistoryPointDTO[]): EloChartPoint[] => {
  return history.map((point, index) => ({
    attemptId: point.attemptId,
    attemptNumber: index + 1,
    elo: point.resultingElo,
  }))
}

export function ProfilePage({ fallbackEmail, onBackToBoard, onOpenAbout, onSignOut }: ProfilePageProps) {
  const [profile, setProfile] = useState<UserProfileDTO | null>(null)
  const [isProfileLoading, setIsProfileLoading] = useState(true)
  const [isEditingUsername, setIsEditingUsername] = useState(false)
  const [usernameDraft, setUsernameDraft] = useState('')
  const [isUsernameSaving, setIsUsernameSaving] = useState(false)
  const [usernameFeedback, setUsernameFeedback] = useState('')
  const [usernameError, setUsernameError] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    const loadProfile = async () => {
      setIsProfileLoading(true)
      setError('')

      try {
        const loadedProfile = await fetchCurrentUserProfile()
        setProfile(loadedProfile)
        setUsernameDraft(loadedProfile.username)
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
  const eloChartData = buildEloChartData(eloHistory)
  const latestEloChange = eloHistory.at(-1)?.eloChange ?? 0

  const handleStartUsernameEdit = () => {
    setUsernameDraft(username)
    setUsernameFeedback('')
    setUsernameError('')
    setIsEditingUsername(true)
  }

  const handleCancelUsernameEdit = () => {
    setUsernameDraft(username)
    setUsernameError('')
    setIsEditingUsername(false)
  }

  const handleUsernameSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setUsernameFeedback('')
    setUsernameError('')

    const nextUsername = usernameDraft.trim()

    if (!nextUsername) {
      setUsernameError('Enter a username.')
      return
    }

    if (nextUsername.length > 50) {
      setUsernameError('Username must be 50 characters or fewer.')
      return
    }

    if (nextUsername === username) {
      setIsEditingUsername(false)
      return
    }

    setIsUsernameSaving(true)

    try {
      const updatedProfile = await updateCurrentUsername(nextUsername)
      setProfile(updatedProfile)
      setUsernameDraft(updatedProfile.username)
      setIsEditingUsername(false)
      setUsernameFeedback('Username updated.')
    } catch (usernameUpdateError) {
      setUsernameError(usernameUpdateError instanceof Error ? usernameUpdateError.message : 'Unable to update your username.')
    } finally {
      setIsUsernameSaving(false)
    }
  }

  return (
    <main className="profile-shell">
      <section className="profile-header">
        <div className="profile-brand">
          <img className="profile-logo" src={appMark} alt="ChessAIssistant logo" />
          <div>
            <p className="panel-title">Player profile</p>
            <h1>{username}</h1>
          </div>
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
        <section className="profile-section" aria-labelledby="personal-data-title">
          <h2 id="personal-data-title">Personal data</h2>
          <div className="profile-section-grid personal-data-grid">
            <div className="profile-summary">
              <span className="profile-avatar" aria-hidden="true">
                {username.slice(0, 1).toUpperCase()}
              </span>
              <div className="profile-username-block">
                <div className="profile-username-heading">
                  <p className="panel-title">Username</p>
                  {!isEditingUsername ? (
                    <button
                      type="button"
                      className="inline-action"
                      onClick={handleStartUsernameEdit}
                      disabled={isProfileLoading}
                    >
                      Modify
                    </button>
                  ) : null}
                </div>

                {isEditingUsername ? (
                  <form className="username-edit-form" onSubmit={handleUsernameSubmit}>
                    <label htmlFor="profile-username-input">New username</label>
                    <input
                      id="profile-username-input"
                      type="text"
                      value={usernameDraft}
                      onChange={(event) => setUsernameDraft(event.target.value)}
                      maxLength={50}
                      autoComplete="nickname"
                      aria-invalid={Boolean(usernameError)}
                      disabled={isUsernameSaving}
                      required
                    />
                    <div className="username-edit-actions">
                      <button type="submit" className="primary-action" disabled={isUsernameSaving}>
                        {isUsernameSaving ? 'Saving...' : 'Save'}
                      </button>
                      <button
                        type="button"
                        className="secondary-action"
                        onClick={handleCancelUsernameEdit}
                        disabled={isUsernameSaving}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <strong>{username}</strong>
                )}

                <div className="username-message-region" aria-live="polite">
                  {usernameFeedback ? <p className="username-feedback success">{usernameFeedback}</p> : null}
                  {usernameError ? <p className="username-feedback error">{usernameError}</p> : null}
                </div>
              </div>
            </div>

            <div className="profile-stat">
              <p className="panel-title">Email</p>
              <strong>{email}</strong>
            </div>
          </div>
        </section>

        <section className="profile-section" aria-labelledby="game-data-title">
          <h2 id="game-data-title">Game data</h2>
          <div className="profile-section-grid game-data-grid">
            <div className="profile-stat">
              <p className="panel-title">Elo rating</p>
              <strong>{isProfileLoading ? 'Loading...' : eloRating}</strong>
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
                  <strong>{isProfileLoading ? 'Loading...' : `${eloHistory.length} puzzle attempts`}</strong>
                </div>
                {!isProfileLoading && eloHistory.length > 0 ? (
                  <div className="elo-delta-summary" aria-label="Last Elo variation">
                    <span>Last Elo variation</span>
                    <span className={`elo-delta ${latestEloChange >= 0 ? 'positive' : 'negative'}`}>
                      {latestEloChange >= 0 ? `+${latestEloChange}` : latestEloChange}
                    </span>
                  </div>
                ) : null}
              </div>

              <div className="elo-chart">
                {isProfileLoading ? (
                  <span>Loading chart...</span>
                ) : eloChartData.length > 0 ? (
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={eloChartData} margin={{ top: 12, right: 8, bottom: 8, left: 0 }}>
                      <CartesianGrid stroke="#e5e5e5" vertical={false} />
                      <XAxis dataKey="attemptNumber" hide />
                      <YAxis
                        dataKey="elo"
                        axisLine={{ stroke: '#6a6a6a', strokeWidth: 1.5 }}
                        tickLine={{ stroke: '#6a6a6a', strokeWidth: 1.5 }}
                        tick={{ fill: '#4c4c4c', fontSize: 12, fontWeight: 800 }}
                        width={56}
                        domain={['dataMin - 10', 'dataMax + 10']}
                      />
                      <Tooltip
                        labelFormatter={(value) => `Puzzle attempt ${value}`}
                        formatter={(value) => [value, 'Elo']}
                      />
                      <Line
                        type="monotone"
                        dataKey="elo"
                        stroke="#111111"
                        strokeWidth={4}
                        dot={{ r: 4, fill: '#111111', strokeWidth: 0 }}
                        activeDot={{ r: 6, fill: '#111111', strokeWidth: 0 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <span>No puzzle attempts yet.</span>
                )}
              </div>
            </div>
          </div>
        </section>

        {error ? <p className="feedback error profile-feedback">{error}</p> : null}
      </section>
      <AboutLink onOpenAbout={onOpenAbout} />
    </main>
  )
}
