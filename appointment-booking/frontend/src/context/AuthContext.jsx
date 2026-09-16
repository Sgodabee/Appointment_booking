import { createContext, useContext, useState, useCallback } from 'react';
import { authenticateCustomer } from '../api/customers';

/**
 * AuthContext — holds the currently authenticated Capitec customer.
 *
 * login() calls POST /api/v1/customers/authenticate with the plain ID
 * and PIN. The backend verifies against the DB and returns a safe
 * profile DTO for form pre-fill. No credentials are stored in state.
 */

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null);   // null = not logged in
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  /**
   * Authenticate a customer against the backend.
   * Returns { success: true, user } or { success: false, message }.
   */
  const login = useCallback(async (idNumber, pin) => {
    setLoading(true);
    setError('');

    try {
      const response = await authenticateCustomer(idNumber.trim(), pin.trim());

      // axios interceptor unwraps response.data, so response = ApiResponse body
      // ApiResponse shape: { success, message, data: CustomerProfileResponse }
      const profile = {
        customerName:  response.data.customerName,
        customerEmail: response.data.customerEmail,
        customerPhone: response.data.customerPhone,
        idNumber:      response.data.idNumber,
      };

      setUser(profile);
      return { success: true, user: profile };

    } catch (err) {
      // err is shaped by the axios response interceptor in client.js:
      // { status, code, message, errors }
      const msg =
        err.status === 404 || err.status === 401
          ? 'ID number or PIN is incorrect. Please try again.'
          : err.message || 'Sign-in failed. Please try again.';

      setError(msg);
      return { success: false, message: msg };

    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    setError('');
  }, []);

  const clearError = useCallback(() => setError(''), []);

  return (
    <AuthContext.Provider value={{ user, loading, error, login, logout, clearError }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
