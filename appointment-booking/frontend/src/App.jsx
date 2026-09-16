import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import ProtectedRoute from './components/ui/ProtectedRoute';
import HomePage from './pages/HomePage';
import BookPage from './pages/BookPage';
import ConfirmationPage from './pages/ConfirmationPage';
import LookupPage from './pages/LookupPage';
import CancelPage from './pages/CancelPage';
import AdminPage from './pages/AdminPage';
import EmployeeLoginPage from './pages/EmployeeLoginPage';
import NotFoundPage from './pages/NotFoundPage';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="book"         element={<BookPage />} />
        <Route path="confirmation" element={<ConfirmationPage />} />
        <Route path="lookup"       element={<LookupPage />} />
        <Route path="cancel"       element={<CancelPage />} />
        <Route path="admin/login"  element={<EmployeeLoginPage />} />
        <Route
          path="admin"
          element={
            <ProtectedRoute>
              <AdminPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
