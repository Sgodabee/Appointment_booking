import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] px-4 text-center animate-fade-in">
      <div className="text-8xl mb-6">🗓️</div>
      <h1 className="text-4xl font-black text-gray-900 mb-3">404</h1>
      <h2 className="text-xl font-semibold text-gray-700 mb-3">Page Not Found</h2>
      <p className="text-gray-500 mb-8 max-w-sm">
        The page you're looking for doesn't exist or has been moved.
      </p>
      <div className="flex flex-col sm:flex-row gap-3">
        <Link to="/" className="btn-primary btn-lg">Go Home</Link>
        <Link to="/book" className="btn-secondary btn-lg">Book an Appointment</Link>
      </div>
    </div>
  );
}
