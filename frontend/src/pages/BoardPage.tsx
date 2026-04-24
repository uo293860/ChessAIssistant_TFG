import { useEffect, useRef, useState } from 'react'
import { Chess } from 'chess.js'
import type { CSSProperties } from 'react'
import type { Square } from 'chess.js'
import { Chessboard } from 'react-chessboard'

type ChessColor = 'w' | 'b'
type PromotionPiece = 'b' | 'n' | 'r' | 'q'

type BoardPageProps = {
  isLoading: boolean
  userEmail?: string | null
  onBack: () => void
  onSignOut: () => Promise<void>
}

type PendingPromotion = {
  from: Square
  to: Square
  color: ChessColor
}

type PuzzleDTO = {
  id: string
  fen: string
  rating: number
  themes: string
  gameUrl: string
  initialMove: string
}

const promotionChoices: PromotionPiece[] = ['q', 'r', 'b', 'n']
const INITIAL_MOVE_DELAY_MS = 1200
const lightSquareColor = '#f0f0f0'
const darkSquareColor = '#8f8f8f'

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

const getPieceLabel = (piece: PromotionPiece) => {
  return piece === 'q' ? 'Queen' : piece === 'r' ? 'Rook' : piece === 'b' ? 'Bishop' : 'Knight'
}

const getPieceCode = (color: ChessColor, piece: PromotionPiece) => {
  return `${color}${piece.toUpperCase()}`
}

