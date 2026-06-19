import appMark from '../../assets/logo.png'

type BoardHeaderProps = {
  isLoading: boolean
  onOpenProfile: () => void
  onSignOut: () => Promise<void>
}

export function BoardHeader({isLoading, onOpenProfile, onSignOut}: BoardHeaderProps) {
  return (
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
  )
}
