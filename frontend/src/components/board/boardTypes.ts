import type {Square} from 'chess.js'

export type ChessColor = 'w' | 'b'
export type PromotionPiece = 'b' | 'n' | 'r' | 'q'

export type PendingPromotion = {
  from: Square
  to: Square
  color: ChessColor
}
