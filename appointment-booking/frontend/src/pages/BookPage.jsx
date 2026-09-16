import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import toast from 'react-hot-toast';
import { format, addDays } from 'date-fns';

import { bookAppointment } from '../api/appointments';
import { useBranches } from '../hooks/useBranches';
import { useAvailableSlots } from '../hooks/useAvailableSlots';
import { bookingSchema, SERVICE_TYPE_LABELS } from '../utils/validation';
import { useAuth } from '../context/AuthContext';
import StepIndicator from '../components/ui/StepIndicator';
import LoginModal from '../components/ui/LoginModal';
import Spinner from '../components/ui/Spinner';
import Alert from '../components/ui/Alert';

// Step 0 is the account-type chooser (not shown in the indicator)
// Steps 1-3 are shown in the indicator
const STEPS = ['Your Details', 'Schedule', 'Review'];

export default function BookPage() {
  const navigate  = useNavigate();
  const { user, logout } = useAuth();

  // ─── Flow state ─────────────────────────────────────────────────────────────
  // 'choose'    → step 0: has account / guest chooser
  // 'auth'      → authenticated customer (pre-filled)
  // 'guest'     → manual entry
  const [flowType,      setFlowType]      = useState('choose');
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [step,           setStep]           = useState(1);
  const [submitting,     setSubmitting]     = useState(false);

  const { branches, loading: branchesLoading } = useBranches();

  const {
    register,
    handleSubmit,
    watch,
    trigger,
    getValues,
    setValue,
    formState: { errors },
  } = useForm({
    resolver: yupResolver(bookingSchema),
    mode: 'onTouched',
    defaultValues: {
      customerName:    '',
      customerEmail:   '',
      customerPhone:   '',
      idNumber:        '',
      branchId:        '',
      serviceType:     '',
      appointmentDate: '',
      appointmentTime: '',
      notes:           '',
    },
  });

  // ─── Pre-fill form when a user logs in ──────────────────────────────────────
  useEffect(() => {
    if (user && flowType === 'auth') {
      setValue('customerName',  user.customerName,  { shouldValidate: true });
      setValue('customerEmail', user.customerEmail, { shouldValidate: true });
      setValue('customerPhone', user.customerPhone, { shouldValidate: true });
      setValue('idNumber',      user.idNumber,      { shouldValidate: true });
    }
  }, [user, flowType, setValue]);

  const watchedBranchId    = watch('branchId');
  const watchedDate        = watch('appointmentDate');
  const watchedTime        = watch('appointmentTime');
  const watchedServiceType = watch('serviceType');

  const { slots, loading: slotsLoading } = useAvailableSlots(
    watchedBranchId ? Number(watchedBranchId) : null,
    watchedDate || null
  );

  const selectedBranch = branches.find((b) => b.id === Number(watchedBranchId));

  // ─── Step navigation ─────────────────────────────────────────────────────────
  const stepFields = {
    1: ['customerName', 'customerEmail', 'customerPhone', 'idNumber'],
    2: ['branchId', 'serviceType', 'appointmentDate', 'appointmentTime'],
  };

  const goNext = async () => {
    const valid = await trigger(stepFields[step]);
    if (valid) setStep((s) => s + 1);
  };

  const goBack = () => {
    if (step === 1) {
      // Back to the chooser: reset flow + logout if they were authenticated
      setFlowType('choose');
      setStep(1);
      if (flowType === 'auth') logout();
    } else {
      setStep((s) => s - 1);
    }
  };

  // ─── Guest flow selection ────────────────────────────────────────────────────
  const handleSelectGuest = () => {
    setFlowType('guest');
    setStep(1);
  };

  // ─── Auth flow: modal success callback ───────────────────────────────────────
  const handleLoginSuccess = (profile) => {
    // Pre-fill immediately with the profile returned by the modal
    // (don't wait for the useEffect — user state and flowType update asynchronously)
    setValue('customerName',  profile.customerName,  { shouldValidate: true });
    setValue('customerEmail', profile.customerEmail, { shouldValidate: true });
    setValue('customerPhone', profile.customerPhone, { shouldValidate: true });
    setValue('idNumber',      profile.idNumber,      { shouldValidate: true });
    setShowLoginModal(false);
    setFlowType('auth');
    setStep(1);
    toast.success(`Welcome, ${profile.customerName.split(' ')[0]}! Your details have been filled in.`);
  };

  // ─── Submit ──────────────────────────────────────────────────────────────────
  const onSubmit = async (data) => {
    setSubmitting(true);
    try {
      const payload = {
        ...data,
        branchId:        Number(data.branchId),
        idNumber:        data.idNumber || null,
        notes:           data.notes   || null,
        appointmentDate: format(new Date(data.appointmentDate), 'yyyy-MM-dd'),
      };

      const res = await bookAppointment(payload);
      toast.success('Appointment booked! Check your email for confirmation.');
      navigate('/confirmation', {
        state: {
          appointment:     res.data,
          emailPreviewUrl: res.emailPreviewUrl,
          branchName:      selectedBranch?.name,
        },
      });
    } catch (err) {
      if (err.code === 'CONFLICT') {
        toast.error('That slot was just taken. Please choose another time.');
        setStep(2);
      } else if (err.errors?.length) {
        err.errors.forEach((e) => toast.error(`${e.field}: ${e.message}`));
      } else {
        toast.error(err.message || 'Booking failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const minDate = format(addDays(new Date(), 1), 'yyyy-MM-dd');
  const maxDate = format(addDays(new Date(), 90), 'yyyy-MM-dd');

  return (
    <div className="max-w-2xl mx-auto px-4 sm:px-6 py-10 animate-fade-in">
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-2">Book an Appointment</h1>
      <p className="text-gray-500 text-sm mb-8">Complete all steps to secure your appointment.</p>

      {/* ─── Step 0: Account-type chooser ─────────────────────────────────── */}
      {flowType === 'choose' && (
        <div className="animate-slide-up space-y-4">
          <p className="text-sm font-medium text-gray-700 mb-2">Do you have a Capitec account?</p>

          {/* Has account */}
          <button
            type="button"
            onClick={() => setShowLoginModal(true)}
            className="w-full flex items-start gap-4 card p-5 text-left hover:border-brand-400 hover:shadow-md transition-all group"
          >
            <div className="w-11 h-11 rounded-full bg-brand-100 text-brand-600 flex items-center justify-center text-xl flex-shrink-0 group-hover:bg-brand-200 transition-colors">
              🔐
            </div>
            <div>
              <p className="font-semibold text-gray-900">Yes, I have a Capitec account</p>
              <p className="text-sm text-gray-500 mt-0.5">
                Sign in with your ID number and remote PIN — we'll fill in your details automatically.
              </p>
            </div>
            <svg className="w-5 h-5 text-gray-300 group-hover:text-brand-400 ml-auto mt-1 flex-shrink-0 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>

          {/* Guest */}
          <button
            type="button"
            onClick={handleSelectGuest}
            className="w-full flex items-start gap-4 card p-5 text-left hover:border-gray-300 hover:shadow-md transition-all group"
          >
            <div className="w-11 h-11 rounded-full bg-gray-100 text-gray-600 flex items-center justify-center text-xl flex-shrink-0 group-hover:bg-gray-200 transition-colors">
              👤
            </div>
            <div>
              <p className="font-semibold text-gray-900">No, I'm a guest</p>
              <p className="text-sm text-gray-500 mt-0.5">
                I'll fill in my details manually to continue.
              </p>
            </div>
            <svg className="w-5 h-5 text-gray-300 group-hover:text-gray-400 ml-auto mt-1 flex-shrink-0 transition-colors" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      )}

      {/* ─── Steps 1-3 ────────────────────────────────────────────────────── */}
      {flowType !== 'choose' && (
        <>
          {/* Authenticated banner */}
          {flowType === 'auth' && user && (
            <div className="flex items-center justify-between bg-green-50 border border-green-200 rounded-xl px-4 py-3 mb-6 text-sm">
              <div className="flex items-center gap-2 text-green-800">
                <span className="text-base">✅</span>
                <span>
                  Signed in as <strong>{user.customerName}</strong> — your details are pre-filled.
                </span>
              </div>
              <button
                type="button"
                onClick={() => { logout(); setFlowType('choose'); setStep(1); }}
                className="text-xs text-green-700 underline underline-offset-2 hover:text-green-900 transition-colors ml-3 flex-shrink-0"
              >
                Sign out
              </button>
            </div>
          )}

          <StepIndicator steps={STEPS} current={step} />

          <form onSubmit={handleSubmit(onSubmit)} noValidate>

            {/* ─── Step 1: Personal Details ──────────────────────────────── */}
            {step === 1 && (
              <div className="card p-6 sm:p-8 animate-slide-up space-y-5">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">Your Details</h2>
                  {flowType === 'auth' && (
                    <span className="inline-flex items-center gap-1.5 text-xs font-medium text-green-700 bg-green-50 border border-green-200 rounded-full px-2.5 py-1">
                      <svg className="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                      </svg>
                      Auto-filled from your account
                    </span>
                  )}
                </div>

                {/* Full Name */}
                <div>
                  <label className="form-label" htmlFor="customerName">
                    Full Name <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="customerName"
                    {...register('customerName')}
                    type="text"
                    autoComplete="name"
                    placeholder="e.g. Jane Doe"
                    readOnly={flowType === 'auth'}
                    className={`form-input ${errors.customerName ? 'form-input-error' : ''} ${flowType === 'auth' ? 'bg-gray-50 text-gray-600 cursor-default' : ''}`}
                  />
                  {errors.customerName && <p className="form-error">{errors.customerName.message}</p>}
                </div>

                {/* Email */}
                <div>
                  <label className="form-label" htmlFor="customerEmail">
                    Email Address <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="customerEmail"
                    {...register('customerEmail')}
                    type="email"
                    autoComplete="email"
                    placeholder="e.g. jane@example.com"
                    readOnly={flowType === 'auth'}
                    className={`form-input ${errors.customerEmail ? 'form-input-error' : ''} ${flowType === 'auth' ? 'bg-gray-50 text-gray-600 cursor-default' : ''}`}
                  />
                  {errors.customerEmail && <p className="form-error">{errors.customerEmail.message}</p>}
                </div>

                {/* Phone */}
                <div>
                  <label className="form-label" htmlFor="customerPhone">
                    Phone Number <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="customerPhone"
                    {...register('customerPhone')}
                    type="tel"
                    autoComplete="tel"
                    placeholder="e.g. +27 82 123 4567"
                    readOnly={flowType === 'auth'}
                    className={`form-input ${errors.customerPhone ? 'form-input-error' : ''} ${flowType === 'auth' ? 'bg-gray-50 text-gray-600 cursor-default' : ''}`}
                  />
                  {errors.customerPhone && <p className="form-error">{errors.customerPhone.message}</p>}
                </div>

                {/* ID Number */}
                <div>
                  <label className="form-label" htmlFor="idNumber">
                    SA ID Number{' '}
                    {flowType === 'guest'
                      ? <span className="text-gray-400 font-normal">(optional)</span>
                      : <span className="text-red-500">*</span>
                    }
                  </label>
                  <input
                    id="idNumber"
                    {...register('idNumber')}
                    type="text"
                    inputMode="numeric"
                    maxLength={13}
                    placeholder="13-digit ID number"
                    readOnly={flowType === 'auth'}
                    className={`form-input ${errors.idNumber ? 'form-input-error' : ''} ${flowType === 'auth' ? 'bg-gray-50 text-gray-600 cursor-default' : ''}`}
                  />
                  {errors.idNumber
                    ? <p className="form-error">{errors.idNumber.message}</p>
                    : <p className="text-xs text-gray-400 mt-1">
                        {flowType === 'auth'
                          ? '🔒 Verified and encrypted on your account.'
                          : 'Your ID number is encrypted before storage.'
                        }
                      </p>
                  }
                </div>

                <div className="flex gap-3 pt-2">
                  <button type="button" onClick={goBack} className="btn-secondary btn-lg">
                    ← Back
                  </button>
                  <button type="button" onClick={goNext} className="btn-primary btn-lg flex-1 sm:flex-none">
                    Continue to Schedule →
                  </button>
                </div>
              </div>
            )}

            {/* ─── Step 2: Schedule ─────────────────────────────────────── */}
            {step === 2 && (
              <div className="card p-6 sm:p-8 animate-slide-up space-y-5">
                <h2 className="text-lg font-semibold text-gray-900">Schedule Your Visit</h2>

                {/* Branch */}
                <div>
                  <label className="form-label" htmlFor="branchId">
                    Branch <span className="text-red-500">*</span>
                  </label>
                  {branchesLoading
                    ? <div className="skeleton h-10 w-full" />
                    : (
                      <select
                        id="branchId"
                        {...register('branchId')}
                        className={`form-input ${errors.branchId ? 'form-input-error' : ''}`}
                      >
                        <option value="">— Select a branch —</option>
                        {branches.map((b) => (
                          <option key={b.id} value={b.id}>{b.name} — {b.city}</option>
                        ))}
                      </select>
                    )}
                  {errors.branchId && <p className="form-error">{errors.branchId.message}</p>}
                </div>

                {/* Service */}
                <div>
                  <label className="form-label" htmlFor="serviceType">
                    Service Type <span className="text-red-500">*</span>
                  </label>
                  <select
                    id="serviceType"
                    {...register('serviceType')}
                    className={`form-input ${errors.serviceType ? 'form-input-error' : ''}`}
                  >
                    <option value="">— Select a service —</option>
                    {Object.entries(SERVICE_TYPE_LABELS).map(([val, label]) => (
                      <option key={val} value={val}>{label}</option>
                    ))}
                  </select>
                  {errors.serviceType && <p className="form-error">{errors.serviceType.message}</p>}
                </div>

                {/* Date */}
                <div>
                  <label className="form-label" htmlFor="appointmentDate">
                    Date <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="appointmentDate"
                    {...register('appointmentDate')}
                    type="date"
                    min={minDate}
                    max={maxDate}
                    className={`form-input ${errors.appointmentDate ? 'form-input-error' : ''}`}
                  />
                  {errors.appointmentDate && <p className="form-error">{errors.appointmentDate.message}</p>}
                </div>

                {/* Time slots */}
                <div>
                  <label className="form-label">Time Slot <span className="text-red-500">*</span></label>
                  {slotsLoading && (
                    <div className="flex items-center gap-2 text-sm text-gray-500 py-3">
                      <Spinner size="sm" /> Checking availability…
                    </div>
                  )}
                  {!slotsLoading && watchedBranchId && watchedDate && slots.length === 0 && (
                    <Alert variant="warning">No available slots for this date. Please choose another date.</Alert>
                  )}
                  {!slotsLoading && slots.length > 0 && (
                    <div className="grid grid-cols-3 sm:grid-cols-4 gap-2 mt-1">
                      {slots.map((slot) => (
                        <label
                          key={slot}
                          className={`flex items-center justify-center py-2.5 px-3 rounded-lg border text-sm font-medium cursor-pointer transition-all
                            ${watchedTime === slot
                              ? 'border-brand-500 bg-brand-50 text-brand-700 ring-2 ring-brand-300'
                              : 'border-gray-200 hover:border-brand-300 hover:bg-gray-50'}`}
                        >
                          <input type="radio" {...register('appointmentTime')} value={slot} className="sr-only" />
                          {slot}
                        </label>
                      ))}
                    </div>
                  )}
                  {(!watchedBranchId || !watchedDate) && (
                    <p className="text-xs text-gray-400 mt-1">Select a branch and date to see available times.</p>
                  )}
                  {errors.appointmentTime && <p className="form-error">{errors.appointmentTime.message}</p>}
                </div>

                {/* Notes */}
                <div>
                  <label className="form-label" htmlFor="notes">
                    Additional Notes <span className="text-gray-400 font-normal">(optional)</span>
                  </label>
                  <textarea
                    id="notes"
                    {...register('notes')}
                    rows={3}
                    placeholder="Any special requirements or additional information…"
                    className={`form-input resize-none ${errors.notes ? 'form-input-error' : ''}`}
                  />
                  {errors.notes && <p className="form-error">{errors.notes.message}</p>}
                </div>

                <div className="flex gap-3 pt-2">
                  <button type="button" onClick={goBack} className="btn-secondary btn-lg">
                    ← Back
                  </button>
                  <button type="button" onClick={goNext} className="btn-primary btn-lg flex-1 sm:flex-none">
                    Review Booking →
                  </button>
                </div>
              </div>
            )}

            {/* ─── Step 3: Review ───────────────────────────────────────── */}
            {step === 3 && (
              <div className="card p-6 sm:p-8 animate-slide-up">
                <h2 className="text-lg font-semibold text-gray-900 mb-5">Review Your Booking</h2>

                {(() => {
                  // Read fresh values and resolve branch name inside the render block
                  const v = getValues();
                  const branch = branches.find((b) => b.id === Number(v.branchId));
                  const branchLabel = branch?.name
                    ?? (branchesLoading ? 'Loading…' : `Branch #${v.branchId}`);

                  return (
                    <dl className="space-y-3 text-sm">
                      {[
                        ['Full Name',  v.customerName],
                        ['Email',      v.customerEmail],
                        ['Phone',      v.customerPhone],
                        ['Branch',     branchLabel],
                        ['Service',    SERVICE_TYPE_LABELS[v.serviceType]],
                        ['Date',       v.appointmentDate],
                        ['Time',       v.appointmentTime],
                        v.notes && ['Notes', v.notes],
                      ].filter(Boolean).map(([label, val]) => (
                        <div key={label} className="flex justify-between gap-4 py-2.5 border-b border-gray-100 last:border-0">
                          <dt className="text-gray-500 font-medium flex-shrink-0">{label}</dt>
                          <dd className="text-gray-900 font-medium text-right">{val}</dd>
                        </div>
                      ))}
                    </dl>
                  );
                })()}

                {flowType === 'auth' && (
                  <div className="flex items-center gap-2 mt-4 text-xs text-green-700 bg-green-50 border border-green-200 rounded-lg px-3 py-2">
                    <span>🔒</span>
                    <span>Details verified from your Capitec account.</span>
                  </div>
                )}

                <Alert variant="info" className="mt-4">
                  Please double-check your details. A confirmation will be sent to <strong>{getValues('customerEmail')}</strong>.
                </Alert>

                <div className="flex gap-3 mt-6">
                  <button type="button" onClick={goBack} className="btn-secondary btn-lg" disabled={submitting}>
                    ← Edit
                  </button>
                  <button type="submit" className="btn-primary btn-lg flex-1 sm:flex-none" disabled={submitting}>
                    {submitting
                      ? <><Spinner size="sm" className="text-white" /> Booking…</>
                      : 'Confirm Booking'
                    }
                  </button>
                </div>
              </div>
            )}
          </form>
        </>
      )}

      {/* ─── Login Modal ──────────────────────────────────────────────────── */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
        onSuccess={handleLoginSuccess}
      />
    </div>
  );
}
