import client from './client';

/**
 * All appointment API calls.
 * Each function returns the `data` payload (unwrapped by the axios interceptor).
 */

export const bookAppointment = (payload) =>
  client.post('/appointments', payload);

export const getAppointmentByReference = (reference) =>
  client.get(`/appointments/${reference.toUpperCase()}`);

export const getAvailableSlots = (branchId, date) =>
  client.get('/appointments/availability', { params: { branchId, date } });

export const cancelAppointment = (referenceNumber, customerEmail) =>
  client.post('/appointments/cancel', { referenceNumber, customerEmail });

// ─── Admin ─────────────────────────────────────────────────────────────────────
export const listAppointments = (filters = {}) =>
  client.get('/appointments/admin/list', { params: filters });

export const updateAppointmentStatus = (id, status) =>
  client.patch(`/appointments/admin/${id}/status`, { status });
