type HintsPanelProps = {
  isLoading: boolean
  error: string | null
  hints: string[]
}

export function HintsPanel({isLoading, error, hints}: HintsPanelProps) {
  return (
    <div className="board-panel">
      <p className="panel-title">Hints</p>
      <div className="moves-output">
        {isLoading
          ? 'Loading hints...'
          : error
            ? <p className="error-text">{error}</p>
            : hints.length > 0
              ? hints.map((hint, index) => <p key={`${index}-${hint}`}>{hint}</p>)
              : 'Ask for AIssistance to load hints for this puzzle.'}
      </div>
    </div>
  )
}
