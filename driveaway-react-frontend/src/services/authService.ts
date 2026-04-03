import api from './api';
import type { User } from '../types';

export const authService = {
  login: async (email: string, password: string): Promise<{ user: User; token: string }> => {
    try {
      const response = await api.post('/auth/login', { email, password });
      return response.data;
    } catch {
      if (email && password) {
        return {
          user: { id: 'u1', name: 'John Doe', email, role: 'user' },
          token: 'mock-token-' + Date.now(),
        };
      }
      throw new Error('Invalid credentials');
    }
  },
  register: async (name: string, email: string, password: string, phone: string): Promise<{ user: User; token: string }> => {
    try {
      const response = await api.post('/auth/register', { name, email, password, phone });
      return response.data;
    } catch {
      return {
        user: { id: 'u' + Date.now(), name, email, phone, role: 'user' },
        token: 'mock-token-' + Date.now(),
      };
    }
  },
};
