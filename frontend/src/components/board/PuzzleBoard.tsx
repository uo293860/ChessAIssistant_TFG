import type {CSSProperties, ReactNode} from 'react'
import type {Chess, Square} from 'chess.js'
import {Chessboard} from 'react-chessboard'

type BoardOrientation = 'white' | 'black'

type SquareClickHandler = (args: {square: string}) => void

type PieceDropHandler = (args: {
  sourceSquare: string
  targetSquare: string | null
}) => boolean

type PuzzleBoardProps = {
  game: Chess
  boardOrientation: BoardOrientation
  boardInteractionDisabled: boolean
  selectedSquare: Square | null
  legalTargets: Square[]
  incorrectMoveSquare: Square | null
  correctMoveSquare: Square | null
  onSquareClick: SquareClickHandler
  onPieceDrop: PieceDropHandler
}

const lightSquareColor = '#f0f0f0'
const darkSquareColor = '#8f8f8f'

const buildSquareStyles = (selectedSquare: Square | null, legalTargets: Square[]) => {
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

  return squareStyles
}

export function PuzzleBoard({
  game,
  boardOrientation,
  boardInteractionDisabled,
  selectedSquare,
  legalTargets,
  incorrectMoveSquare,
  correctMoveSquare,
  onSquareClick,
  onPieceDrop,
}: PuzzleBoardProps) {
  const squareStyles = buildSquareStyles(selectedSquare, legalTargets)

  const renderSquare = ({square, children}: {square: string; children?: ReactNode}) => {
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

  return (
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
            darkSquareStyle: {backgroundColor: darkSquareColor},
            lightSquareStyle: {backgroundColor: lightSquareColor},
            squareStyles,
            squareRenderer: renderSquare,
            canDragPiece: ({piece}) => {
              if (boardInteractionDisabled || !piece) {
                return false
              }

              return piece.pieceType[0].toLowerCase() === game.turn()
            },
            onSquareClick,
            onPieceDrop,
          }}
        />
      </div>
    </div>
  )
}
