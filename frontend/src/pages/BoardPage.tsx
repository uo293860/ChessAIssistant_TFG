import type {CSSProperties, ReactNode} from 'react'
import {useEffect, useRef, useState} from 'react'
import type {Square} from 'chess.js'
import {Chess} from 'chess.js'
import {Chessboard, defaultPieces} from 'react-chessboard'
import {fetchWithAuth} from '../api/apiClient'
import appMark from '../assets/logo.png'
import {AboutLink} from '../components/AboutLink'
import {Leaderboard} from '../components/Leaderboard'

type ChessColor = 'w' | 'b'
type PromotionPiece = 'b' | 'n' | 'r' | 'q'
type PromotionPieceType = 'wB' | 'wN' | 'wR' | 'wQ' | 'bB' | 'bN' | 'bR' | 'bQ'

type BoardPageProps = {
  isLoading: boolean
  onOpenProfile: () => void
  onOpenAbout: () => void
  onSignOut: () => Promise<void>
}

type PendingPromotion = {
  from: Square
  to: Square
  color: ChessColor
}

type PuzzleDTO = {
  id: string
  sessionId: number
  fen: string
  rating: number
  themes: string
  gameUrl: string
  initialMove: string
}

type VerifyPuzzleMoveRequestDTO = {
  sessionId: number
  puzzleId: string
  move: string
}

type VerifyPuzzleMoveResponseDTO = {
  correct: boolean
  opponentMove: string
  nextMoveIndex: number
  puzzleCompleted: boolean
  newElo: number | null
  eloChange: number | null
}

type SurrenderPuzzleRequestDTO = {
  sessionId: number
  puzzleId: string
}

type SurrenderPuzzleResponseDTO = {
  puzzleCompleted: boolean
  newElo: number | null
  eloChange: number | null
}

const promotionChoices: PromotionPiece[] = ['q', 'r', 'b', 'n']
const promotionPieceTypes: Record<ChessColor, Record<PromotionPiece, PromotionPieceType>> = {
  w: {
    b: 'wB',
    n: 'wN',
    r: 'wR',
    q: 'wQ',
  },
  b: {
    b: 'bB',
    n: 'bN',
    r: 'bR',
    q: 'bQ',
  },
}
const INITIAL_MOVE_DELAY_MS = 1200
const INCORRECT_MOVE_FEEDBACK_MS = 650
const CORRECT_MOVE_FEEDBACK_MS = 650
const MAX_HINT_COUNT = 3
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

const getPromotionPieceType = (color: ChessColor, piece: PromotionPiece) => {
  return promotionPieceTypes[color][piece]
}

const renderPromotionPiece = (color: ChessColor, piece: PromotionPiece) => {
  const PromotionPieceSvg = defaultPieces[getPromotionPieceType(color, piece)]

  return (
    <span className="promotion-piece-visual" aria-hidden="true">
      <PromotionPieceSvg />
    </span>
  )
}

const formatEloChange = (eloChange: number) => {
  return eloChange >= 0 ? `+${eloChange}` : `${eloChange}`
}

