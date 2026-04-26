import type { FormEvent } from 'react'
import { useEffect, useState } from 'react'
import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from 'firebase/auth'
import type { User } from 'firebase/auth'
import './App.css'
import { auth } from './firebase'
import { BoardPage } from './pages/BoardPage'

type AuthMode = 'login' | 'register'
type AppRoute = '/' | '/board'

const AUTH_ROUTE: AppRoute = '/'
const BOARD_ROUTE: AppRoute = '/board'

const getCurrentRoute = (): AppRoute => {
  return window.location.pathname === BOARD_ROUTE ? BOARD_ROUTE : AUTH_ROUTE
}

const navigateTo = (route: AppRoute, replace = false) => {
  if (window.location.pathname === route) {
    return
  }

  const method = replace ? window.history.replaceState : window.history.pushState
  method.call(window.history, null, '', route)
}

function App() {
  const googleProvider = new GoogleAuthProvider()
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
        navigateTo(BOARD_ROUTE, true)
        setRoute(BOARD_ROUTE)
        return
      }

      if (getCurrentRoute() === BOARD_ROUTE) {
        navigateTo(AUTH_ROUTE, true)
        setRoute(AUTH_ROUTE)
        setError('You must log in before accessing the board.')
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

    if (mode === 'register' && password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }

    setIsLoading(true)

    try {
      if (mode === 'register') {
        const credential = await createUserWithEmailAndPassword(auth, email, password)
        setFeedback(`Account created for ${credential.user.email ?? email}. Redirecting to the board...`)
      } else {
        const credential = await signInWithEmailAndPassword(auth, email, password)
        setFeedback(`Welcome back, ${credential.user.email ?? email}. Redirecting to the board...`)
      }

      setPassword('')
      setConfirmPassword('')
      openRoute(BOARD_ROUTE)
    } catch (firebaseError) {
      if (firebaseError instanceof Error) {
        setError(firebaseError.message)
      } else {
        setError('Authentication failed.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const handleGoogleAccess = async () => {
    resetMessages()
    setIsLoading(true)

    try {
      const credential = await signInWithPopup(auth, googleProvider)
      setFeedback(`Welcome, ${credential.user.email ?? 'player'}. Redirecting to the board...`)
      openRoute(BOARD_ROUTE)
    } catch (firebaseError) {
      if (firebaseError instanceof Error) {
        setError(firebaseError.message)
      } else {
        setError('Google sign-in failed.')
      }
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
      if (firebaseError instanceof Error) {
        setError(firebaseError.message)
      } else {
        setError('Sign out failed.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  const renderAuthPage = () => (
    <main className="auth-shell">
      <section className="auth-hero">
        <p className="eyebrow">Enter the board</p>
        <h1>Chess AIssistant</h1>
        <p className="hero-copy">
          The first ever AI tutor that helps you understand the board.
        </p>

        <div className="hero-pattern" aria-hidden="true">
          {Array.from({ length: 16 }, (_, index) => {
            const row = Math.floor(index / 4);
            const col = index % 4;
            const isLight = (row + col) % 2 === 0;
            return (
                <span
                    key={index}
                    className={`pattern-square ${isLight ? 'light' : 'dark'}`}
                />
            );
          })}
        </div>

        <div className="hero-note">
          <span className="note-label">Session</span>
          <strong>{isAuthReady ? currentUser?.email ?? 'No active player' : 'Checking session...'}</strong>
        </div>
      </section>

      <section className="auth-card">
        <div className="mode-switch" role="tablist" aria-label="Authentication mode">
          <button
            type="button"
            className={mode === 'login' ? 'selected' : ''}
            onClick={() => handleModeChange('login')}
          >
            Log In
          </button>
          <button
            type="button"
            className={mode === 'register' ? 'selected' : ''}
            onClick={() => handleModeChange('register')}
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
              placeholder="player@club.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
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
              minLength={6}
              required
            />
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
                minLength={6}
                required
              />
            </label>
          ) : null}

          <button type="submit" className="primary-action" disabled={isLoading}>
            {isLoading ? 'Working...' : mode === 'login' ? 'Log In' : 'Create Account'}
          </button>
        </form>

        <div className="divider" aria-hidden="true">
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
          <span>Continue With Google</span>
        </button>

        {feedback ? <p className="feedback success">{feedback}</p> : null}
        {error ? <p className="feedback error">{error}</p> : null}

        <p className="form-caption">
          {mode === 'login'
            ? 'Use your existing account to continue.'
            : 'Successful registration automatically signs you in and opens the board.'}
        </p>
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
        userEmail={currentUser.email}
        onSignOut={handleSignOut}
      />
    )
  }

  return renderAuthPage()
}

export default App
