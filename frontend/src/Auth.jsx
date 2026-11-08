import { useEffect, useState } from 'react'

const API = '/api'

const css = `
.auth-overlay{position:fixed;inset:0;z-index:1000;display:flex;align-items:center;justify-content:center;padding:16px;background:rgba(10,30,22,.55);backdrop-filter:blur(6px)}
.auth-card{display:grid;grid-template-columns:5fr 6fr;width:min(860px,100%);min-height:540px;background:#fff;color:#14231c;border-radius:22px;overflow:hidden;box-shadow:0 30px 80px rgba(0,0,0,.35)}
.auth-side{background:linear-gradient(160deg,#0f3d2e 0%,#0a2a20 100%);color:#fff;padding:40px 34px;display:flex;flex-direction:column;justify-content:space-between}
.auth-logo{display:flex;align-items:center;gap:10px;font-size:22px;font-weight:700}
.auth-logo b{display:grid;place-items:center;width:34px;height:34px;border-radius:9px;background:#fff;color:#0f3d2e}
.auth-side h3{font-size:30px;line-height:1.15;margin:0 0 12px;font-weight:700}
.auth-side p{margin:0;color:rgba(255,255,255,.75);font-size:15px;line-height:1.5}
.auth-points{list-style:none;margin:0;padding:0;display:grid;gap:10px;font-size:14px;color:rgba(255,255,255,.88)}
.auth-points li::before{content:"\\2713";margin-right:10px;color:#7ee0b0;font-weight:700}
.auth-main{position:relative;padding:40px 40px 32px;display:flex;flex-direction:column;justify-content:center}
.auth-close{position:absolute;top:14px;right:14px;width:34px;height:34px;border:none;border-radius:50%;background:#f1f4f2;color:#33423a;font-size:18px;cursor:pointer}
.auth-tabs{display:grid;grid-template-columns:1fr 1fr;background:#f1f4f2;border-radius:12px;padding:4px;margin-bottom:24px}
.auth-tabs button{border:none;background:transparent;padding:10px;border-radius:9px;font-size:15px;font-weight:600;color:#5b6b62;cursor:pointer}
.auth-tabs button.on{background:#fff;color:#0f3d2e;box-shadow:0 1px 4px rgba(0,0,0,.12)}
.auth-title{margin:0 0 4px;font-size:26px;font-weight:700}
.auth-sub{margin:0 0 22px;color:#5b6b62;font-size:14.5px}
.auth-form{display:grid;gap:16px}
.auth-field{display:grid;gap:6px}
.auth-field label{font-size:13px;font-weight:600;color:#33423a}
.auth-input{position:relative}
.auth-input input{width:100%;box-sizing:border-box;padding:13px 14px;font-size:15.5px;border:1.5px solid #d5ddd8;border-radius:11px;background:#fff;color:#14231c;outline:none}
.auth-input input:focus{border-color:#0f3d2e;box-shadow:0 0 0 4px rgba(15,61,46,.12)}
.auth-eye{position:absolute;right:8px;top:50%;transform:translateY(-50%);border:none;background:none;cursor:pointer;color:#5b6b62;padding:6px;font-size:13px;font-weight:600}
.auth-error{background:#fdecea;border:1px solid #f5c2bd;color:#a12a1f;padding:11px 13px;border-radius:11px;font-size:14px}
.auth-submit{display:flex;align-items:center;justify-content:center;gap:10px;width:100%;padding:14px;margin-top:4px;border:none;border-radius:12px;background:#0f3d2e;color:#fff;font-size:16px;font-weight:700;cursor:pointer}
.auth-submit:disabled{opacity:.7;cursor:wait}
.auth-spin{width:16px;height:16px;border:2.5px solid rgba(255,255,255,.4);border-top-color:#fff;border-radius:50%;animation:authSpin .7s linear infinite}
.auth-foot{margin:18px 0 0;text-align:center;font-size:14px;color:#5b6b62}
.auth-foot button{border:none;background:none;color:#0f3d2e;font-weight:700;cursor:pointer;font-size:14px;padding:0}
@keyframes authSpin{to{transform:rotate(360deg)}}
@media (max-width:720px){.auth-card{grid-template-columns:1fr;min-height:0}.auth-side{display:none}.auth-main{padding:34px 24px 26px}}
`

