import axios from 'axios';
import toast from 'react-hot-toast';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:5000/api/v1';

const client = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: false,
});

// ─── Request interceptor ───────────────────────────────────────────────────────
client.interceptors.request.use(
  (config) => {
    // Attach a per-request ID for traceability
    config.headers['X-Request-ID'] = crypto.randomUUID();
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response interceptor ──────────────────────────────────────────────────────
client.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (!error.response) {
      // Network error / timeout
      toast.error('Cannot reach the server. Please check your connection.');
      return Promise.reject(new Error('Network error'));
    }

    const { status, data } = error.response;

    // 429 Rate limited
    if (status === 429) {
      toast.error('Too many requests. Please wait a moment and try again.');
    }

    // 500+ — don't expose backend details
    if (status >= 500) {
      toast.error('Something went wrong on our end. Please try again later.');
    }

    // Return structured error for callers to handle specifically
    return Promise.reject({
      status,
      code: data?.code,
      message: data?.message || 'An error occurred',
      errors: data?.errors || [],
    });
  }
);

export default client;
