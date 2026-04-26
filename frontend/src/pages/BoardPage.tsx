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

type VerifyPuzzleMoveRequestDTO = {
  puzzleId: string
  move: string
  moveIndex: number
}

type VerifyPuzzleMoveResponseDTO = {
  correct: boolean
  opponentMove: string
  nextMoveIndex: number
  puzzleCompleted: boolean
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

export function BoardPage({ isLoading, onSignOut }: BoardPageProps) {
  const [game, setGame] = useState(() => new Chess())
  const [boardOrientation, setBoardOrientation] = useState<'white' | 'black'>('white')
  const [puzzle, setPuzzle] = useState<PuzzleDTO | null>(null)
  const [isPuzzleLoading, setIsPuzzleLoading] = useState(true)
  const [isReplayingInitialMove, setIsReplayingInitialMove] = useState(false)
  const [selectedSquare, setSelectedSquare] = useState<Square | null>(null)
  const [legalTargets, setLegalTargets] = useState<Square[]>([])
  const [pendingPromotion, setPendingPromotion] = useState<PendingPromotion | null>(null)
  const [puzzleHints, setPuzzleHints] = useState<string[]>([])
  const [revealedHintCount, setRevealedHintCount] = useState(0)
  const [isHintsLoading, setIsHintsLoading] = useState(false)
  const [isVerifyingMove, setIsVerifyingMove] = useState(false)
  const [isReplayingOpponentMove, setIsReplayingOpponentMove] = useState(false)
  const [currentMoveIndex, setCurrentMoveIndex] = useState(1)
  const [isPuzzleCompleted, setIsPuzzleCompleted] = useState(false)
  const replayTimeoutRef = useRef<number | null>(null)

  const clearSelection = () => {
    setSelectedSquare(null)
    setLegalTargets([])
    setPendingPromotion(null)
  }

  const clearReplayTimeout = () => {
    if (replayTimeoutRef.current !== null) {
      window.clearTimeout(replayTimeoutRef.current)
      replayTimeoutRef.current = null
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
    replayTimeoutRef.current = window.setTimeout(() => {
      setGame(getPositionAfterInitialMove(nextPuzzle))
      setIsReplayingInitialMove(false)
      replayTimeoutRef.current = null
    }, INITIAL_MOVE_DELAY_MS)
  }

  const replayPuzzleMove = (position: Chess, move: string) => {
    if (move) {
      position.move(move)
    }

    return position
  }

  const selectSquare = (square: Square) => {
    const legalMoves = game.moves({ square, verbose: true })
    setSelectedSquare(square)
    setLegalTargets(legalMoves.map((move) => move.to))
  }

  const buildUciMove = (from: Square, to: Square, promotion?: PromotionPiece) => {
    return `${from}${to}${promotion ?? ''}`
  }

  const verifyPuzzleMove = async (request: VerifyPuzzleMoveRequestDTO) => {
    const response = await fetch('http://localhost:8080/api/puzzles/verify-move', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error('Unable to verify the puzzle move.')
    }

    return (await response.json()) as VerifyPuzzleMoveResponseDTO
  }

  const tryPuzzleMove = async (from: Square, to: Square, promotion?: PromotionPiece) => {
    if (!puzzle || isPuzzleCompleted) {
      return false
    }

    const nextGame = new Chess(game.fen())
    const moveResult = nextGame.move({ from, to, promotion })

    if (!moveResult) {
      return false
    }

    setIsVerifyingMove(true)

    try {
      const verification = await verifyPuzzleMove({
        puzzleId: puzzle.id,
        move: buildUciMove(from, to, promotion),
        moveIndex: currentMoveIndex,
      })

      if (!verification.correct) {
        clearSelection()
        return false
      }

      setGame(nextGame)
      clearSelection()

      if (verification.opponentMove) {
        const resolvedGame = new Chess(nextGame.fen())
        setIsReplayingOpponentMove(true)

        replayTimeoutRef.current = window.setTimeout(() => {
          replayPuzzleMove(resolvedGame, verification.opponentMove)
          setGame(resolvedGame)
          setCurrentMoveIndex(verification.nextMoveIndex)
          setIsPuzzleCompleted(verification.puzzleCompleted)
          setIsReplayingOpponentMove(false)
          replayTimeoutRef.current = null
        }, INITIAL_MOVE_DELAY_MS)
        return true
      }

      setCurrentMoveIndex(verification.nextMoveIndex)
      setIsPuzzleCompleted(verification.puzzleCompleted)
      return true
    } finally {
      setIsVerifyingMove(false)
    }
  }

  const loadRandomPuzzle = async () => {
    clearReplayTimeout()
    setIsPuzzleLoading(true)
    setIsReplayingInitialMove(false)
    setIsReplayingOpponentMove(false)
    setIsVerifyingMove(false)
    setPuzzleHints([])
    setRevealedHintCount(0)
    setIsHintsLoading(false)
    setCurrentMoveIndex(1)
    setIsPuzzleCompleted(false)
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

  const loadHints = async () => {
    if (!puzzle) {
      return
    }

    if (puzzleHints.length > 0) {
      setRevealedHintCount((currentCount) => Math.min(currentCount + 1, puzzleHints.length))
      return
    }

    setIsHintsLoading(true)

    try {
      const response = await fetch(`http://localhost:8080/api/puzzles/${puzzle.id}/hints`)

      if (!response.ok) {
        throw new Error('Unable to load puzzle hints.')
      }

      const nextHints = (await response.json()) as string[]
      const sanitizedHints = nextHints.filter((hint) => hint.trim().length > 0)
      setPuzzleHints(sanitizedHints)
      setRevealedHintCount(sanitizedHints.length > 0 ? 1 : 0)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to load puzzle hints.'
      setPuzzleHints([message])
      setRevealedHintCount(1)
    } finally {
      setIsHintsLoading(false)
    }
  }

  useEffect(() => {
    void loadRandomPuzzle()
    return () => clearReplayTimeout()
  }, [])

  const handlePromotionChoice = (promotion: PromotionPiece) => {
    if (!pendingPromotion) {
      return
    }

    void tryPuzzleMove(pendingPromotion.from, pendingPromotion.to, promotion)
  }

  const handleSquareClick = ({ square }: { square: string }) => {
    if (isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isPuzzleCompleted) {
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

      void tryPuzzleMove(selectedSquare, boardSquare)
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
    if (!targetSquare || isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isPuzzleCompleted) {
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

    void tryPuzzleMove(from, to)
    return false
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

  const boardInteractionDisabled =
    isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isPuzzleCompleted
  const displayedHints = puzzleHints.slice(0, revealedHintCount)
  const areHintsExhausted = puzzleHints.length > 0 && revealedHintCount >= Math.min(puzzleHints.length, 3)

  return (
    <main className="board-shell">
      <section className="board-header-card">
        <div>
          <h1>ChessAIssistant</h1>
        </div>
        <div className="board-actions">
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
                allowDrawingArrows: true,
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
            <p className="panel-title">Game state</p>
            <strong>{isPuzzleLoading ? 'Loading puzzle...' : getStatusMessage(game)}</strong>
            <p className="panel-copy">
              {isReplayingInitialMove
                ? `Showing the starting position. The opponent move ${puzzle?.initialMove ?? ''} will play in a moment.`
                : isReplayingOpponentMove
                  ? 'Waiting for the opponent reply.'
                : isVerifyingMove
                  ? 'Verifying your move with the backend.'
                : isPuzzleCompleted
                  ? 'Puzzle solved. Load a new puzzle to continue.'
                : pendingPromotion
                  ? `Choose a promotion piece for ${pendingPromotion.to}.`
                  : selectedSquare
                    ? `Selected square: ${selectedSquare}`
                    : isPuzzleLoading
                      ? 'Fetching a random puzzle from the backend.'
                      : 'Drag a piece or click a square to see legal moves.'}
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

          <div>
            <button type="button" className="secondary-action compact-action" onClick={handleBoardReset}>
              New puzzle
            </button>
          </div>

          <button
              type="button"
              className="secondary-action compact-action"
              onClick={askHints}
              disabled={!puzzle || isHintsLoading || areHintsExhausted}
          >
            Ask for AIssistance
          </button>

          <div className="board-panel">
            <p className="panel-title">Hints</p>
            <div className="moves-output">
              {isHintsLoading
                ? 'Loading hints...'
                : displayedHints.length > 0
                  ? displayedHints.map((hint, index) => <p key={`${index}-${hint}`}>{hint}</p>)
                  : 'Ask for AIssistance to load hints for this puzzle.'}
            </div>
          </div>
        </aside>
      </section>
    </main>
  )

  function handleBoardReset() {
    void loadRandomPuzzle()
  }

  function askHints() {
    void loadHints()
  }
}
