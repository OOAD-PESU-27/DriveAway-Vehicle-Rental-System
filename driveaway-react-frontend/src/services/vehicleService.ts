import api from './api';
import { mockVehicles } from '../data/mockData';
import type { Vehicle } from '../types';

export const vehicleService = {
  getAll: async (): Promise<Vehicle[]> => {
    try {
      const response = await api.get('/vehicles');
      return response.data;
    } catch {
      return mockVehicles;
    }
  },
  getById: async (id: string): Promise<Vehicle> => {
    try {
      const response = await api.get(`/vehicles/${id}`);
      return response.data;
    } catch {
      const vehicle = mockVehicles.find((v) => v.id === id);
      if (!vehicle) throw new Error('Vehicle not found');
      return vehicle;
    }
  },
};