export function BoardPage({ isLoading, userEmail, onBack, onSignOut }: BoardPageProps) {
  const [game, setGame] = useState(() => new Chess())
  const [boardOrientation, setBoardOrientation] = useState<'white' | 'black'>('white')
  const [puzzle, setPuzzle] = useState<PuzzleDTO | null>(null)
  const [isPuzzleLoading, setIsPuzzleLoading] = useState(true)
  const [isReplayingInitialMove, setIsReplayingInitialMove] = useState(false)
  const [selectedSquare, setSelectedSquare] = useState<Square | null>(null)
  const [legalTargets, setLegalTargets] = useState<Square[]>([])
  const [pendingPromotion, setPendingPromotion] = useState<PendingPromotion | null>(null)
  const initialMoveTimeoutRef = useRef<number | null>(null)

  const clearSelection = () => {
    setSelectedSquare(null)
    setLegalTargets([])
    setPendingPromotion(null)
  }

  const clearInitialMoveTimeout = () => {
    if (initialMoveTimeoutRef.current !== null) {
      window.clearTimeout(initialMoveTimeoutRef.current)
      initialMoveTimeoutRef.current = null
    }
  }

  const getPositionAfterInitialMove = (nextPuzzle: PuzzleDTO) => {
    const position = new Chess(nextPuzzle.fen)

    if (nextPuzzle.initialMove) {
      position.move(nextPuzzle.initialMove)
    }

    return position
  }

  const replayInitialMove = (nextPuzzle: PuzzleDTO) => {
    if (!nextPuzzle.initialMove) {
      return
    }

    setIsReplayingInitialMove(true)
    initialMoveTimeoutRef.current = window.setTimeout(() => {
      setGame(getPositionAfterInitialMove(nextPuzzle))
      setIsReplayingInitialMove(false)
      initialMoveTimeoutRef.current = null
    }, INITIAL_MOVE_DELAY_MS)
  }

  const selectSquare = (square: Square) => {
    const legalMoves = game.moves({ square, verbose: true })
    setSelectedSquare(square)
    setLegalTargets(legalMoves.map((move) => move.to))
  }

  const commitMove = (from: Square, to: Square, promotion?: PromotionPiece) => {
    const nextGame = new Chess(game.fen())
    const moveResult = nextGame.move({ from, to, promotion })

    if (!moveResult) {
      return false
    }

    setGame(nextGame)
    clearSelection()
    return true
  }

  const loadRandomPuzzle = async () => {
    clearInitialMoveTimeout()
    setIsPuzzleLoading(true)
    setIsReplayingInitialMove(false)
    clearSelection()

    const response = await fetch('http://localhost:8080/api/puzzles/random')
    const nextPuzzle = (await response.json()) as PuzzleDTO
    const nextPosition = getPositionAfterInitialMove(nextPuzzle)

    setPuzzle(nextPuzzle)
    setBoardOrientation(nextPosition.turn() === 'w' ? 'white' : 'black')
    setGame(new Chess(nextPuzzle.fen))
    replayInitialMove(nextPuzzle)
    setIsPuzzleLoading(false)
  }

  useEffect(() => {
    void loadRandomPuzzle()
    return () => clearInitialMoveTimeout()
  }, [])

  const handlePromotionChoice = (promotion: PromotionPiece) => {
    if (!pendingPromotion) {
      return
    }

    commitMove(pendingPromotion.from, pendingPromotion.to, promotion)
  }

  const handleSquareClick = ({ square }: { square: string }) => {
    if (isPuzzleLoading || isReplayingInitialMove) {
      return
    }

    const boardSquare = square as Square
    const piece = game.get(boardSquare)

    if (selectedSquare && legalTargets.includes(boardSquare)) {
      const matchingMoves = game.moves({ square: selectedSquare, verbose: true }).filter((move) => move.to === boardSquare)
      const requiresPromotion = matchingMoves.some((move) => move.promotion)

      if (requiresPromotion) {
        const selectedPiece = game.get(selectedSquare)

        if (selectedPiece) {
          setPendingPromotion({ from: selectedSquare, to: boardSquare, color: selectedPiece.color })
        }

        return
      }

      commitMove(selectedSquare, boardSquare)
      return
    }

    if (!piece) {
      clearSelection()
      return
    }

    if (piece.color !== game.turn()) {
      clearSelection()
      return
    }

    selectSquare(boardSquare)
  }

  const handlePieceDrop = ({
    sourceSquare,
    targetSquare,
  }: {
    sourceSquare: string
    targetSquare: string | null
  }) => {
    if (!targetSquare || isPuzzleLoading || isReplayingInitialMove) {
      return false
    }

    const from = sourceSquare as Square
    const to = targetSquare as Square
    const matchingMoves = game.moves({ square: from, verbose: true }).filter((move) => move.to === to)

    if (matchingMoves.length === 0) {
      return false
    }

    const requiresPromotion = matchingMoves.some((move) => move.promotion)

    if (requiresPromotion) {
      const draggedPiece = game.get(from)

      if (draggedPiece) {
        setPendingPromotion({ from, to, color: draggedPiece.color })
        setSelectedSquare(from)
        setLegalTargets([to])
      }

      return false
    }

    return commitMove(from, to)
  }

  const squareStyles: Record<string, CSSProperties> = {}

  if (selectedSquare) {
    squareStyles[selectedSquare] = {
      background: 'radial-gradient(circle at center, rgba(17, 17, 17, 0.16) 0%, rgba(17, 17, 17, 0.34) 100%)',
      boxShadow: 'inset 0 0 0 4px rgba(17, 17, 17, 0.72)',
    }
  }

  for (const target of legalTargets) {
    squareStyles[target] = {
      ...(squareStyles[target] ?? {}),
      background:
        'radial-gradient(circle, rgba(17, 17, 17, 0.22) 0%, rgba(17, 17, 17, 0.22) 18%, transparent 20%)',
    }
  }

  const boardInteractionDisabled = isPuzzleLoading || isReplayingInitialMove

  return (
    <main className="board-shell">
      <section className="board-header-card">
        <div>
          <p className="eyebrow">Protected endpoint</p>
          <h1>Random Chess Puzzle</h1>
          <p className="hero-copy board-copy">
            This page is available only to authenticated users. It loads one random puzzle from the backend, pauses on
            the original FEN, and then replays the opponent&apos;s last move before you start solving.
          </p>
        </div>

        <div className="board-actions">
          <button type="button" className="primary-action compact-action" onClick={handleBoardReset}>
            New puzzle
          </button>
          <button type="button" className="secondary-action compact-action" onClick={onBack}>
            Back to auth
          </button>
          <button type="button" className="secondary-action compact-action" onClick={() => void onSignOut()} disabled={isLoading}>
            Sign out
          </button>
        </div>
      </section>

      <section className="board-layout">
        <div className="board-card">
          <div className="board-frame">
            <Chessboard
              options={{
                id: 'puzzle-board',
                position: game.fen(),
                boardOrientation,
                showNotation: true,
                allowDrawingArrows: false,
                allowDragging: !boardInteractionDisabled,
                animationDurationInMs: 220,
                boardStyle: {
                  width: '100%',
                  borderRadius: '22px',
                  boxShadow: '0 24px 48px rgba(0, 0, 0, 0.16)',
                },
                darkSquareStyle: { backgroundColor: darkSquareColor },
                lightSquareStyle: { backgroundColor: lightSquareColor },
                squareStyles,
                canDragPiece: ({ piece }) => {
                  if (boardInteractionDisabled || !piece) {
                    return false
                  }

                  return piece.pieceType[0].toLowerCase() === game.turn()
                },
                onSquareClick: handleSquareClick,
                onPieceDrop: handlePieceDrop,
              }}
            />
          </div>
        </div>

        <aside className="board-sidebar">
          <div className="board-panel">
            <p className="panel-title">Session</p>
            <strong>{userEmail ?? 'Unknown user'}</strong>
            <p className="panel-copy">Authenticated users can reload this route and keep access while Firebase keeps the session alive.</p>
          </div>

          <div className="board-panel">
            <p className="panel-title">Game state</p>
            <strong>{isPuzzleLoading ? 'Loading puzzle...' : getStatusMessage(game)}</strong>
            <p className="panel-copy">
              {isReplayingInitialMove
                ? `Showing the starting position. The opponent move ${puzzle?.initialMove ?? ''} will play in a moment.`
                : pendingPromotion
                  ? `Choose a promotion piece for ${pendingPromotion.to}.`
                  : selectedSquare
                    ? `Selected square: ${selectedSquare}`
                    : isPuzzleLoading
                      ? 'Fetching a random puzzle from the backend.'
                      : 'Drag a piece or click a square to see legal moves.'}
            </p>
          </div>

          <div className="board-panel">
            <p className="panel-title">Puzzle</p>
            <strong>{puzzle?.id ?? 'No puzzle loaded'}</strong>
            <p className="panel-copy">
              {puzzle
                ? `Rating ${puzzle.rating}. Themes: ${puzzle.themes || 'No themes available.'} Last move: ${puzzle.initialMove || 'Unknown'}.`
                : 'The board will show the fetched puzzle position.'}
            </p>
          </div>

          {pendingPromotion ? (
            <div className="board-panel">
              <p className="panel-title">Promotion</p>
              <strong>Select the piece for pawn promotion.</strong>
              <div className="promotion-actions">
                {promotionChoices.map((promotion) => (
                  <button
                    key={promotion}
                    type="button"
                    className="promotion-action"
                    onClick={() => handlePromotionChoice(promotion)}
                    aria-label={`Promote to ${getPieceLabel(promotion)}`}
                  >
                    <span className="promotion-piece-code">{getPieceCode(pendingPromotion.color, promotion)}</span>
                    <span className="promotion-label">{getPieceLabel(promotion)}</span>
                  </button>
                ))}
              </div>
              <button type="button" className="secondary-action promotion-cancel" onClick={clearSelection}>
                Cancel promotion
              </button>
            </div>
          ) : null}

          <div className="board-panel">
            <p className="panel-title">FEN</p>
            <code className="fen-output">{game.fen()}</code>
          </div>

          <div className="board-panel">
            <p className="panel-title">Source</p>
            <p className="moves-output">
              {puzzle?.gameUrl ? (
                <a href={puzzle.gameUrl} target="_blank" rel="noreferrer">
                  Open original game
                </a>
              ) : (
                'No source game available.'
              )}
            </p>
          </div>

          <div className="board-panel">
            <p className="panel-title">Moves</p>
            <p className="moves-output">{game.pgn() || 'No moves played yet.'}</p>
          </div>
        </aside>
      </section>
    </main>
  )

  function handleBoardReset() {
    void loadRandomPuzzle()
  }
}
