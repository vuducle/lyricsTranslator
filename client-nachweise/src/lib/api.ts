import { store } from '@/store';
import { updateAccessToken } from '@/store/slices/userSlice';
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8088',
});

// Request interceptor to add auth token to headers
api.interceptors.request.use(
  (config) => {
    const token = store.getState().user.token;
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh
api.interceptors.response.use(
  (res) => {
    return res;
  },
  async (err) => {
    const originalConfig = err.config;

    if (originalConfig.url !== '/api/auth/login' && err.response) {
      // Access Token was expired
      if (err.response.status === 401 && !originalConfig._retry) {
        originalConfig._retry = true;

        try {
          const rs = await axios.post(
            '/api/auth/refresh',
            {
              refreshToken: store.getState().user.refreshToken,
            },
            {
              baseURL:
                process.env.NEXT_PUBLIC_API_URL ||
                'http://localhost:8088',
            }
          );

          const { accessToken } = rs.data;
          store.dispatch(updateAccessToken(accessToken));

          return api(originalConfig);
        } catch (_error) {
          return Promise.reject(_error);
        }
      }
    }

    return Promise.reject(err);
  }
);

export const forgotPassword = async (email: string) => {
  const response = await api.post('/api/auth/forgot-password', { email });
  return response.data;
};

export const resetPassword = async (token: string, newPassword: string) => {
  const response = await api.post('/api/auth/reset-password', { token, newPassword });
  return response.data;
};

export default api;
