import {useEffect, useRef, useState} from 'react'
import type {Square} from 'chess.js'
import {Chess} from 'chess.js'
import {
  fetchPuzzleThemes,
  fetchRandomFailedPuzzle,
  fetchRandomPuzzle,
  requestPuzzleHint,
  surrenderPuzzle,
  verifyPuzzleMove,
} from '../../api/puzzles'
import type {PuzzleDTO, PuzzleThemeDTO} from '../../api/puzzles'
import type {PendingPromotion, PromotionPiece} from './boardTypes'

const INITIAL_MOVE_DELAY_MS = 1200
const INCORRECT_MOVE_FEEDBACK_MS = 650
const CORRECT_MOVE_FEEDBACK_MS = 650
const MAX_HINT_COUNT = 3

export function usePuzzleTrainer() {
  const [game, setGame] = useState(() => new Chess())
  const [boardOrientation, setBoardOrientation] = useState<'white' | 'black'>('white')
  const [puzzle, setPuzzle] = useState<PuzzleDTO | null>(null)
  const [puzzleThemes, setPuzzleThemes] = useState<PuzzleThemeDTO[]>([])
  const [selectedThemeId, setSelectedThemeId] = useState<string | null>(null)
  const [isThemeFilterOpen, setIsThemeFilterOpen] = useState(false)
  const [themeSearch, setThemeSearch] = useState('')
  const [themeLoadError, setThemeLoadError] = useState<string | null>(null)
  const [isThemesLoading, setIsThemesLoading] = useState(false)
  const [isPuzzleLoading, setIsPuzzleLoading] = useState(true)
  const [isReplayingInitialMove, setIsReplayingInitialMove] = useState(false)
  const [selectedSquare, setSelectedSquare] = useState<Square | null>(null)
  const [legalTargets, setLegalTargets] = useState<Square[]>([])
  const [pendingPromotion, setPendingPromotion] = useState<PendingPromotion | null>(null)
  const [puzzleHints, setPuzzleHints] = useState<string[]>([])
  const [hintsExhausted, setHintsExhausted] = useState(false)
  const [hintLoadError, setHintLoadError] = useState<string | null>(null)
  const [isHintsLoading, setIsHintsLoading] = useState(false)
  const [isVerifyingMove, setIsVerifyingMove] = useState(false)
  const [isSurrendering, setIsSurrendering] = useState(false)
  const [isReplayingOpponentMove, setIsReplayingOpponentMove] = useState(false)
  const [failedAttempts, setFailedAttempts] = useState(0)
  const [updatedElo, setUpdatedElo] = useState<number | null>(null)
  const [updatedEloChange, setUpdatedEloChange] = useState<number | null>(null)
  const [isPuzzleCompleted, setIsPuzzleCompleted] = useState(false)
  const [isPuzzleSurrendered, setIsPuzzleSurrendered] = useState(false)
  const [isRepeatingFailedPuzzle, setIsRepeatingFailedPuzzle] = useState(false)
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
    const legalMoves = game.moves({square, verbose: true})
    setSelectedSquare(square)
    setLegalTargets(legalMoves.map((move) => move.to))
  }

  const buildUciMove = (from: Square, to: Square, promotion?: PromotionPiece) => {
    return `${from}${to}${promotion ?? ''}`
  }

  const loadPuzzleThemes = async () => {
    setIsThemesLoading(true)
    setThemeLoadError(null)

    try {
      const themes = await fetchPuzzleThemes()
      setPuzzleThemes(themes.filter((theme) => theme.id.trim() && theme.label.trim()))
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to load puzzle themes.'
      setThemeLoadError(message)
    } finally {
      setIsThemesLoading(false)
    }
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
    const moveResult = nextGame.move({from, to, promotion})

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

  const resetPuzzlePlayState = (repeatingFailedPuzzle: boolean) => {
    clearReplayTimeout()
    clearIncorrectMoveTimeout()
    clearCorrectMoveTimeout()
    setIsReplayingInitialMove(false)
    setIsReplayingOpponentMove(false)
    setIsVerifyingMove(false)
    setIsSurrendering(false)
    setIncorrectMoveSquare(null)
    setCorrectMoveSquare(null)
    setPuzzleHints([])
    setHintsExhausted(false)
    setHintLoadError(null)
    setIsHintsLoading(false)
    setFailedAttempts(0)
    setUpdatedElo(null)
    setUpdatedEloChange(null)
    setIsPuzzleCompleted(false)
    setIsPuzzleSurrendered(false)
    setIsRepeatingFailedPuzzle(repeatingFailedPuzzle)
    setPuzzleActionError(null)
    clearSelection()
  }

  const startPuzzle = (nextPuzzle: PuzzleDTO) => {
    const nextPosition = getPositionAfterInitialMove(nextPuzzle)

    setPuzzle(nextPuzzle)
    setBoardOrientation(nextPosition.turn() === 'w' ? 'white' : 'black')
    setGame(new Chess(nextPuzzle.fen))
    replayInitialMove(nextPuzzle)
  }

  const loadRandomPuzzle = async () => {
    setIsPuzzleLoading(true)
    resetPuzzlePlayState(false)

    try {
      const nextPuzzle = await fetchRandomPuzzle(selectedThemeId)
      startPuzzle(nextPuzzle)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to load a puzzle.'
      setPuzzleActionError(message)
    } finally {
      setIsPuzzleLoading(false)
    }
  }

  const loadRepeatedFailedPuzzle = async () => {
    if (!isPuzzleFinished || isPuzzleLoading || isVerifyingMove || isSurrendering) {
      return
    }

    setIsPuzzleLoading(true)
    setPuzzleActionError(null)

    try {
      const repeatedPuzzle = await fetchRandomFailedPuzzle()
      resetPuzzlePlayState(true)
      startPuzzle(repeatedPuzzle)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'No failed puzzle is available to retry.'
      setPuzzleActionError(message)
    } finally {
      setIsPuzzleLoading(false)
    }
  }

  const loadHints = async () => {
    if (!puzzle || isPuzzleFinished || isHintsLoading || hintsExhausted) {
      return
    }

    setIsHintsLoading(true)
    setHintLoadError(null)

    try {
      const nextHint = await requestPuzzleHint(puzzle.id, {sessionId: puzzle.sessionId})
      const sanitizedHint = nextHint.hint.trim()

      if (!sanitizedHint) {
        throw new Error('Received an empty puzzle hint.')
      }

      const resolvedMaxHintCount = nextHint.maxHintCount > 0 ? nextHint.maxHintCount : MAX_HINT_COUNT
      setPuzzleHints((currentHints) => [...currentHints, sanitizedHint].slice(0, resolvedMaxHintCount))
      setHintsExhausted(nextHint.hintsExhausted || nextHint.hintNumber >= resolvedMaxHintCount)
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to load puzzle hints.'
      setHintLoadError(message)
    } finally {
      setIsHintsLoading(false)
    }
  }

  useEffect(() => {
    void loadPuzzleThemes()
    void loadRandomPuzzle()
    return () => {
      clearReplayTimeout()
      clearIncorrectMoveTimeout()
      clearCorrectMoveTimeout()
    }
    // The initial puzzle should be loaded once when the board page mounts.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const toggleThemeSelection = (themeId: string) => {
    setSelectedThemeId((currentThemeId) => currentThemeId === themeId ? null : themeId)
    setIsThemeFilterOpen(false)
  }

  const resetThemeFilter = () => {
    setSelectedThemeId(null)
    setIsThemeFilterOpen(false)
  }

  const handlePromotionChoice = (promotion: PromotionPiece) => {
    if (!pendingPromotion) {
      return
    }

    void tryPuzzleMove(pendingPromotion.from, pendingPromotion.to, promotion)
  }

  const handleSquareClick = ({square}: {square: string}) => {
    if (isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isPuzzleFinished) {
      return
    }

    const boardSquare = square as Square
    const piece = game.get(boardSquare)

    if (selectedSquare && legalTargets.includes(boardSquare)) {
      const matchingMoves = game.moves({square: selectedSquare, verbose: true}).filter((move) => move.to === boardSquare)
      const requiresPromotion = matchingMoves.some((move) => move.promotion)

      if (requiresPromotion) {
        const selectedPiece = game.get(selectedSquare)

        if (selectedPiece) {
          setPendingPromotion({from: selectedSquare, to: boardSquare, color: selectedPiece.color})
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
    const matchingMoves = game.moves({square: from, verbose: true}).filter((move) => move.to === to)

    if (matchingMoves.length === 0) {
      return false
    }

    const requiresPromotion = matchingMoves.some((move) => move.promotion)

    if (requiresPromotion) {
      const draggedPiece = game.get(from)

      if (draggedPiece) {
        setPendingPromotion({from, to, color: draggedPiece.color})
        setSelectedSquare(from)
        setLegalTargets([to])
      }

      return false
    }

    void tryPuzzleMove(from, to)
    return false
  }

  const handlePuzzleAction = () => {
    if (isPuzzleFinished) {
      void loadRandomPuzzle()
      return
    }

    void surrenderCurrentPuzzle()
  }

  const askHints = () => {
    void loadHints()
  }

  const boardInteractionDisabled =
    isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isSurrendering || isPuzzleFinished
  const puzzleActionLabel = isPuzzleFinished ? 'New puzzle' : isSurrendering ? 'Surrendering...' : 'Surrender'
  const hintActionLabel = puzzle && !isRepeatingFailedPuzzle ? `Ask for AIssistance (-${puzzle.hintEloPenalty} Elo)` : 'Ask for AIssistance'
  const puzzleActionDisabled = isPuzzleFinished
    ? isPuzzleLoading
    : !puzzle || isPuzzleLoading || isReplayingInitialMove || isReplayingOpponentMove || isVerifyingMove || isSurrendering
  const repeatActionDisabled = !isPuzzleFinished || isPuzzleLoading || isVerifyingMove || isSurrendering
  const hintActionDisabled = !puzzle || isHintsLoading || isPuzzleFinished || hintsExhausted

  return {
    leaderboardRefreshKey,
    puzzleBoard: {
      game,
      boardOrientation,
      boardInteractionDisabled,
      selectedSquare,
      legalTargets,
      incorrectMoveSquare,
      correctMoveSquare,
      onSquareClick: handleSquareClick,
      onPieceDrop: handlePieceDrop,
    },
    themeFilter: {
      themes: puzzleThemes,
      selectedThemeId,
      isOpen: isThemeFilterOpen,
      search: themeSearch,
      isLoading: isThemesLoading,
      error: themeLoadError,
      onToggleOpen: () => setIsThemeFilterOpen((isOpen) => !isOpen),
      onSearchChange: setThemeSearch,
      onToggleTheme: toggleThemeSelection,
      onReset: resetThemeFilter,
    },
    gameState: {
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
      puzzleActionError,
    },
    promotion: {
      pendingPromotion,
      onPromotionChoice: handlePromotionChoice,
      onCancel: clearSelection,
    },
    controls: {
      isPuzzleFinished,
      puzzleActionLabel,
      puzzleActionDisabled,
      repeatActionDisabled,
      hintActionLabel,
      hintActionDisabled,
      onRepeatFailedPuzzle: () => void loadRepeatedFailedPuzzle(),
      onPuzzleAction: handlePuzzleAction,
      onAskHints: askHints,
    },
    hints: {
      isLoading: isHintsLoading,
      error: hintLoadError,
      hints: puzzleHints,
    },
  }
}
