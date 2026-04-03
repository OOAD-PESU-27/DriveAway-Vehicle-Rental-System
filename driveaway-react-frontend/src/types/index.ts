export interface Vehicle {
  id: string;
  name: string;
  brand: string;
  model: string;
  type: 'sedan' | 'suv' | 'luxury' | 'economy' | 'truck' | 'van';
  year: number;
  pricePerDay: number;
  transmission: 'automatic' | 'manual';
  seats: number;
  fuelType: string;
  mileage: string;
  image: string;
  images: string[];
  features: string[];
  rating: number;
  reviews: number;
  available: boolean;
  location: string;
}

export interface Booking {
  id: string;
  vehicleId: string;
  vehicle?: Vehicle;
  userId: string;
  startDate: string;
  endDate: string;
  pickupLocation: string;
  dropoffLocation: string;
  totalAmount: number;
  status: 'pending' | 'confirmed' | 'active' | 'completed' | 'cancelled';
  paymentStatus: 'pending' | 'paid' | 'refunded';
  createdAt: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  phone?: string;
  avatar?: string;
  role: 'user' | 'admin';
}

export interface Review {
  id: string;
  userId: string;
  userName: string;
  userAvatar?: string;
  vehicleId: string;
  rating: number;
  comment: string;
  createdAt: string;
}
