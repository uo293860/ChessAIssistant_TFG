import {AboutLink} from '../components/AboutLink'
import {BoardHeader} from '../components/board/BoardHeader'
import {GameStatePanel} from '../components/board/GameStatePanel'
import {HintsPanel} from '../components/board/HintsPanel'
import {PromotionPanel} from '../components/board/PromotionPanel'
import {PuzzleBoard} from '../components/board/PuzzleBoard'
import {PuzzleControls} from '../components/board/PuzzleControls'
import {ThemeFilterPanel} from '../components/board/ThemeFilterPanel'
import {usePuzzleTrainer} from '../components/board/usePuzzleTrainer'
import {Leaderboard} from '../components/Leaderboard'

type BoardPageProps = {
  isLoading: boolean
  onOpenProfile: () => void
  onOpenAbout: () => void
  onSignOut: () => Promise<void>
}

export function BoardPage({isLoading, onOpenProfile, onOpenAbout, onSignOut}: BoardPageProps) {
  const trainer = usePuzzleTrainer()

  return (
    <main className="board-shell">
      <BoardHeader isLoading={isLoading} onOpenProfile={onOpenProfile} onSignOut={onSignOut} />

      <section className="board-layout">
        <Leaderboard refreshKey={trainer.leaderboardRefreshKey} />

        <section className="training-layout" aria-label="Puzzle board and controls">
          <PuzzleBoard {...trainer.puzzleBoard} />

          <aside className="board-sidebar">
            <ThemeFilterPanel {...trainer.themeFilter} />
            <GameStatePanel {...trainer.gameState} />
            <PromotionPanel {...trainer.promotion} />
            <PuzzleControls {...trainer.controls} />
            <HintsPanel {...trainer.hints} />
          </aside>
        </section>
      </section>

      <AboutLink onOpenAbout={onOpenAbout} />
    </main>
  )
}
