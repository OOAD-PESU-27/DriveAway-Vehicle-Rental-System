import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useForm } from 'react-hook-form';
import BookingSteps from '../components/booking/BookingSteps';
import { useBookingStore } from '../store/bookingStore';
import { bookingService } from '../services/bookingService';
import { useAuthStore } from '../store/authStore';
import toast from 'react-hot-toast';
import { FaCar, FaCalendar, FaMapMarkerAlt, FaCreditCard, FaCheck } from 'react-icons/fa';

interface DateFormData { startDate: string; endDate: string; }
interface LocationFormData { pickupLocation: string; dropoffLocation: string; }
interface PaymentFormData { cardNumber: string; expiry: string; cvv: string; cardName: string; }

type StepFormData = DateFormData | LocationFormData | PaymentFormData;

export default function BookingPage() {
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const { selectedVehicle, updateBooking, currentBooking, addBooking, reset } = useBookingStore();
  const { user } = useAuthStore();
  const navigate = useNavigate();
  const { register, handleSubmit, watch, formState: { errors } } = useForm<DateFormData & LocationFormData & PaymentFormData>();

  if (!selectedVehicle) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <FaCar className="text-6xl text-gray-300 mx-auto mb-4" />
          <p className="text-gray-500 text-xl">No vehicle selected</p>
          <button onClick={() => navigate('/vehicles')} className="btn-primary mt-4">Browse Vehicles</button>
        </div>
      </div>
    );
  }

  const startDate = watch('startDate');
  const endDate = watch('endDate');
  const days = startDate && endDate ? Math.ceil((new Date(endDate).getTime() - new Date(startDate).getTime()) / (1000 * 60 * 60 * 24)) : 0;
  const totalAmount = days * selectedVehicle.pricePerDay;

  const onNext = (data: StepFormData) => {
    updateBooking(data as never);
    if (step < 4) setStep(step + 1);
  };

  const onPayment = async (_data: PaymentFormData) => {
    setLoading(true);
    try {
      const booking = await bookingService.create({
        vehicleId: selectedVehicle.id,
        userId: user?.id || '',
        startDate: currentBooking.startDate || '',
        endDate: currentBooking.endDate || '',
        pickupLocation: currentBooking.pickupLocation || '',
        dropoffLocation: currentBooking.dropoffLocation || '',
        totalAmount,
      });
      addBooking({ ...booking, vehicle: selectedVehicle });
      setStep(5);
      toast.success('Booking confirmed!');
    } catch {
      toast.error('Booking failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  if (step === 5) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <motion.div initial={{ opacity: 0, scale: 0.8 }} animate={{ opacity: 1, scale: 1 }} className="bg-white rounded-3xl shadow-2xl p-12 text-center max-w-md">
          <div className="bg-green-100 w-24 h-24 rounded-full flex items-center justify-center mx-auto mb-6"><FaCheck className="text-green-500 text-4xl" /></div>
          <h2 className="text-3xl font-bold text-gray-900 mb-3">Booking Confirmed!</h2>
          <p className="text-gray-500 mb-2">Your {selectedVehicle.name} has been booked.</p>
          <p className="text-2xl font-bold text-blue-700 mb-8">Total: ${totalAmount}</p>
          <div className="flex gap-4">
            <button onClick={() => { reset(); navigate('/dashboard'); }} className="btn-primary flex-1">View Dashboard</button>
            <button onClick={() => { reset(); navigate('/vehicles'); }} className="flex-1 border border-blue-700 text-blue-700 py-3 px-6 rounded-lg hover:bg-blue-50 font-semibold">Book Another</button>
          </div>
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-10">
      <div className="max-w-4xl mx-auto px-4">
        <BookingSteps currentStep={step} />

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <AnimatePresence mode="wait">
              <motion.div key={step} initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }}
                className="bg-white rounded-2xl shadow-md p-8">
                {step === 1 && (
                  <form onSubmit={handleSubmit(onNext as (data: DateFormData & LocationFormData & PaymentFormData) => void)}>
                    <h2 className="text-2xl font-bold mb-6 flex items-center gap-2"><FaCalendar className="text-blue-700" />Select Dates</h2>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Pickup Date</label>
                        <input {...register('startDate', { required: 'Required' })} type="date" min={new Date().toISOString().split('T')[0]}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                        {errors.startDate && <p className="text-red-500 text-sm mt-1">Required</p>}
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Return Date</label>
                        <input {...register('endDate', { required: 'Required' })} type="date" min={startDate}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                        {errors.endDate && <p className="text-red-500 text-sm mt-1">Required</p>}
                      </div>
                    </div>
                    {days > 0 && <div className="mt-6 bg-blue-50 rounded-xl p-4 text-blue-700 font-semibold">{days} day{days > 1 ? 's' : ''} = ${totalAmount}</div>}
                    <button type="submit" className="btn-primary w-full mt-6">Next: Select Location</button>
                  </form>
                )}

                {step === 2 && (
                  <form onSubmit={handleSubmit(onNext as (data: DateFormData & LocationFormData & PaymentFormData) => void)}>
                    <h2 className="text-2xl font-bold mb-6 flex items-center gap-2"><FaMapMarkerAlt className="text-blue-700" />Location</h2>
                    {(['pickupLocation', 'dropoffLocation'] as const).map((field) => (
                      <div key={field} className="mb-4">
                        <label className="block text-sm font-medium text-gray-700 mb-1">{field === 'pickupLocation' ? 'Pickup' : 'Drop-off'} Location</label>
                        <input {...register(field, { required: 'Required' })} placeholder="City, airport, or address"
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                      </div>
                    ))}
                    <div className="flex gap-4 mt-6">
                      <button type="button" onClick={() => setStep(1)} className="flex-1 border border-gray-300 text-gray-600 py-3 rounded-xl hover:bg-gray-50 font-medium">Back</button>
                      <button type="submit" className="flex-1 btn-primary">Next: Review</button>
                    </div>
                  </form>
                )}

                {step === 3 && (
                  <div>
                    <h2 className="text-2xl font-bold mb-6">Review Booking</h2>
                    <div className="space-y-4 text-gray-600">
                      <div className="flex justify-between border-b pb-3"><span>Vehicle</span><span className="font-semibold text-gray-900">{selectedVehicle.name}</span></div>
                      <div className="flex justify-between border-b pb-3"><span>Dates</span><span className="font-semibold text-gray-900">{currentBooking.startDate} → {currentBooking.endDate}</span></div>
                      <div className="flex justify-between border-b pb-3"><span>Duration</span><span className="font-semibold text-gray-900">{days} days</span></div>
                      <div className="flex justify-between border-b pb-3"><span>Pickup</span><span className="font-semibold text-gray-900">{currentBooking.pickupLocation}</span></div>
                      <div className="flex justify-between font-bold text-lg mt-4"><span>Total</span><span className="text-blue-700">${totalAmount}</span></div>
                    </div>
                    <div className="flex gap-4 mt-8">
                      <button onClick={() => setStep(2)} className="flex-1 border border-gray-300 text-gray-600 py-3 rounded-xl hover:bg-gray-50 font-medium">Back</button>
                      <button onClick={() => setStep(4)} className="flex-1 btn-primary">Proceed to Payment</button>
                    </div>
                  </div>
                )}

                {step === 4 && (
                  <form onSubmit={handleSubmit(onPayment as (data: DateFormData & LocationFormData & PaymentFormData) => void)}>
                    <h2 className="text-2xl font-bold mb-6 flex items-center gap-2"><FaCreditCard className="text-blue-700" />Payment</h2>
                    <div className="space-y-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Card Number</label>
                        <input {...register('cardNumber', { required: 'Required', minLength: { value: 16, message: '16 digits required' } })} placeholder="1234 5678 9012 3456" maxLength={16}
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none font-mono" />
                        {errors.cardNumber && <p className="text-red-500 text-sm mt-1">{errors.cardNumber.message}</p>}
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">Expiry</label>
                          <input {...register('expiry', { required: 'Required' })} placeholder="MM/YY"
                            className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                        </div>
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">CVV</label>
                          <input {...register('cvv', { required: 'Required' })} placeholder="123" maxLength={3}
                            className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                        </div>
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Cardholder Name</label>
                        <input {...register('cardName', { required: 'Required' })} placeholder="John Doe"
                          className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                      </div>
                    </div>
                    <div className="flex gap-4 mt-6">
                      <button type="button" onClick={() => setStep(3)} className="flex-1 border border-gray-300 text-gray-600 py-3 rounded-xl hover:bg-gray-50 font-medium">Back</button>
                      <button type="submit" disabled={loading} className="flex-1 btn-primary disabled:opacity-70">
                        {loading ? 'Processing...' : `Pay $${totalAmount}`}
                      </button>
                    </div>
                  </form>
                )}
              </motion.div>
            </AnimatePresence>
          </div>

          <div>
            <div className="bg-white rounded-2xl shadow-md overflow-hidden sticky top-20">
              <img src={selectedVehicle.image} alt={selectedVehicle.name} className="w-full h-44 object-cover" />
              <div className="p-5">
                <h3 className="font-bold text-gray-900 text-lg">{selectedVehicle.name}</h3>
                <p className="text-gray-500 text-sm capitalize">{selectedVehicle.type} · {selectedVehicle.year}</p>
                <div className="border-t mt-4 pt-4">
                  <div className="flex justify-between text-sm text-gray-600 mb-2"><span>${selectedVehicle.pricePerDay}/day</span><span>× {days || 0} days</span></div>
                  <div className="flex justify-between font-bold text-lg text-blue-700"><span>Total</span><span>${totalAmount}</span></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
