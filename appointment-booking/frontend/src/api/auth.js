import client from './client';

/**
 * Authenticate a branch employee for admin dashboard access.
 *
 * POST /api/v1/auth/employee/login
 *
 * @param {string} username
 * @param {string} password
 * @returns {Promise<EmployeeProfileResponse>}
 */
export const employeeLogin = (username, password) =>
  client.post('/auth/employee/login', { username, password });
