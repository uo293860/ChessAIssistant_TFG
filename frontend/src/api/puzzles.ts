import {fetchWithAuth} from './apiClient'

export type PuzzleDTO = {
  id: string
  sessionId: number
  fen: string
  rating: number
  themes: string
  gameUrl: string
  initialMove: string
  hintEloPenalty: number
}

export type PuzzleThemeDTO = {
  id: string
  label: string
}

export type PuzzleHintRequestDTO = {
  sessionId: number
}

export type PuzzleHintResponseDTO = {
  hint: string
  hintNumber: number
  maxHintCount: number
  hintsExhausted: boolean
}

export type VerifyPuzzleMoveRequestDTO = {
  sessionId: number
  puzzleId: string
  move: string
}

export type VerifyPuzzleMoveResponseDTO = {
  correct: boolean
  opponentMove: string
  nextMoveIndex: number
  puzzleCompleted: boolean
  newElo: number | null
  eloChange: number | null
}

export type SurrenderPuzzleRequestDTO = {
  sessionId: number
  puzzleId: string
}

export type SurrenderPuzzleResponseDTO = {
  puzzleCompleted: boolean
  newElo: number | null
  eloChange: number | null
}

const buildRandomPuzzlePath = (themeId: string | null) => {
  const params = new URLSearchParams()

  if (themeId) {
    params.set('theme', themeId)
  }

  const query = params.toString()
  return query ? `/api/puzzles/random?${query}` : '/api/puzzles/random'
}

export const fetchRandomPuzzle = async (themeId: string | null) => {
  const response = await fetchWithAuth(buildRandomPuzzlePath(themeId))

  if (!response.ok) {
    throw new Error('Unable to load a puzzle.')
  }

  return (await response.json()) as PuzzleDTO
}

export const fetchRandomFailedPuzzle = async () => {
  const response = await fetchWithAuth('/api/puzzles/failed/random', {
    method: 'POST',
  })

  if (!response.ok) {
    throw new Error('No failed puzzle is available to retry.')
  }

  return (await response.json()) as PuzzleDTO
}

export const fetchPuzzleThemes = async () => {
  const response = await fetchWithAuth('/api/puzzles/themes')

  if (!response.ok) {
    throw new Error('Unable to load puzzle themes.')
  }

  return (await response.json()) as PuzzleThemeDTO[]
}

export const verifyPuzzleMove = async (request: VerifyPuzzleMoveRequestDTO) => {
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

export const surrenderPuzzle = async (request: SurrenderPuzzleRequestDTO) => {
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

export const requestPuzzleHint = async (puzzleId: string, request: PuzzleHintRequestDTO) => {
  const response = await fetchWithAuth(`/api/puzzles/${puzzleId}/hints`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    throw new Error('Unable to load a puzzle hint.')
  }

  return (await response.json()) as PuzzleHintResponseDTO
}
