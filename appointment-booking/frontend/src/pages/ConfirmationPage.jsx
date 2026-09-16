import { useLocation, Link } from 'react-router-dom';
import { formatDate, formatTime, formatServiceType } from '../utils/formatters';

export default function ConfirmationPage() {
  const { state } = useLocation();

  if (!state?.appointment) {
    return (
      <div className="max-w-lg mx-auto px-4 py-20 text-center animate-fade-in">
        <div className="text-6xl mb-4">🔍</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-3">No Booking Found</h1>
        <p className="text-gray-500 mb-8">It looks like you arrived here directly. Book an appointment first.</p>
        <Link to="/book" className="btn-primary btn-lg">Book an Appointment</Link>
      </div>
    );
  }

  const { appointment, emailPreviewUrl, branchName } = state;

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-12 animate-fade-in">
      {/* Success banner */}
      <div className="flex flex-col items-center text-center mb-10">
        <div className="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center text-4xl mb-5 shadow-sm">
          ✅
        </div>
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">
          Appointment Confirmed!
        </h1>
        <p className="text-gray-500 max-w-sm">
          Your appointment has been successfully booked. A confirmation email has been sent to{' '}
          <strong className="text-gray-700">{appointment.customerEmail}</strong>.
        </p>
      </div>

      {/* Reference card */}
      <div className="card p-6 sm:p-8 mb-6">
        <div className="text-center mb-6 p-4 bg-brand-50 rounded-xl border border-brand-100">
          <p className="text-xs text-brand-600 font-semibold uppercase tracking-widest mb-1">
            Reference Number
          </p>
          <p className="text-2xl sm:text-3xl font-black text-brand-700 tracking-widest font-mono">
            {appointment.referenceNumber}
          </p>
          <p className="text-xs text-gray-500 mt-1">Keep this reference number for your records.</p>
        </div>

        <dl className="space-y-0">
          {[
            ['Customer',      appointment.customerName],
            ['Branch',        branchName ?? `Branch #${appointment.branchId}`],
            ['Service',       formatServiceType(appointment.serviceType)],
            ['Date',          formatDate(appointment.appointmentDate)],
            ['Time',          formatTime(appointment.appointmentTime)],
            ['Status',        <span key="status" className="badge badge-confirmed">Confirmed</span>],
          ].map(([label, val]) => (
            <div key={label} className="flex justify-between gap-4 py-3 border-b border-gray-100 last:border-0 text-sm">
              <dt className="text-gray-500 font-medium">{label}</dt>
              <dd className="text-gray-900 font-semibold text-right">{val}</dd>
            </div>
          ))}
        </dl>
      </div>

      {/* Email preview link (Ethereal simulation) */}
      {emailPreviewUrl && (
        <div className="card p-4 mb-6 bg-blue-50 border-blue-100">
          <p className="text-sm text-blue-800 font-medium mb-1">📧 Simulated Email Preview</p>
          <p className="text-xs text-blue-600 mb-2">
            This is a test environment. View the simulated confirmation email here:
          </p>
          <a
            href={emailPreviewUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-xs text-brand-600 underline break-all"
          >
            {emailPreviewUrl}
          </a>
        </div>
      )}

      {/* Reminder */}
      <div className="card p-5 mb-8 bg-yellow-50 border-yellow-100">
        <p className="text-sm font-semibold text-yellow-800 mb-2">📋 Please remember to bring:</p>
        <ul className="text-sm text-yellow-700 space-y-1 list-disc list-inside">
          <li>A valid South African ID or passport</li>
          <li>Any relevant supporting documents for your service</li>
          <li>Your reference number: <strong>{appointment.referenceNumber}</strong></li>
        </ul>
      </div>

      {/* Actions */}
      <div className="flex flex-col sm:flex-row gap-3">
        <Link to="/" className="btn-secondary btn-lg flex-1 justify-center">
          Back to Home
        </Link>
        <Link to="/cancel" className="btn-danger btn-lg flex-1 justify-center">
          Cancel This Appointment
        </Link>
      </div>
    </div>
  );
}
