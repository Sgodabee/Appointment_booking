import axios from 'axios';
import toast from 'react-hot-toast';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000/api/v1';

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: false,
});

// ─── In-memory token store ─────────────────────────────────────────────────
// Stored in a closure — never in localStorage (XSS risk).
// Both EmployeeAuthContext and AuthContext call setAuthToken() after login
// and clearAuthToken() on logout.
let _token = null;

export const setAuthToken   = (token) => { _token = token; };
export const clearAuthToken = ()      => { _token = null;  };
export const getAuthToken   = ()      => _token;

// ─── Request interceptor ──────────────────────────────────────────────────
client.interceptors.request.use(
  (config) => {
    config.headers['X-Request-ID'] = crypto.randomUUID();
    if (_token) {
      config.headers['Authorization'] = `Bearer ${_token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response interceptor ─────────────────────────────────────────────────
client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (!error.response) {
      toast.error('Cannot reach the server. Please check your connection.');
      return Promise.reject(new Error('Network error'));
    }

    const { status, data } = error.response;

    if (status === 401) {
      // Token expired or invalid — clear it so the UI redirects to login
      clearAuthToken();
      toast.error('Your session has expired. Please sign in again.');
    }

    if (status === 403) {
      toast.error('You do not have permission to perform this action.');
    }

    if (status === 429) {
      toast.error('Too many requests. Please wait a moment and try again.');
    }

    if (status >= 500) {
      toast.error('Something went wrong on our end. Please try again later.');
    }

    return Promise.reject({
      status,
      code:    data?.code,
      message: data?.message || 'An error occurred',
      errors:  data?.errors  || [],
    });
  }
);

export default client;
