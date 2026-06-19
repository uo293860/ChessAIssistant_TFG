type PuzzleControlsProps = {
  isPuzzleFinished: boolean
  puzzleActionLabel: string
  puzzleActionDisabled: boolean
  repeatActionDisabled: boolean
  hintActionLabel: string
  hintActionDisabled: boolean
  onRepeatFailedPuzzle: () => void
  onPuzzleAction: () => void
  onAskHints: () => void
}

export function PuzzleControls({
  isPuzzleFinished,
  puzzleActionLabel,
  puzzleActionDisabled,
  repeatActionDisabled,
  hintActionLabel,
  hintActionDisabled,
  onRepeatFailedPuzzle,
  onPuzzleAction,
  onAskHints,
}: PuzzleControlsProps) {
  return (
    <>
      <div className={isPuzzleFinished ? 'puzzle-result-actions' : undefined}>
        {isPuzzleFinished ? (
          <button
            type="button"
            className="secondary-action compact-action"
            onClick={onRepeatFailedPuzzle}
            disabled={repeatActionDisabled}
          >
            Random failed puzzle
          </button>
        ) : null}
        <button
          type="button"
          className={`secondary-action compact-action ${isPuzzleFinished ? '' : 'surrender-action'}`}
          onClick={onPuzzleAction}
          disabled={puzzleActionDisabled}
        >
          {puzzleActionLabel}
        </button>
      </div>

      <button
        type="button"
        className="secondary-action compact-action"
        onClick={onAskHints}
        disabled={hintActionDisabled}
      >
        {hintActionLabel}
      </button>
    </>
  )
}