export default function AuthPanel({ onLogin, onClose }) {
  const [mode, setMode] = useState('login')
  const [form, setForm] = useState({ fullName: '', email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const onKey = event => event.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  const update = field => event => setForm({ ...form, [field]: event.target.value })

  function switchMode(next) {
    setMode(next)
    setError('')
  }

  async function call(path, body) {
    let response
    try {
      response = await fetch(`${API}/users/${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })
    } catch {
      throw new Error('Cannot reach the server. Check that the backend is running.')
    }
    const data = await response.json().catch(() => ({}))
    if (!response.ok) {
      if (data.message) throw new Error(data.message)
      if (response.status === 400)
        throw new Error('Enter a valid email and a password of at least 6 characters.')
      throw new Error(`Server error (HTTP ${response.status}). Please try again.`)
    }
    return data
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (mode === 'register') await call('register', form)
      const auth = await call('login', { email: form.email, password: form.password })
      onLogin(auth)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  const isLogin = mode === 'login'

  return (
    <div className="auth-overlay" onClick={onClose}>
      <style>{css}</style>
      <div className="auth-card" onClick={event => event.stopPropagation()}>
        <aside className="auth-side">
          <div className="auth-logo"><b>R</b> Rush Cart</div>
          <div>
            <h3>{isLogin ? 'Welcome back.' : 'Join Rush Cart.'}</h3>
            <p>Sign in to place orders and keep every purchase tied to your account.</p>
          </div>
          <ul className="auth-points">
            <li>Free delivery over ₹2,000</li>
            <li>Orders processed in real time</li>
            <li>Secure, encrypted passwords</li>
          </ul>
        </aside>

        <section className="auth-main">
          <button className="auth-close" onClick={onClose} aria-label="Close">×</button>

          <div className="auth-tabs">
            <button type="button" className={isLogin ? 'on' : ''} onClick={() => switchMode('login')}>Log in</button>
            <button type="button" className={!isLogin ? 'on' : ''} onClick={() => switchMode('register')}>Sign up</button>
          </div>

          <h2 className="auth-title">{isLogin ? 'Log in to your account' : 'Create your account'}</h2>
          <p className="auth-sub">{isLogin ? 'Enter your email and password to continue.' : 'It only takes a few seconds.'}</p>

          <form className="auth-form" onSubmit={submit}>
            {!isLogin && (
              <div className="auth-field">
                <label htmlFor="auth-name">Full name</label>
                <div className="auth-input">
                  <input id="auth-name" autoFocus placeholder="Your full name" value={form.fullName}
                    onChange={update('fullName')} required autoComplete="name" />
                </div>
              </div>
            )}

            <div className="auth-field">
              <label htmlFor="auth-email">Email address</label>
              <div className="auth-input">
                <input id="auth-email" type="email" autoFocus={isLogin} placeholder="you@example.com"
                  value={form.email} onChange={update('email')} required autoComplete="email" />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="auth-password">Password</label>
              <div className="auth-input">
                <input id="auth-password" type={showPassword ? 'text' : 'password'}
                  placeholder={isLogin ? 'Your password' : 'At least 6 characters'}
                  value={form.password} onChange={update('password')} required minLength={6}
                  autoComplete={isLogin ? 'current-password' : 'new-password'} style={{ paddingRight: 64 }} />
                <button type="button" className="auth-eye" onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </div>

            {error && <div className="auth-error" role="alert">⚠ {error}</div>}

            <button className="auth-submit" type="submit" disabled={busy}>
              {busy && <span className="auth-spin" />}
              {busy ? 'Please wait…' : isLogin ? 'Log in' : 'Create account'}
            </button>
          </form>

          <p className="auth-foot">
            {isLogin ? 'New to Rush Cart? ' : 'Already have an account? '}
            <button type="button" onClick={() => switchMode(isLogin ? 'register' : 'login')}>
              {isLogin ? 'Create an account' : 'Log in'}
            </button>
          </p>
        </section>
      </div>
    </div>
  )
}