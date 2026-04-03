import api from './api';
import type { Booking } from '../types';
import { mockBookings } from '../data/mockData';

export const bookingService = {
  create: async (booking: Partial<Booking>): Promise<Booking> => {
    try {
      const response = await api.post('/bookings', booking);
      return response.data;
    } catch {
      const newBooking: Booking = {
        id: 'b' + Date.now(),
        vehicleId: booking.vehicleId || '',
        userId: booking.userId || '',
        startDate: booking.startDate || '',
        endDate: booking.endDate || '',
        pickupLocation: booking.pickupLocation || '',
        dropoffLocation: booking.dropoffLocation || '',
        totalAmount: booking.totalAmount || 0,
        status: 'confirmed',
        paymentStatus: 'paid',
        createdAt: new Date().toISOString(),
      };
      return newBooking;
    }
  },
  getUserBookings: async (_userId: string): Promise<Booking[]> => {
    try {
      const response = await api.get(`/bookings/user/${_userId}`);
      return response.data;
    } catch {
      return mockBookings;
    }
  },
};