export function BoardPage({ isLoading, onOpenProfile, onOpenAbout, onSignOut }: BoardPageProps) {
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
  const [isSurrendering, setIsSurrendering] = useState(false)
  const [isReplayingOpponentMove, setIsReplayingOpponentMove] = useState(false)
  const [failedAttempts, setFailedAttempts] = useState(0)
  const [updatedElo, setUpdatedElo] = useState<number | null>(null)
  const [updatedEloChange, setUpdatedEloChange] = useState<number | null>(null)
  const [isPuzzleCompleted, setIsPuzzleCompleted] = useState(false)
  const [isPuzzleSurrendered, setIsPuzzleSurrendered] = useState(false)
  const [puzzleActionError, setPuzzleActionError] = useState<string | null>(null)
  const [incorrectMoveSquare, setIncorrectMoveSquare] = useState<Square | null>(null)
  const [correctMoveSquare, setCorrectMoveSquare] = useState<Square | null>(null)
  const [leaderboardRefreshKey, setLeaderboardRefreshKey] = useState(0)
  const replayTimeoutRef = useRef<number | null>(null)
  const incorrectMoveTimeoutRef = useRef<number | null>(null)
  const correctMoveTimeoutRef = useRef<number | null>(null)
  const isPuzzleFinished = isPuzzleCompleted || isPuzzleSurrendered

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

  const clearIncorrectMoveTimeout = () => {
    if (incorrectMoveTimeoutRef.current !== null) {
      window.clearTimeout(incorrectMoveTimeoutRef.current)
      incorrectMoveTimeoutRef.current = null
    }
  }

  const clearCorrectMoveTimeout = () => {
    if (correctMoveTimeoutRef.current !== null) {
      window.clearTimeout(correctMoveTimeoutRef.current)
      correctMoveTimeoutRef.current = null
    }
  }

  const showIncorrectMoveFeedback = (square: Square) => {
    clearIncorrectMoveTimeout()
    clearCorrectMoveTimeout()
    setCorrectMoveSquare(null)
    setIncorrectMoveSquare(square)

    return new Promise<void>((resolve) => {
      incorrectMoveTimeoutRef.current = window.setTimeout(() => {
        setIncorrectMoveSquare(null)
        incorrectMoveTimeoutRef.current = null
        resolve()
      }, INCORRECT_MOVE_FEEDBACK_MS)
    })
  }

  const showCorrectMoveFeedback = (square: Square) => {
    clearCorrectMoveTimeout()
    clearIncorrectMoveTimeout()
    setIncorrectMoveSquare(null)
    setCorrectMoveSquare(square)

    correctMoveTimeoutRef.current = window.setTimeout(() => {
      setCorrectMoveSquare(null)
      correctMoveTimeoutRef.current = null
    }, CORRECT_MOVE_FEEDBACK_MS)
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
    const response = await fetchWithAuth('/api/puzzles/verify-move', {
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

  const surrenderPuzzle = async (request: SurrenderPuzzleRequestDTO) => {
    const response = await fetchWithAuth('/api/puzzles/surrender', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error('Unable to surrender the puzzle.')
    }

    return (await response.json()) as SurrenderPuzzleResponseDTO
  }

  const applyPuzzleResult = (
    puzzleCompleted: boolean,
    newElo: number | null,
    eloChange: number | null,
    puzzleSurrendered = false
  ) => {
    setIsPuzzleCompleted(puzzleCompleted && !puzzleSurrendered)
    setIsPuzzleSurrendered(puzzleSurrendered)
    setUpdatedElo(newElo)
    setUpdatedEloChange(eloChange)
    setPuzzleActionError(null)

    if (puzzleCompleted && newElo !== null) {
      setLeaderboardRefreshKey((currentKey) => currentKey + 1)
    }
  }

  const tryPuzzleMove = async (from: Square, to: Square, promotion?: PromotionPiece) => {
    if (!puzzle || isPuzzleFinished) {
      return false
    }

    const nextGame = new Chess(game.fen())
    const moveResult = nextGame.move({ from, to, promotion })

    if (!moveResult) {
      return false
    }

    const previousGame = new Chess(game.fen())
    setGame(nextGame)
    setIsVerifyingMove(true)

    try {
      const verification = await verifyPuzzleMove({
        sessionId: puzzle.sessionId,
        puzzleId: puzzle.id,
        move: buildUciMove(from, to, promotion),
      })

      if (!verification.correct) {
        setFailedAttempts((currentAttempts) => currentAttempts + 1)
        clearSelection()
        await showIncorrectMoveFeedback(to)
        setGame(previousGame)
        return false
      }

      clearSelection()
      showCorrectMoveFeedback(to)

      if (verification.opponentMove) {
        const resolvedGame = new Chess(nextGame.fen())
        setIsReplayingOpponentMove(true)

        replayTimeoutRef.current = window.setTimeout(() => {
          replayPuzzleMove(resolvedGame, verification.opponentMove)
          setGame(resolvedGame)
          applyPuzzleResult(verification.puzzleCompleted, verification.newElo, verification.eloChange)
          setIsReplayingOpponentMove(false)
          replayTimeoutRef.current = null
        }, INITIAL_MOVE_DELAY_MS)
        return true
      }

      applyPuzzleResult(verification.puzzleCompleted, verification.newElo, verification.eloChange)
      return true
    } catch {
      clearSelection()
      setGame(previousGame)
      return false
    } finally {
      setIsVerifyingMove(false)
    }
  }

  const surrenderCurrentPuzzle = async () => {
    if (
      !puzzle ||
      isPuzzleFinished ||
      isPuzzleLoading ||
      isReplayingInitialMove ||
      isReplayingOpponentMove ||
      isVerifyingMove ||
      isSurrendering
    ) {
      return
    }

    clearSelection()
    setIsSurrendering(true)
    setPuzzleActionError(null)

    try {
      const surrender = await surrenderPuzzle({
        sessionId: puzzle.sessionId,
        puzzleId: puzzle.id,
      })
      applyPuzzleResult(surrender.puzzleCompleted, surrender.newElo, surrender.eloChange, true)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to surrender the puzzle.'
      setPuzzleActionError(message)
    } finally {
      setIsSurrendering(false)
    }
  }

  const loadRandomPuzzle = async () => {
    clearReplayTimeout()
    clearIncorrectMoveTimeout()
    clearCorrectMoveTimeout()
    setIsPuzzleLoading(true)
    setIsReplayingInitialMove(false)
    setIsReplayingOpponentMove(false)
    setIsVerifyingMove(false)
    setIsSurrendering(false)
    setIncorrectMoveSquare(null)
    setCorrectMoveSquare(null)
    setPuzzleHints([])
    setRevealedHintCount(0)
    setIsHintsLoading(false)
    setFailedAttempts(0)
    setUpdatedElo(null)
    setUpdatedEloChange(null)
    setIsPuzzleCompleted(false)
    setIsPuzzleSurrendered(false)
    setPuzzleActionError(null)
    clearSelection()

    const response = await fetchWithAuth('/api/puzzles/random')
    const nextPuzzle = (await response.json()) as PuzzleDTO
    const nextPosition = getPositionAfterInitialMove(nextPuzzle)

    setPuzzle(nextPuzzle)
    setBoardOrientation(nextPosition.turn() === 'w' ? 'white' : 'black')
    setGame(new Chess(nextPuzzle.fen))
    replayInitialMove(nextPuzzle)
    setIsPuzzleLoading(false)
  }

  const loadHints = async () => {
    if (!puzzle || isPuzzleFinished) {
      return
    }

    setIsHintsLoading(true)

    try {
      const response = await fetchWithAuth(`/api/puzzles/${puzzle.id}/hints?sessionId=${puzzle.sessionId}`)

      if (!response.ok) {
        throw new Error('Unable to load puzzle hints.')
      }

      const nextHints = (await response.json()) as string[]
      const sanitizedHints = nextHints.filter((hint) => hint.trim().length > 0)
      setPuzzleHints(sanitizedHints)
      setRevealedHintCount((currentCount) =>
        sanitizedHints.length > 0 ? Math.min(currentCount + 1, sanitizedHints.length, MAX_HINT_COUNT) : 0
      )
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
    return () => {
      clearReplayTimeout()
      clearIncorrectMoveTimeout()
      clearCorrectMoveTimeout()
    }
    // The initial puzzle should be loaded once when the board page mounts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handlePromotionChoice = (promotion: PromotionPiece) => {
    if (!pendingPromotion) {
      return
    }

    void tryPuzzleMove(pendingPromotion.from, pendingPromotion.to, promotion)
  }

  const handleSquareClick = ({ square }: { square: string }) => {
    if (isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isPuzzleFinished) {
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
    if (!targetSquare || isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isPuzzleFinished) {
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

  const renderSquare = ({ square, children }: { square: string; children?: ReactNode }) => {
    const isIncorrectMove = square === incorrectMoveSquare
    const isCorrectMove = square === correctMoveSquare

    return (
      <div className="board-square-content" style={squareStyles[square]}>
        {children}
        {isCorrectMove ? <span className="correct-move-badge" aria-hidden="true" /> : null}
        {isIncorrectMove ? <span className="incorrect-move-badge" aria-hidden="true" /> : null}
      </div>
    )
  }

  const boardInteractionDisabled =
    isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isSurrendering || isPuzzleFinished
  const displayedHints = puzzleHints.slice(0, revealedHintCount)
  const areHintsExhausted = puzzleHints.length > 0 && revealedHintCount >= Math.min(puzzleHints.length, MAX_HINT_COUNT)
  const hasFinalEloResult = isPuzzleFinished && updatedElo !== null && updatedEloChange !== null
  const puzzleActionLabel = isPuzzleFinished ? 'New puzzle' : isSurrendering ? 'Surrendering...' : 'Surrender'
  const puzzleActionDisabled = isPuzzleFinished
    ? isPuzzleLoading
    : !puzzle || isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isSurrendering

  return (
    <main className="board-shell">
      <section className="board-header-card">
        <div className="page-brand">
          <img className="page-logo" src={appMark} alt="" aria-hidden="true" />
          <h1>ChessAIssistant</h1>
        </div>
        <div className="board-actions">
          <button type="button" className="secondary-action compact-action" onClick={onOpenProfile}>
            Profile
          </button>
          <button type="button" className="secondary-action compact-action" onClick={() => void onSignOut()} disabled={isLoading}>
            Sign out
          </button>
        </div>
      </section>

      <section className="board-layout">
        <Leaderboard refreshKey={leaderboardRefreshKey} />

        <section className="training-layout" aria-label="Puzzle board and controls">
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
                squareRenderer: renderSquare,
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
            <p className="puzzle-rating">
              {isPuzzleLoading || !puzzle ? 'Puzzle Elo loading...' : `Puzzle Elo: ${puzzle.rating}`}
            </p>
            <p className="panel-copy">
              {isReplayingInitialMove
                ? `Showing the starting position. The opponent move ${puzzle?.initialMove ?? ''} will play in a moment.`
                : isReplayingOpponentMove
                  ? 'Waiting for the opponent reply.'
                : isVerifyingMove
                  ? 'Verifying your move with the backend.'
                : isSurrendering
                  ? 'Surrendering the puzzle and updating your Elo.'
                : isPuzzleSurrendered
                  ? 'Puzzle surrendered. Elo has been updated as a failed attempt. Load a new puzzle to continue.'
                : isPuzzleCompleted
                  ? failedAttempts > 0
                    ? updatedElo === null
                      ? 'Puzzle completed with mistakes. It will count as unsolved.'
                      : 'Puzzle completed with mistakes. It will count as unsolved.'
                    : updatedElo === null
                      ? 'Puzzle solved. Load a new puzzle to continue.'
                      : 'Puzzle solved. Load a new puzzle to continue.'
                : pendingPromotion
                  ? `Choose a promotion piece for ${pendingPromotion.to}.`
                  : selectedSquare
                    ? `Selected square: ${selectedSquare}`
                    : isPuzzleLoading
                      ? 'Fetching a random puzzle from the backend.'
                      : 'Drag a piece or click a square to see legal moves.'}
            </p>
            {puzzleActionError ? <p className="feedback error">{puzzleActionError}</p> : null}
            {hasFinalEloResult ? (
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
                    {renderPromotionPiece(pendingPromotion.color, promotion)}
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
            <button
              type="button"
              className={`secondary-action compact-action ${isPuzzleFinished ? '' : 'surrender-action'}`}
              onClick={handlePuzzleAction}
              disabled={puzzleActionDisabled}
            >
              {puzzleActionLabel}
            </button>
          </div>

          <button
              type="button"
              className="secondary-action compact-action"
              onClick={askHints}
              disabled={!puzzle || isHintsLoading || isPuzzleFinished || areHintsExhausted}
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
      </section>
      <AboutLink onOpenAbout={onOpenAbout} />
    </main>
  )

  function handlePuzzleAction() {
    if (isPuzzleFinished) {
      void loadRandomPuzzle()
      return
    }

    void surrenderCurrentPuzzle()
  }

  function askHints() {
    void loadHints()
  }
}
