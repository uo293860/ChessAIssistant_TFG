type AboutLinkProps = {
  onOpenAbout: () => void
}

export function AboutLink({ onOpenAbout }: AboutLinkProps) {
  return (
    <footer className="about-link-footer">
      <button type="button" className="about-link-button" onClick={onOpenAbout}>
        About ChessAIssistant
      </button>
    </footer>
  )
}
