import { Navigate, useLocation } from 'react-router-dom';
import { useEmployeeAuth } from '../../context/EmployeeAuthContext';

/**
 * Wraps a route so only authenticated employees can access it.
 * Unauthenticated visitors are redirected to /admin/login,
 * with the attempted path saved so they land back after login.
 */
export default function ProtectedRoute({ children }) {
  const { employee } = useEmployeeAuth();
  const location     = useLocation();

  if (!employee) {
    return (
      <Navigate
        to="/admin/login"
        state={{ from: location }}
        replace
      />
    );
  }

  return children;
}
