import { createContext, useContext, useState, useCallback } from 'react';
import { employeeLogin } from '../api/auth';

/**
 * EmployeeAuthContext — session state for branch employees.
 *
 * Persists the employee profile in sessionStorage so a page refresh
 * within the same browser tab keeps them logged in, but closing the
 * tab/browser clears the session automatically.
 */

const EmployeeAuthContext = createContext(null);

const SESSION_KEY = 'emp_session';

function loadSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function EmployeeAuthProvider({ children }) {
  const [employee, setEmployee] = useState(loadSession);
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState('');

  const login = useCallback(async (username, password) => {
    setLoading(true);
    setError('');

    try {
      const response = await employeeLogin(username.trim(), password);

      const profile = {
        id:       response.data.id,
        username: response.data.username,
        fullName: response.data.fullName,
        role:     response.data.role,
      };

      sessionStorage.setItem(SESSION_KEY, JSON.stringify(profile));
      setEmployee(profile);
      return { success: true, employee: profile };

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
    sessionStorage.removeItem(SESSION_KEY);
    setEmployee(null);
    setError('');
  }, []);

  const clearError = useCallback(() => setError(''), []);

  return (
    <EmployeeAuthContext.Provider
      value={{ employee, loading, error, login, logout, clearError }}
    >
      {children}
    </EmployeeAuthContext.Provider>
  );
}

export function useEmployeeAuth() {
  const ctx = useContext(EmployeeAuthContext);
  if (!ctx) throw new Error('useEmployeeAuth must be used inside <EmployeeAuthProvider>');
  return ctx;
}
