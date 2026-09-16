import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { getAppointmentByReference } from '../api/appointments';
import { formatDate, formatTime, formatServiceType } from '../utils/formatters';
import Badge from '../components/ui/Badge';
import Spinner from '../components/ui/Spinner';
import Alert from '../components/ui/Alert';

export default function LookupPage() {
  const [appointment, setAppointment] = useState(null);
  const [loading, setLoading]         = useState(false);
  const [error, setError]             = useState(null);

  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async ({ reference }) => {
    setLoading(true);
    setError(null);
    setAppointment(null);
    try {
      const res = await getAppointmentByReference(reference.trim().toUpperCase());
      setAppointment(res.data);
    } catch (err) {
      setError(
        err.code === 'NOT_FOUND'
          ? 'No appointment found for that reference number. Please check and try again.'
          : err.message || 'Something went wrong. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto px-4 sm:px-6 py-10 animate-fade-in">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">Look Up My Appointment</h1>
      <p className="text-gray-500 text-sm mb-8">
        Enter your reference number to view your appointment details.
      </p>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="card p-6 sm:p-8 mb-6">
        <div className="mb-5">
          <label className="form-label" htmlFor="reference">
            Reference Number <span className="text-red-500">*</span>
          </label>
          <input
            id="reference"
            {...register('reference', {
              required: 'Reference number is required',
              pattern: {
                value: /^APB-\d{8}-[A-F0-9]{4}$/i,
                message: 'Format: APB-YYYYMMDD-XXXX (e.g. APB-20261001-AB12)',
              },
            })}
            type="text"
            placeholder="APB-YYYYMMDD-XXXX"
            className={`form-input uppercase tracking-widest font-mono ${errors.reference ? 'form-input-error' : ''}`}
          />
          {errors.reference && <p className="form-error">{errors.reference.message}</p>}
        </div>

        <button type="submit" className="btn-primary w-full" disabled={loading}>
          {loading ? <><Spinner size="sm" className="text-white" /> Searching…</> : 'Find Appointment'}
        </button>
      </form>

      {error && <Alert variant="error" className="mb-6">{error}</Alert>}

      {appointment && (
        <div className="card p-6 sm:p-8 animate-slide-up">
          <div className="flex items-center justify-between mb-5">
            <h2 className="text-lg font-semibold text-gray-900">Appointment Details</h2>
            <Badge status={appointment.status} />
          </div>

          <dl className="space-y-0">
            {[
              ['Reference',  <span key="ref" className="font-mono font-bold text-brand-600">{appointment.referenceNumber}</span>],
              ['Name',       appointment.customerName],
              ['Email',      appointment.customerEmail],
              ['Phone',      appointment.customerPhone],
              ['Service',    formatServiceType(appointment.serviceType)],
              ['Date',       formatDate(appointment.appointmentDate)],
              ['Time',       formatTime(appointment.appointmentTime)],
              appointment.notes && ['Notes', appointment.notes],
            ].filter(Boolean).map(([label, val]) => (
              <div key={label} className="flex justify-between gap-4 py-3 border-b border-gray-100 last:border-0 text-sm">
                <dt className="text-gray-500 font-medium">{label}</dt>
                <dd className="text-gray-900 font-semibold text-right">{val}</dd>
              </div>
            ))}
          </dl>
        </div>
      )}
    </div>
  );
}
