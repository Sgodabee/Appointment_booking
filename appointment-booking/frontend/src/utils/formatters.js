import { format, parseISO } from 'date-fns';

export const formatDate = (dateStr) => {
  try {
    const d = typeof dateStr === 'string' ? parseISO(dateStr) : dateStr;
    return format(d, 'EEEE, d MMMM yyyy');
  } catch {
    return dateStr;
  }
};

export const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const [h, m] = timeStr.split(':');
  const hour = parseInt(h, 10);
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour % 12 || 12;
  return `${displayHour}:${m} ${ampm}`;
};

export const formatServiceType = (serviceType) => {
  const labels = {
    account_opening:     'Account Opening',
    loan_application:    'Loan Application',
    card_services:       'Card Services',
    general_enquiry:     'General Enquiry',
    document_submission: 'Document Submission',
    investment_advice:   'Investment Advice',
  };
  return labels[serviceType] ?? serviceType?.replace(/_/g, ' ');
};
