import client from './client';

/**
 * Authenticate a customer with their SA ID number and remote PIN.
 *
 * POST /api/v1/customers/authenticate
 *
 * @param {string} idNumber  - 13-digit SA ID number (plain)
 * @param {string} pin       - 4-6 digit remote PIN (plain)
 * @returns {Promise<CustomerProfileResponse>} pre-fill profile
 */
export const authenticateCustomer = (idNumber, pin) =>
  client.post('/customers/authenticate', { idNumber, pin });
