import { create } from 'zustand';
import type { Booking, Vehicle } from '../types';

interface BookingState {
  selectedVehicle: Vehicle | null;
  currentBooking: Partial<Booking>;
  bookings: Booking[];
  step: number;
  setSelectedVehicle: (vehicle: Vehicle) => void;
  updateBooking: (data: Partial<Booking>) => void;
  setStep: (step: number) => void;
  addBooking: (booking: Booking) => void;
  reset: () => void;
}

export const useBookingStore = create<BookingState>((set) => ({
  selectedVehicle: null,
  currentBooking: {},
  bookings: [],
  step: 1,
  setSelectedVehicle: (vehicle) => set({ selectedVehicle: vehicle }),
  updateBooking: (data) => set((state) => ({ currentBooking: { ...state.currentBooking, ...data } })),
  setStep: (step) => set({ step }),
  addBooking: (booking) => set((state) => ({ bookings: [...state.bookings, booking] })),
  reset: () => set({ selectedVehicle: null, currentBooking: {}, step: 1 }),
}));
