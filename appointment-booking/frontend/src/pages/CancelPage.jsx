import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import toast from 'react-hot-toast';
import { cancelAppointment } from '../api/appointments';
import { cancelSchema } from '../utils/validation';
import Spinner from '../components/ui/Spinner';
import Alert from '../components/ui/Alert';

export default function CancelPage() {
  const [submitting, setSubmitting] = useState(false);
  const [cancelled, setCancelled]   = useState(null);

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: yupResolver(cancelSchema),
    mode: 'onTouched',
  });

  const onSubmit = async ({ referenceNumber, customerEmail }) => {
    setSubmitting(true);
    try {
      const res = await cancelAppointment(
        referenceNumber.trim().toUpperCase(),
        customerEmail.trim().toLowerCase()
      );
      setCancelled(res.data);
      toast.success('Appointment cancelled successfully.');
    } catch (err) {
      if (err.code === 'NOT_FOUND') {
        toast.error('No matching appointment found. Check your reference number and email.');
      } else {
        toast.error(err.message || 'Cancellation failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  if (cancelled) {
    return (
      <div className="max-w-xl mx-auto px-4 sm:px-6 py-16 text-center animate-fade-in">
        <div className="w-20 h-20 rounded-full bg-red-100 flex items-center justify-center text-4xl mb-5 mx-auto">
          ❌
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-3">Appointment Cancelled</h1>
        <p className="text-gray-500 mb-2">
          Reference <strong className="font-mono text-gray-700">{cancelled.referenceNumber}</strong> has been cancelled.
        </p>
        <p className="text-gray-500 text-sm mb-8">
          A cancellation confirmation has been sent to <strong>{cancelled.customerEmail}</strong>.
        </p>
        <a href="/book" className="btn-primary btn-lg">Book a New Appointment</a>
      </div>
    );
  }

  return (
    <div className="max-w-xl mx-auto px-4 sm:px-6 py-10 animate-fade-in">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">Cancel an Appointment</h1>
      <p className="text-gray-500 text-sm mb-8">
        Enter your reference number and the email address used when booking.
      </p>

      <Alert variant="warning" className="mb-6">
        Cancellations are final. You will receive a confirmation email once cancelled.
      </Alert>

      <form onSubmit={handleSubmit(onSubmit)} noValidate className="card p-6 sm:p-8 space-y-5">
        <div>
          <label className="form-label" htmlFor="referenceNumber">
            Reference Number <span className="text-red-500">*</span>
          </label>
          <input
            id="referenceNumber"
            {...register('referenceNumber')}
            type="text"
            placeholder="APB-YYYYMMDD-XXXX"
            className={`form-input uppercase tracking-widest font-mono ${errors.referenceNumber ? 'form-input-error' : ''}`}
          />
          {errors.referenceNumber && <p className="form-error">{errors.referenceNumber.message}</p>}
        </div>

        <div>
          <label className="form-label" htmlFor="cancelEmail">
            Email Address <span className="text-red-500">*</span>
          </label>
          <input
            id="cancelEmail"
            {...register('customerEmail')}
            type="email"
            autoComplete="email"
            placeholder="Email used when booking"
            className={`form-input ${errors.customerEmail ? 'form-input-error' : ''}`}
          />
          {errors.customerEmail && <p className="form-error">{errors.customerEmail.message}</p>}
        </div>

        <button type="submit" className="btn-danger w-full btn-lg" disabled={submitting}>
          {submitting ? <><Spinner size="sm" className="text-white" /> Cancelling…</> : 'Cancel Appointment'}
        </button>
      </form>
    </div>
  );
}
