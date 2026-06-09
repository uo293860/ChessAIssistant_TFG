import type {FormEvent} from 'react'
import {useEffect, useMemo, useState} from 'react'
import type {User} from 'firebase/auth'
import {
  createUserWithEmailAndPassword,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from 'firebase/auth'
import {FirebaseError} from 'firebase/app'
import './App.css'
import {auth} from './firebase'
import appLogo from './assets/logo.png'
import {AboutLink} from './components/AboutLink'
import {AboutPage} from './pages/AboutPage'
import {BoardPage} from './pages/BoardPage'
import {ProfilePage} from './pages/ProfilePage'

type AuthMode = 'login' | 'register'
type AppRoute = '/' | '/board' | '/profile' | '/about'

const AUTH_ROUTE: AppRoute = '/'
const BOARD_ROUTE: AppRoute = '/board'
const PROFILE_ROUTE: AppRoute = '/profile'
const ABOUT_ROUTE: AppRoute = '/about'
const MIN_PASSWORD_LENGTH = 6

const getFirebaseAuthErrorMessage = (firebaseError: unknown, fallbackMessage: string) => {
  if (!(firebaseError instanceof FirebaseError)) {
    return fallbackMessage
  }

  switch (firebaseError.code) {
    case 'auth/email-already-in-use':
      return 'An account already exists with this email. Sign in instead or use a different email address.'
    case 'auth/invalid-email':
      return 'Enter a valid email address.'
    case 'auth/invalid-credential':
    case 'auth/user-not-found':
    case 'auth/wrong-password':
      return 'The email or password is incorrect. Check your details and try again.'
    case 'auth/popup-closed-by-user':
      return 'Google sign-in was closed before it finished.'
    case 'auth/popup-blocked':
      return 'Your browser blocked the Google sign-in window. Allow pop-ups for this site and try again.'
    case 'auth/account-exists-with-different-credential':
      return 'This email is already linked to a different sign-in method. Use the method you used before.'
    case 'auth/network-request-failed':
      return 'We could not reach the sign-in service. Check your connection and try again.'
    case 'auth/too-many-requests':
      return 'Access is temporarily limited after several failed attempts. Wait a moment and try again.'
    case 'auth/weak-password':
      return `Use a password with at least ${MIN_PASSWORD_LENGTH} characters and a capital letter.`
    case 'auth/operation-not-allowed':
      return 'This sign-in method is not enabled yet. Contact support if the problem continues.'
    default:
      return fallbackMessage
  }
}

const getCurrentRoute = (): AppRoute => {
  if (window.location.pathname === BOARD_ROUTE) {
    return BOARD_ROUTE
  }

  if (window.location.pathname === PROFILE_ROUTE) {
    return PROFILE_ROUTE
  }

  if (window.location.pathname === ABOUT_ROUTE) {
    return ABOUT_ROUTE
  }

  return AUTH_ROUTE
}

const navigateTo = (route: AppRoute, replace = false) => {
  if (window.location.pathname === route) {
    return
  }

  const method = replace ? window.history.replaceState : window.history.pushState
  method.call(window.history, null, '', route)
}

function App() {
  const googleProvider = useMemo(() => new GoogleAuthProvider(), [])
  const [mode, setMode] = useState<AuthMode>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [isAuthReady, setIsAuthReady] = useState(false)
  const [feedback, setFeedback] = useState('')
  const [error, setError] = useState('')
  const [route, setRoute] = useState<AppRoute>(getCurrentRoute)

  useEffect(() => {
    const handlePopState = () => {
      setRoute(getCurrentRoute())
    }

    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user)
      setIsAuthReady(true)

      if (user) {
        if (getCurrentRoute() === AUTH_ROUTE) {
          navigateTo(BOARD_ROUTE, true)
          setRoute(BOARD_ROUTE)
        }
        return
      }

      if (getCurrentRoute() === BOARD_ROUTE || getCurrentRoute() === PROFILE_ROUTE) {
        navigateTo(AUTH_ROUTE, true)
        setRoute(AUTH_ROUTE)
        setError('You must log in before accessing this page.')
      }
    })

    return unsubscribe
  }, [])

  const resetMessages = () => {
    setFeedback('')
    setError('')
  }

  const openRoute = (nextRoute: AppRoute, replace = false) => {
    navigateTo(nextRoute, replace)
    setRoute(nextRoute)
  }

  const handleModeChange = (nextMode: AuthMode) => {
    setMode(nextMode)
    setPassword('')
    setConfirmPassword('')
    resetMessages()
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()

    const normalizedEmail = email.trim()

    if (!normalizedEmail) {
      setError('Enter the email address linked to your account.')
      return
    }

    if (password.length < MIN_PASSWORD_LENGTH) {
      setError(`Use a password with at least ${MIN_PASSWORD_LENGTH} characters and a capital letter.`)
      return
    }

    if (mode === 'register' && password !== confirmPassword) {
      setError('The passwords do not match. Enter the same password in both fields.')
      return
    }

    setIsLoading(true)

    try {
      if (mode === 'register') {
        await createUserWithEmailAndPassword(auth, normalizedEmail, password)
        setFeedback('Account created. Opening your training board.')
      } else {
        await signInWithEmailAndPassword(auth, normalizedEmail, password)
        setFeedback('Signed in. Opening your training board.')
      }

      setPassword('')
      setConfirmPassword('')
      openRoute(BOARD_ROUTE)
    } catch (firebaseError) {
      setError(getFirebaseAuthErrorMessage(firebaseError, 'We could not complete sign-in. Check your details and try again.'))
    } finally {
      setIsLoading(false)
    }
  }

  const handleGoogleAccess = async () => {
    resetMessages()
    setIsLoading(true)

    try {
      await signInWithPopup(auth, googleProvider)
      setFeedback('Google sign-in completed. Opening your training board.')
      openRoute(BOARD_ROUTE)
    } catch (firebaseError) {
      setError(getFirebaseAuthErrorMessage(firebaseError, 'Google sign-in could not be completed. Try again in a moment.'))
    } finally {
      setIsLoading(false)
    }
  }

  const handleSignOut = async () => {
    resetMessages()
    setIsLoading(true)

    try {
      await signOut(auth)
      setFeedback('Session closed successfully.')
      openRoute(AUTH_ROUTE, true)
    } catch (firebaseError) {
      setError(getFirebaseAuthErrorMessage(firebaseError, 'We could not sign you out. Try again in a moment.'))
    } finally {
      setIsLoading(false)
    }
  }

  const renderAuthPage = () => (
    <main className="auth-shell">
      <section className="auth-hero">
        <div>
          <p className="eyebrow">AI chess training</p>
          <div className="hero-title-row">
            <h1>ChessAIssistant</h1>
            <img className="hero-title-logo" src={appLogo} alt="ChessAIssistant logo" />
          </div>
          <p className="hero-copy">
            Train with rated puzzles, review your progress, and get targeted AI hints when the position demands it.
          </p>
        </div>

        <div className="hero-pattern" aria-hidden="true">
          {Array.from({ length: 16 }, (_, index) => {
            const row = Math.floor(index / 4)
            const col = index % 4
            const isLight = (row + col) % 2 === 0
            return (
              <span
                key={index}
                className={`pattern-square ${isLight ? 'light' : 'dark'}`}
              />
            )
          })}
        </div>

        <div className="auth-highlights" aria-label="Training features">
          <div>
            <span className="note-label">Adaptive</span>
            <strong>Elo-based puzzle selection</strong>
          </div>
          <div>
            <span className="note-label">Guided</span>
            <strong>Context-aware hints</strong>
          </div>
          <div>
            <span className="note-label">Tracked</span>
            <strong>Profile and progress history</strong>
          </div>
        </div>
      </section>

      <section className="auth-card">
        <div className="auth-card-header">
          <p className="panel-title">Player access</p>
          <h2>{mode === 'login' ? 'Sign in to continue' : 'Create your account'}</h2>
          <p>
            {mode === 'login'
              ? 'Use your email and password, or continue with Google.'
              : 'Create a secure account to save puzzle attempts and Elo progress.'}
          </p>
        </div>

        <div className="mode-switch" role="tablist" aria-label="Authentication mode">
          <button
            type="button"
            className={mode === 'login' ? 'selected' : ''}
            onClick={() => handleModeChange('login')}
            role="tab"
            aria-selected={mode === 'login'}
          >
            Sign in
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'selected' : ''}
            onClick={() => handleModeChange('register')}
            role="tab"
            aria-selected={mode === 'register'}
          >
            Register
          </button>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label>
            <span>Email</span>
            <input
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              aria-invalid={Boolean(error)}
              required
            />
          </label>

          <label>
            <span>Password</span>
            <input
              type="password"
              autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
              placeholder={mode === 'register' ? 'Create a password' : 'Enter your password'}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={MIN_PASSWORD_LENGTH}
              aria-invalid={Boolean(error)}
              required
            />
            <small>Minimum {MIN_PASSWORD_LENGTH} characters.</small>
          </label>

          {mode === 'register' ? (
            <label>
              <span>Confirm Password</span>
              <input
                type="password"
                autoComplete="new-password"
                placeholder="Repeat your password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                minLength={MIN_PASSWORD_LENGTH}
                aria-invalid={Boolean(error)}
                required
              />
            </label>
          ) : null}

          <button type="submit" className="primary-action" disabled={isLoading}>
            {isLoading ? 'Working...' : mode === 'login' ? 'Log In' : 'Create Account'}
          </button>
        </form>

        <div className="divider">
          <span>or</span>
        </div>

        <button type="button" className="google-action" onClick={() => void handleGoogleAccess()} disabled={isLoading}>
          <span className="google-mark" aria-hidden="true">
            <svg viewBox="0 0 24 24" focusable="false">
              <path
                d="M21.805 12.23c0-.73-.065-1.431-.186-2.104H12v3.983h5.498a4.703 4.703 0 0 1-2.04 3.085v2.561h3.3c1.931-1.778 3.047-4.4 3.047-7.525Z"
                fill="#111111"
              />
              <path
                d="M12 22c2.76 0 5.074-.914 6.765-2.479l-3.3-2.561c-.916.614-2.087.977-3.465.977-2.663 0-4.92-1.799-5.726-4.218H2.863v2.641A10 10 0 0 0 12 22Z"
                fill="#444444"
              />
              <path
                d="M6.274 13.719A5.995 5.995 0 0 1 5.954 12c0-.597.103-1.177.32-1.719V7.64H2.863A10 10 0 0 0 2 12c0 1.61.385 3.132.863 4.36l3.411-2.641Z"
                fill="#777777"
              />
              <path
                d="M12 6.063c1.5 0 2.847.516 3.91 1.531l2.932-2.932C17.07 2.99 14.757 2 12 2A10 10 0 0 0 2.863 7.64l3.411 2.641c.806-2.419 3.063-4.218 5.726-4.218Z"
                fill="#b0b0b0"
              />
            </svg>
          </span>
          <span>Continue with Google</span>
        </button>

        <div className="auth-message-region" aria-live="polite">
          {feedback ? <p className="feedback success">{feedback}</p> : null}
          {error ? <p className="feedback error">{error}</p> : null}
        </div>

        <p className="form-caption">
          {mode === 'login'
            ? 'Your session is protected by Firebase Authentication.'
            : 'After registration, you will be signed in automatically.'}
        </p>

        <AboutLink onOpenAbout={() => openRoute(ABOUT_ROUTE)} />
      </section>
    </main>
  )

  if (!isAuthReady) {
    return (
      <main className="auth-shell">
        <section className="auth-hero">
          <p className="eyebrow">Chess Assistant</p>
          <h1>Checking session.</h1>
          <p className="hero-copy">Waiting for Firebase Authentication to restore the current player.</p>
        </section>
      </main>
    )
  }

  if (route === BOARD_ROUTE) {
    if (!currentUser) {
      return renderAuthPage()
    }

    return (
      <BoardPage
        isLoading={isLoading}
        onOpenProfile={() => openRoute(PROFILE_ROUTE)}
        onOpenAbout={() => openRoute(ABOUT_ROUTE)}
        onSignOut={handleSignOut}
      />
    )
  }

  if (route === PROFILE_ROUTE) {
    if (!currentUser) {
      return renderAuthPage()
    }

    return (
      <ProfilePage
        fallbackEmail={currentUser.email}
        onBackToBoard={() => openRoute(BOARD_ROUTE)}
        onOpenAbout={() => openRoute(ABOUT_ROUTE)}
        onSignOut={handleSignOut}
      />
    )
  }

  if (route === ABOUT_ROUTE) {
    return (
      <AboutPage
        isAuthenticated={Boolean(currentUser)}
        onOpenBoard={() => openRoute(BOARD_ROUTE)}
        onOpenProfile={() => openRoute(PROFILE_ROUTE)}
        onOpenAuth={() => openRoute(AUTH_ROUTE)}
        onSignOut={handleSignOut}
      />
    )
  }

  return renderAuthPage()
}

export default App
