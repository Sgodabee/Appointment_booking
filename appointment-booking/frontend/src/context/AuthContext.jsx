import { createContext, useContext, useState, useCallback } from 'react';
import { authenticateCustomer } from '../api/customers';
import { setAuthToken, clearAuthToken } from '../api/client';

/**
 * AuthContext — JWT-backed session for authenticated Capitec customers.
 *
 * Storage strategy:
 *  - JWT stored in memory only via setAuthToken() — not in localStorage.
 *  - Profile (no token) kept in React state only — customer sessions are
 *    intentionally short-lived (1 hour) and scoped to the booking flow.
 *  - On logout or page refresh the customer must sign in again (by design —
 *    the booking form is a single-session action).
 *
 * Note: if an employee is also signed in, their token takes priority in the
 * axios interceptor. Customer auth is only used while completing a booking.
 */

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user,    setUser]    = useState(null);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState('');

  const login = useCallback(async (idNumber, pin) => {
    setLoading(true);
    setError('');

    try {
      // response shape: ApiResponse { success, data: AuthTokenResponse { token, tokenType, expiresIn, profile } }
      const response = await authenticateCustomer(idNumber.trim(), pin.trim());

      const { token, profile } = response.data;

      const customerProfile = {
        customerName:  profile.customerName,
        customerEmail: profile.customerEmail,
        customerPhone: profile.customerPhone,
        idNumber:      profile.idNumber,
      };

      // Arm axios interceptor — replaces any existing token
      setAuthToken(token);

      setUser(customerProfile);
      return { success: true, user: customerProfile };

    } catch (err) {
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
    clearAuthToken();
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
