import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { employeeLogin } from '../api/auth';
import { setAuthToken, clearAuthToken } from '../api/client';

/**
 * EmployeeAuthContext — JWT-backed session for branch employees.
 *
 * Storage strategy:
 *  - The JWT is stored in memory via setAuthToken() so it's attached to
 *    every Axios request automatically.
 *  - The safe profile (no token) is persisted in sessionStorage so a page
 *    refresh within the same tab restores the employee name/role in the UI.
 *  - On tab/browser close sessionStorage is cleared automatically.
 *  - On mount we also restore the token from sessionStorage so refreshes
 *    don't force a re-login within the same tab.
 */

const EmployeeAuthContext = createContext(null);

const SESSION_KEY = 'emp_session';  // stores { profile, token, expiresAt }

function loadSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const session = JSON.parse(raw);
    // Discard if token is already expired
    if (session.expiresAt && Date.now() > session.expiresAt) {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
    return session;
  } catch {
    return null;
  }
}

export function EmployeeAuthProvider({ children }) {
  const [employee, setEmployee] = useState(null);
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState('');

  // Restore session on mount (e.g. page refresh)
  useEffect(() => {
    const session = loadSession();
    if (session) {
      setEmployee(session.profile);
      setAuthToken(session.token);   // re-arm the axios interceptor
    }
  }, []);

  const login = useCallback(async (username, password) => {
    setLoading(true);
    setError('');

    try {
      // response shape: ApiResponse { success, data: AuthTokenResponse { token, tokenType, expiresIn, profile } }
      const response = await employeeLogin(username.trim(), password);

      const { token, expiresIn, profile } = response.data;

      const expiresAt = Date.now() + expiresIn * 1_000;

      const emp = {
        id:       profile.id,
        username: profile.username,
        fullName: profile.fullName,
        role:     profile.role,
      };

      // Arm axios interceptor with the real JWT
      setAuthToken(token);

      // Persist safe session (profile + token for tab-refresh restore)
      sessionStorage.setItem(SESSION_KEY, JSON.stringify({ profile: emp, token, expiresAt }));

      setEmployee(emp);
      return { success: true, employee: emp };

    } catch (err) {
      const msg =
        err.status === 404 || err.status === 401
          ? 'Invalid username or password. Please try again.'
          : err.message || 'Login failed. Please try again.';
      setError(msg);
      return { success: false, message: msg };

    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    clearAuthToken();
    sessionStorage.removeItem(SESSION_KEY);
    setEmployee(null);
    setError('');
  }, []);

  const clearError = useCallback(() => setError(''), []);

  return (
    <EmployeeAuthContext.Provider value={{ employee, loading, error, login, logout, clearError }}>
      {children}
    </EmployeeAuthContext.Provider>
  );
}

export function useEmployeeAuth() {
  const ctx = useContext(EmployeeAuthContext);
  if (!ctx) throw new Error('useEmployeeAuth must be used inside <EmployeeAuthProvider>');
  return ctx;
}
