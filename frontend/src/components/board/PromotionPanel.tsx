import {defaultPieces} from 'react-chessboard'
import type {ChessColor, PendingPromotion, PromotionPiece} from './boardTypes'

type PromotionPanelProps = {
  pendingPromotion: PendingPromotion | null
  onPromotionChoice: (promotion: PromotionPiece) => void
  onCancel: () => void
}

type PromotionPieceType = 'wB' | 'wN' | 'wR' | 'wQ' | 'bB' | 'bN' | 'bR' | 'bQ'

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

export function PromotionPanel({pendingPromotion, onPromotionChoice, onCancel}: PromotionPanelProps) {
  if (!pendingPromotion) {
    return null
  }

  return (
    <div className="board-panel">
      <p className="panel-title">Promotion</p>
      <strong>Select the piece for pawn promotion.</strong>
      <div className="promotion-actions">
        {promotionChoices.map((promotion) => (
          <button
            key={promotion}
            type="button"
            className="promotion-action"
            onClick={() => onPromotionChoice(promotion)}
            aria-label={`Promote to ${getPieceLabel(promotion)}`}
          >
            {renderPromotionPiece(pendingPromotion.color, promotion)}
            <span className="promotion-label">{getPieceLabel(promotion)}</span>
          </button>
        ))}
      </div>
      <button type="button" className="secondary-action promotion-cancel" onClick={onCancel}>
        Cancel promotion
      </button>
    </div>
  )
}
