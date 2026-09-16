import * as yup from 'yup';

// ─── SA ID Luhn validation (mirrors backend) ───────────────────────────────────
export const validateSAId = (id) => {
  if (!id) return true; // optional field
  if (!/^\d{13}$/.test(id)) return false;
  let sum = 0;
  for (let i = 0; i < 12; i++) {
    const digit = parseInt(id[i], 10);
    if (i % 2 === 0) { sum += digit; }
    else { const d = digit * 2; sum += d > 9 ? d - 9 : d; }
  }
  return ((10 - (sum % 10)) % 10) === parseInt(id[12], 10);
};

const tomorrow = () => {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  d.setHours(0, 0, 0, 0);
  return d;
};

const maxDate = () => {
  const d = new Date();
  d.setDate(d.getDate() + 90);
  return d;
};

export const SERVICE_TYPE_LABELS = {
  account_opening:    'Account Opening',
  loan_application:   'Loan Application',
  card_services:      'Card Services',
  general_enquiry:    'General Enquiry',
  document_submission:'Document Submission',
  investment_advice:  'Investment Advice',
};

export const bookingSchema = yup.object({
  customerName: yup
    .string()
    .trim()
    .required('Full name is required')
    .min(2, 'Name must be at least 2 characters')
    .max(200, 'Name cannot exceed 200 characters')
    .matches(/^[a-zA-Z\s'-]+$/, 'Name may only contain letters, spaces, hyphens, and apostrophes'),

  customerEmail: yup
    .string()
    .trim()
    .required('Email address is required')
    .email('Please enter a valid email address')
    .max(255, 'Email too long'),

  customerPhone: yup
    .string()
    .trim()
    .required('Phone number is required')
    .matches(/^\+?[0-9\s\-()\\.]{7,20}$/, 'Please enter a valid phone number'),

  idNumber: yup
    .string()
    .optional()
    .nullable()
    .test('sa-id', 'Invalid South African ID number', (val) => {
      if (!val) return true;
      return validateSAId(val);
    }),

  branchId: yup
    .number()
    .required('Please select a branch')
    .positive('Please select a branch'),

  serviceType: yup
    .string()
    .required('Please select a service')
    .oneOf(Object.keys(SERVICE_TYPE_LABELS), 'Invalid service type'),

  appointmentDate: yup
    .date()
    .required('Please select a date')
    .min(tomorrow(), 'Appointment must be at least one day in advance')
    .max(maxDate(), 'Appointment cannot be more than 90 days away'),

  appointmentTime: yup
    .string()
    .required('Please select a time slot'),

  notes: yup
    .string()
    .optional()
    .nullable()
    .max(500, 'Notes cannot exceed 500 characters'),
});

export const cancelSchema = yup.object({
  referenceNumber: yup
    .string()
    .trim()
    .required('Reference number is required')
    .matches(/^APB-\d{8}-[A-F0-9]{4}$/i, 'Invalid reference number format (e.g. APB-20261001-AB12)'),

  customerEmail: yup
    .string()
    .trim()
    .required('Email address is required')
    .email('Please enter a valid email address'),
});
