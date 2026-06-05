import appMark from '../assets/logo.png'
import {FaqAccordionItem} from '../components/FaqAccordionItem'

type AboutPageProps = {
  isAuthenticated: boolean
  onOpenBoard: () => void
  onOpenProfile: () => void
  onOpenAuth: () => void
  onSignOut: () => Promise<void>
}

type FaqItem = {
  question: string
  answer: string
}

const faqItems: FaqItem[] = [
  {
    question: 'How do I start training?',
    answer:
      'Sign in, open the board, and the application will load a tactical puzzle adapted to your current Elo rating.',
  },
  {
    question: 'How do I solve a puzzle?',
    answer:
      'Drag a piece or click a piece and then its destination square. The backend verifies each move and plays the opponent replies when the puzzle continues.',
  },
  {
    question: 'What happens when I request a hint?',
    answer:
      'The AI assistant unlocks one textual hint at a time. Hints have an Elo cost, shown directly in the hint button before you request one.',
  },
  {
    question: 'How does Elo work?',
    answer:
      'The board shows the maximum Elo gain for solving the puzzle cleanly. Mistakes and hints reduce the final result, and the final Elo variation is shown after completion.',
  },
  {
    question: 'Where can I review my progress?',
    answer:
      'Open the profile page to see your account data, current Elo, success rate, and Elo history chart.',
  },
]

export function AboutPage({ isAuthenticated, onOpenBoard, onOpenProfile, onOpenAuth, onSignOut }: AboutPageProps) {
  return (
    <main className="about-shell">
      <section className="about-header">
        <div className="profile-brand">
          <img className="profile-logo" src={appMark} alt="ChessAIssistant logo" />
          <div>
            <p className="panel-title">About the project</p>
            <h1>ChessAIssistant</h1>
          </div>
        </div>
        <div className="profile-actions">
          {isAuthenticated ? (
            <>
              <button type="button" className="secondary-action compact-action" onClick={onOpenBoard}>
                Board
              </button>
              <button type="button" className="secondary-action compact-action" onClick={onOpenProfile}>
                Profile
              </button>
              <button type="button" className="secondary-action compact-action" onClick={() => void onSignOut()}>
                Sign out
              </button>
            </>
          ) : (
            <button type="button" className="secondary-action compact-action" onClick={onOpenAuth}>
              Sign in
            </button>
          )}
        </div>
      </section>

      <section className="about-hero-card">
        <p className="eyebrow">Academic project</p>
        <h2>AI-assisted chess tactics training</h2>
        <p>
          ChessAIssistant is an academic final degree project for Software Engineering at the University of Oviedo. It
          explores how a web application can combine real tactical chess puzzles, adaptive Elo-based training, and
          generative AI hints to simulate part of the guidance a human chess coach would provide.
        </p>
      </section>

      <section className="about-grid" aria-label="Project information">
        <article className="about-card">
          <p className="panel-title">Training model</p>
          <strong>Adaptive puzzles</strong>
          <p>Puzzles are selected around the player rating so the training difficulty stays close to the user level.</p>
        </article>
        <article className="about-card">
          <p className="panel-title">AI support</p>
          <strong>Conceptual hints</strong>
          <p>The assistant is designed to provide guidance without immediately revealing the full solution.</p>
        </article>
        <article className="about-card">
          <p className="panel-title">Stack</p>
          <strong>Spring Boot and React</strong>
          <p>The project uses a Java backend, a React and TypeScript frontend, Firebase authentication, and AI services.</p>
        </article>
      </section>

      <section className="faq-section" aria-labelledby="faq-title">
        <div>
          <p className="panel-title">FAQ</p>
          <h2 id="faq-title">Using the web application</h2>
        </div>
        <div className="faq-list">
          {faqItems.map((item) => (
            <FaqAccordionItem key={item.question} question={item.question} answer={item.answer} />
          ))}
        </div>
      </section>
    </main>
  )
}
