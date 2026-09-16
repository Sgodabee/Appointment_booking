import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useEmployeeAuth } from '../context/EmployeeAuthContext';
import Spinner from '../components/ui/Spinner';

export default function EmployeeLoginPage() {
  const { login, loading, error, clearError } = useEmployeeAuth();
  const navigate  = useNavigate();
  const location  = useLocation();
  const from      = location.state?.from?.pathname || '/admin';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPwd,  setShowPwd]  = useState(false);

  const usernameRef = useRef(null);
  useEffect(() => { usernameRef.current?.focus(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const result = await login(username, password);
    if (result.success) {
      navigate(from, { replace: true });
    }
  };

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center
                    px-4 py-12 bg-gradient-to-br from-brand-950 via-brand-900 to-brand-800">

      {/* Decorative blobs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-32 -right-32 w-96 h-96 rounded-full bg-white/5 blur-3xl" />
        <div className="absolute bottom-0 -left-20 w-72 h-72 rounded-full bg-white/5 blur-2xl" />
      </div>

      <div className="relative w-full max-w-md animate-slide-up">

        {/* Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl
                          bg-white shadow-btn mx-auto mb-4">
            <span className="text-brand-600 font-black text-2xl">C</span>
          </div>
          <h1 className="text-2xl font-bold text-white">Employee Portal</h1>
          <p className="text-white/60 text-sm mt-1">
            Sign in to access the Admin Dashboard
          </p>
        </div>

        {/* Card */}
        <div className="card-glass p-8">

          {/* Error */}
          {error && (
            <div role="alert"
              className="flex items-start gap-2.5 bg-red-500/10 border border-red-400/30
                         text-red-300 text-sm rounded-xl px-4 py-3 mb-6">
              <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd"
                  d="M10 18a8 8 0 100-16 8 8 0 000 16zm-.75-5.75a.75.75 0 011.5 0v.5a.75.75 0 01-1.5 0v-.5zm0-5.5a.75.75 0 011.5 0v3a.75.75 0 01-1.5 0v-3z"
                  clipRule="evenodd" />
              </svg>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} noValidate className="space-y-5">

            {/* Username */}
            <div>
              <label htmlFor="emp-username" className="form-label text-gray-700">
                Username <span className="text-red-500">*</span>
              </label>
              <input
                ref={usernameRef}
                id="emp-username"
                type="text"
                autoComplete="username"
                value={username}
                onChange={(e) => { setUsername(e.target.value); clearError(); }}
                placeholder="e.g. admin"
                className="form-input"
                required
              />
            </div>

            {/* Password */}
            <div>
              <label htmlFor="emp-password" className="form-label text-gray-700">
                Password <span className="text-red-500">*</span>
              </label>
              <div className="relative">
                <input
                  id="emp-password"
                  type={showPwd ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => { setPassword(e.target.value); clearError(); }}
                  placeholder="Your password"
                  className="form-input pr-11"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPwd((v) => !v)}
                  aria-label={showPwd ? 'Hide password' : 'Show password'}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  {showPwd
                    ? <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M13.875 18.825A10.05 10.05 0 0112 19c-5 0-9-4-9-7 0-.9.3-1.8.8-2.6M6.1 6.1A9.97 9.97 0 0112 5c5 0 9 4 9 7 0 1.2-.4 2.3-1.1 3.3M15 12a3 3 0 11-6 0 3 3 0 016 0zM3 3l18 18" />
                      </svg>
                    : <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                          d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                      </svg>
                  }
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || !username || !password}
              className="btn-primary w-full btn-lg mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading
                ? <><Spinner size="sm" className="text-white" /> Signing in…</>
                : 'Sign In to Dashboard'
              }
            </button>
          </form>

          {/* Role hint */}
          <p className="text-xs text-gray-400 text-center mt-5">
            Only authorised Capitec employees can access the admin dashboard.
          </p>
        </div>
      </div>
    </div>
  );
}
