import type {Chess, Square} from 'chess.js'
import type {PuzzleDTO} from '../../api/puzzles'
import type {PendingPromotion} from './boardTypes'

type GameStatePanelProps = {
  game: Chess
  puzzle: PuzzleDTO | null
  isPuzzleLoading: boolean
  isReplayingInitialMove: boolean
  isReplayingOpponentMove: boolean
  isVerifyingMove: boolean
  isSurrendering: boolean
  isPuzzleCompleted: boolean
  isPuzzleSurrendered: boolean
  isRepeatingFailedPuzzle: boolean
  pendingPromotion: PendingPromotion | null
  selectedSquare: Square | null
  failedAttempts: number
  updatedElo: number | null
  updatedEloChange: number | null
  surrenderedSolutionMoves: string[]
  puzzleActionError: string | null
}

type GameStateCopyOptions = {
  puzzle: PuzzleDTO | null
  isPuzzleLoading: boolean
  isReplayingInitialMove: boolean
  isReplayingOpponentMove: boolean
  isVerifyingMove: boolean
  isSurrendering: boolean
  isPuzzleCompleted: boolean
  isPuzzleSurrendered: boolean
  isRepeatingFailedPuzzle: boolean
  pendingPromotion: PendingPromotion | null
  selectedSquare: Square | null
  failedAttempts: number
}

const getStatusMessage = (game: Chess) => {
  if (game.isCheckmate()) {
    return `Checkmate. ${game.turn() === 'w' ? 'Black' : 'White'} wins.`
  }

  if (game.isDraw()) {
    return 'Draw. No legal continuation remains.'
  }

  if (game.isCheck()) {
    return `${game.turn() === 'w' ? 'White' : 'Black'} to move and currently in check.`
  }

  return `${game.turn() === 'w' ? 'White' : 'Black'} to move.`
}

const formatEloChange = (eloChange: number) => {
  return eloChange >= 0 ? `+${eloChange}` : `${eloChange}`
}

const getGameStateCopy = ({
  puzzle,
  isPuzzleLoading,
  isReplayingInitialMove,
  isReplayingOpponentMove,
  isVerifyingMove,
  isSurrendering,
  isPuzzleCompleted,
  isPuzzleSurrendered,
  isRepeatingFailedPuzzle,
  pendingPromotion,
  selectedSquare,
  failedAttempts,
}: GameStateCopyOptions) => {
  if (isReplayingInitialMove) {
    return `Showing the starting position. The opponent move ${puzzle?.initialMove ?? ''} will play in a moment.`
  }

  if (isReplayingOpponentMove) {
    return 'Waiting for the opponent reply.'
  }

  if (isVerifyingMove) {
    return 'Verifying your move with the backend.'
  }

  if (isSurrendering) {
    return 'Surrendering the puzzle and updating your Elo.'
  }

  if (isPuzzleSurrendered) {
    return isRepeatingFailedPuzzle
      ? 'Retry surrendered. The failed attempt remains unsolved.'
      : 'Puzzle surrendered. Elo has been updated as a failed attempt. Choose a new puzzle or retry a random failed puzzle.'
  }

  if (isPuzzleCompleted) {
    if (isRepeatingFailedPuzzle) {
      return 'Failed puzzle solved. The failed attempt is now marked as solved.'
    }

    if (failedAttempts > 0) {
      return 'Puzzle completed with mistakes. Choose a new puzzle or retry a random failed puzzle.'
    }

    return 'Puzzle solved. Choose a new puzzle or retry a random failed puzzle.'
  }

  if (pendingPromotion) {
    return `Choose a promotion piece for ${pendingPromotion.to}.`
  }

  if (selectedSquare) {
    return `Selected square: ${selectedSquare}`
  }

  if (isPuzzleLoading) {
    return 'Fetching a random puzzle from the backend.'
  }

  return 'Drag a piece or click a square to see legal moves.'
}

export function GameStatePanel({
  game,
  puzzle,
  isPuzzleLoading,
  isReplayingInitialMove,
  isReplayingOpponentMove,
  isVerifyingMove,
  isSurrendering,
  isPuzzleCompleted,
  isPuzzleSurrendered,
  isRepeatingFailedPuzzle,
  pendingPromotion,
  selectedSquare,
  failedAttempts,
  updatedElo,
  updatedEloChange,
  surrenderedSolutionMoves,
  puzzleActionError,
}: GameStatePanelProps) {
  const panelCopy = getGameStateCopy({
    puzzle,
    isPuzzleLoading,
    isReplayingInitialMove,
    isReplayingOpponentMove,
    isVerifyingMove,
    isSurrendering,
    isPuzzleCompleted,
    isPuzzleSurrendered,
    isRepeatingFailedPuzzle,
    pendingPromotion,
    selectedSquare,
    failedAttempts,
  })
  const surrenderedSolution = surrenderedSolutionMoves.join(' ')
  const puzzleTrainingUrl = puzzle ? `https://lichess.org/training/${encodeURIComponent(puzzle.id)}` : null

  return (
    <div className="board-panel">
      <p className="panel-title">Game state</p>
      <strong>{isPuzzleLoading ? 'Loading puzzle...' : getStatusMessage(game)}</strong>
      <p className="puzzle-rating">
        {isPuzzleLoading || !puzzle ? 'Puzzle Elo loading...' : `Puzzle Elo: ${puzzle.rating}`}
      </p>
      <p className="panel-copy">{panelCopy}</p>
      {isPuzzleSurrendered ? (
        <div className="solution-summary" aria-label="Puzzle solution">
          <span>Solution</span>
          <strong>{surrenderedSolution || 'No solution moves were returned.'}</strong>
          {puzzleTrainingUrl ? (
            <a href={puzzleTrainingUrl} target="_blank" rel="noreferrer">
              Open puzzle on Lichess
            </a>
          ) : null}
        </div>
      ) : null}
      {puzzleActionError ? <p className="feedback error">{puzzleActionError}</p> : null}
      {(isPuzzleCompleted || isPuzzleSurrendered) && updatedElo !== null && updatedEloChange !== null ? (
        <div className="completion-elo-summary" aria-label="Puzzle Elo result">
          <span className="completion-elo-item">
            <span>New Elo</span>
            <strong>{updatedElo}</strong>
          </span>
          <span className="completion-elo-item">
            <span>Variation</span>
            <strong className={updatedEloChange >= 0 ? 'positive' : 'negative'}>
              {formatEloChange(updatedEloChange)}
            </strong>
          </span>
        </div>
      ) : null}
    </div>
  )
}
