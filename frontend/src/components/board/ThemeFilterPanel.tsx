import type {PuzzleThemeDTO} from '../../api/puzzles'

type ThemeFilterPanelProps = {
  themes: PuzzleThemeDTO[]
  selectedThemeId: string | null
  isOpen: boolean
  search: string
  isLoading: boolean
  error: string | null
  onToggleOpen: () => void
  onSearchChange: (search: string) => void
  onToggleTheme: (themeId: string) => void
  onReset: () => void
}

export function ThemeFilterPanel({
  themes,
  selectedThemeId,
  isOpen,
  search,
  isLoading,
  error,
  onToggleOpen,
  onSearchChange,
  onToggleTheme,
  onReset,
}: ThemeFilterPanelProps) {
  const normalizedSearch = search.trim().toLowerCase()
  const visibleThemes = normalizedSearch
    ? themes.filter((theme) =>
      theme.label.toLowerCase().includes(normalizedSearch) || theme.id.toLowerCase().includes(normalizedSearch)
    )
    : themes
  const selectedThemeLabel = selectedThemeId
    ? themes.find((theme) => theme.id === selectedThemeId)?.label ?? selectedThemeId
    : 'Any theme'

  return (
    <div className="board-panel theme-filter-panel">
      <div className="theme-filter-header">
        <button
          type="button"
          className="theme-toggle-action"
          onClick={onToggleOpen}
          aria-expanded={isOpen}
        >
          <span className="theme-filter-heading">
            <span className="panel-title">Theme</span>
            <strong>{selectedThemeLabel}</strong>
          </span>
          <span className={`theme-toggle-indicator ${isOpen ? 'open' : ''}`} aria-hidden="true" />
        </button>

        {selectedThemeId ? (
          <button type="button" className="inline-action theme-reset-action" onClick={onReset}>
            Clear
          </button>
        ) : null}
      </div>

      {isOpen ? (
        <div className="theme-filter-body">
          <input
            className="theme-search-input"
            type="search"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search themes"
            aria-label="Search puzzle themes"
          />

          {error ? <p className="error-text theme-state">{error}</p> : null}

          <div className="theme-filter-list">
            {isLoading ? <p className="theme-state">Loading themes...</p> : null}
            {!isLoading && visibleThemes.length === 0 ? <p className="theme-state">No themes found.</p> : null}
            {visibleThemes.map((theme) => {
              const selected = selectedThemeId === theme.id

              return (
                <label key={theme.id} className={`theme-option ${selected ? 'selected' : ''}`}>
                  <input
                    type="checkbox"
                    checked={selected}
                    onChange={() => onToggleTheme(theme.id)}
                  />
                  <span className="theme-option-copy">
                    <strong>{theme.label}</strong>
                    <code>{theme.id}</code>
                  </span>
                </label>
              )
            })}
          </div>
        </div>
      ) : null}
    </div>
  )
}
