import { useEffect, useState } from 'react'
import { fetchLeaderboard } from '../api/users'
import type { UserLeaderboardEntryDTO } from '../api/users'

type LeaderboardProps = {
  refreshKey: number
}

const getDailyRankChangeClassName = (dailyRankChange: number) => {
  if (dailyRankChange > 0) {
    return 'leaderboard-daily-change positive'
  }

  if (dailyRankChange < 0) {
    return 'leaderboard-daily-change negative'
  }

  return 'leaderboard-daily-change neutral'
}

const getDailyRankChangeLabel = (dailyRankChange: number) => {
  if (dailyRankChange > 0) {
    return `Climbed ${dailyRankChange} positions today`
  }

  if (dailyRankChange < 0) {
    return `Descended ${Math.abs(dailyRankChange)} positions today`
  }

  return 'No rank change today'
}

const renderDailyRankChange = (dailyRankChange: number, role?: 'cell') => (
  <span
    className={getDailyRankChangeClassName(dailyRankChange)}
    role={role}
    aria-label={getDailyRankChangeLabel(dailyRankChange)}
  >
    {dailyRankChange === 0 ? (
      <span className="leaderboard-daily-line" aria-hidden="true" />
    ) : (
      <>
        <span className="leaderboard-daily-arrow" aria-hidden="true" />
        <span>{Math.abs(dailyRankChange)}</span>
      </>
    )}
  </span>
)

export function Leaderboard({ refreshKey }: LeaderboardProps) {
  const [entries, setEntries] = useState<UserLeaderboardEntryDTO[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const currentUserIndex = entries.findIndex((entry) => entry.currentUser)
  const currentUserEntry = currentUserIndex >= 0 ? entries[currentUserIndex] : null

  useEffect(() => {
    let isActive = true

    const loadLeaderboard = async () => {
      setIsLoading(true)
      setError('')

      try {
        const nextEntries = await fetchLeaderboard()

        if (isActive) {
          setEntries(nextEntries)
        }
      } catch (leaderboardError) {
        if (isActive) {
          setError(leaderboardError instanceof Error ? leaderboardError.message : 'Unable to load the leaderboard.')
        }
      } finally {
        if (isActive) {
          setIsLoading(false)
        }
      }
    }

    void loadLeaderboard()

    return () => {
      isActive = false
    }
  }, [refreshKey])

  return (
    <aside className="board-panel leaderboard-panel" aria-labelledby="leaderboard-title">
      <div className="leaderboard-header">
        <div>
          <p className="panel-title">Leaderboard</p>
          <strong id="leaderboard-title">Top players</strong>
        </div>
      </div>

      <div className="leaderboard-list" role="table" aria-label="Users ordered by Elo rating">
        <div className="leaderboard-row leaderboard-row-header" role="row">
          <span role="columnheader">#</span>
          <span role="columnheader">Username</span>
          <span role="columnheader">Elo</span>
          <span role="columnheader">Day</span>
        </div>

        {isLoading ? (
          <p className="leaderboard-state">Loading leaderboard...</p>
        ) : error ? (
          <p className="leaderboard-state error-text">{error}</p>
        ) : entries.length === 0 ? (
          <p className="leaderboard-state">No rated users yet.</p>
        ) : (
          entries.map((entry, index) => {
            return (
              <div
                className={`leaderboard-row ${entry.currentUser ? 'leaderboard-row-current' : ''}`}
                role="row"
                key={entry.username}
                aria-current={entry.currentUser ? 'true' : undefined}
              >
                <span className="leaderboard-rank" role="cell">
                  {index + 1}
                </span>
                <span className="leaderboard-username" role="cell">
                  <span className="leaderboard-username-text">{entry.username}</span>
                  {entry.currentUser ? <span className="leaderboard-current-label">You</span> : null}
                </span>
                <strong className="leaderboard-elo" role="cell">
                  {entry.eloRating}
                </strong>
                {renderDailyRankChange(entry.dailyRankChange, 'cell')}
              </div>
            )
          })
        )}
      </div>

      {currentUserEntry ? (
        <div className="leaderboard-current-summary" aria-label="Your leaderboard position">
          <span className="leaderboard-current-summary-label">Your position</span>
          <div className="leaderboard-row leaderboard-row-current leaderboard-current-summary-row">
            <span className="leaderboard-rank">{currentUserIndex + 1}</span>
            <span className="leaderboard-username">
              <span className="leaderboard-username-text">{currentUserEntry.username}</span>
              <span className="leaderboard-current-label">You</span>
            </span>
            <strong className="leaderboard-elo">{currentUserEntry.eloRating}</strong>
            {renderDailyRankChange(currentUserEntry.dailyRankChange)}
          </div>
        </div>
      ) : null}
    </aside>
  )
}
