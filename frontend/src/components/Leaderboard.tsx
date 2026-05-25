import { useEffect, useState } from 'react'
import { fetchLeaderboard } from '../api/users'
import type { UserLeaderboardEntryDTO } from '../api/users'

type LeaderboardProps = {
  refreshKey: number
}

export function Leaderboard({ refreshKey }: LeaderboardProps) {
  const [entries, setEntries] = useState<UserLeaderboardEntryDTO[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

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
        </div>

        {isLoading ? (
          <p className="leaderboard-state">Loading leaderboard...</p>
        ) : error ? (
          <p className="leaderboard-state error-text">{error}</p>
        ) : entries.length === 0 ? (
          <p className="leaderboard-state">No rated users yet.</p>
        ) : (
          entries.map((entry, index) => (
            <div className="leaderboard-row" role="row" key={`${entry.username}-${entry.eloRating}-${index}`}>
              <span className="leaderboard-rank" role="cell">
                {index + 1}
              </span>
              <span className="leaderboard-username" role="cell">
                {entry.username}
              </span>
              <strong className="leaderboard-elo" role="cell">
                {entry.eloRating}
              </strong>
            </div>
          ))
        )}
      </div>
    </aside>
  )
}
