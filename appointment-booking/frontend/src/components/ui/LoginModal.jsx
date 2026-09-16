import { useState, useEffect, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import Spinner from './Spinner';

/**
 * LoginModal — slides up as a bottom-sheet on mobile, centred dialog on desktop.
 *
 * Props:
 *   isOpen   {boolean}  — controls visibility
 *   onClose  {fn}       — called when the user dismisses without logging in
 *   onSuccess {fn}      — called with the user profile after a successful login
 */
export default function LoginModal({ isOpen, onClose, onSuccess }) {
  const { login, loading, error, clearError } = useAuth();

  const [idNumber, setIdNumber] = useState('');
  const [pin,      setPin]      = useState('');
  const [showPin,  setShowPin]  = useState(false);

  const idRef = useRef(null);

  // Focus the ID field whenever the modal opens
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => idRef.current?.focus(), 80);
    } else {
      setIdNumber('');
      setPin('');
      setShowPin(false);
      clearError();
    }
  }, [isOpen, clearError]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const result = await login(idNumber, pin);
    if (result.success) {
      onSuccess(result.user);
    }
  };

  if (!isOpen) return null;

  return (
    /* ── Backdrop ── */
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="login-modal-title"
    >
      {/* ── Panel ── */}
      <div className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-2xl shadow-2xl p-6 sm:p-8 animate-slide-up">

        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <span className="text-2xl">🏦</span>
              <h2 id="login-modal-title" className="text-lg font-bold text-gray-900">
                Sign in to your Capitec account
              </h2>
            </div>
            <p className="text-sm text-gray-500">
              We'll fill in your details automatically.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close sign-in"
            className="p-1.5 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Error banner */}
        {error && (
          <div role="alert" className="flex items-start gap-2 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg px-4 py-3 mb-5">
            <svg className="w-4 h-4 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm-.75-5.75a.75.75 0 011.5 0v.5a.75.75 0 01-1.5 0v-.5zm0-5.5a.75.75 0 011.5 0v3a.75.75 0 01-1.5 0v-3z" clipRule="evenodd" />
            </svg>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {/* SA ID Number */}
          <div>
            <label htmlFor="login-id" className="form-label">
              SA ID Number <span className="text-red-500">*</span>
            </label>
            <input
              ref={idRef}
              id="login-id"
              type="text"
              inputMode="numeric"
              maxLength={13}
              value={idNumber}
              onChange={(e) => { setIdNumber(e.target.value.replace(/\D/g, '')); clearError(); }}
              placeholder="13-digit ID number"
              className="form-input"
              autoComplete="username"
              required
            />
          </div>

          {/* PIN */}
          <div>
            <label htmlFor="login-pin" className="form-label">
              Remote PIN <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <input
                id="login-pin"
                type={showPin ? 'text' : 'password'}
                inputMode="numeric"
                maxLength={5}
                value={pin}
                onChange={(e) => { setPin(e.target.value.replace(/\D/g, '')); clearError(); }}
                placeholder="5-digit PIN"
                className="form-input pr-11"
                autoComplete="current-password"
                required
              />
              <button
                type="button"
                onClick={() => setShowPin((v) => !v)}
                aria-label={showPin ? 'Hide PIN' : 'Show PIN'}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                {showPin
                  ? /* eye-off */
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M13.875 18.825A10.05 10.05 0 0112 19c-5 0-9-4-9-7 0-.9.3-1.8.8-2.6M6.1 6.1A9.97 9.97 0 0112 5c5 0 9 4 9 7 0 1.2-.4 2.3-1.1 3.3M15 12a3 3 0 11-6 0 3 3 0 016 0zM3 3l18 18" />
                    </svg>
                  : /* eye */
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M15 12a3 3 0 11-6 0 3 3 0 016 0zM2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                    </svg>
                }
              </button>
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={loading || idNumber.length !== 13 || pin.length < 4}
            className="btn-primary w-full btn-lg mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading
              ? <><Spinner size="sm" className="text-white" /> Signing in…</>
              : 'Sign In & Continue'
            }
          </button>
        </form>


      </div>
    </div>
  );
}
