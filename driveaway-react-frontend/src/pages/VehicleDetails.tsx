import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FaStar, FaUsers, FaGasPump, FaCog, FaMapMarkerAlt, FaCheck, FaArrowLeft } from 'react-icons/fa';
import { mockVehicles, mockReviews } from '../data/mockData';
import { useBookingStore } from '../store/bookingStore';
import { useAuthStore } from '../store/authStore';
import toast from 'react-hot-toast';

export default function VehicleDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const vehicle = mockVehicles.find((v) => v.id === id);
  const [activeImage, setActiveImage] = useState(0);
  const { setSelectedVehicle } = useBookingStore();
  const { isAuthenticated } = useAuthStore();

  if (!vehicle) return <div className="text-center py-20 text-gray-500">Vehicle not found.</div>;

  const reviews = mockReviews.filter((r) => r.vehicleId === id);

  const handleBook = () => {
    if (!isAuthenticated) { toast.error('Please login to book a vehicle'); navigate('/login'); return; }
    setSelectedVehicle(vehicle);
    navigate('/booking');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 py-10">
        <button onClick={() => navigate(-1)} className="flex items-center gap-2 text-blue-700 hover:text-blue-900 font-medium mb-6 transition-colors">
          <FaArrowLeft /> Back to Vehicles
        </button>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
          <div>
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="rounded-2xl overflow-hidden shadow-lg h-80">
              <img src={vehicle.images[activeImage]} alt={vehicle.name} className="w-full h-full object-cover" />
            </motion.div>
            <div className="flex gap-3 mt-4">
              {vehicle.images.map((img, i) => (
                <button key={i} onClick={() => setActiveImage(i)}
                  className={`w-20 h-16 rounded-xl overflow-hidden border-2 transition-all ${activeImage === i ? 'border-blue-700' : 'border-transparent'}`}>
                  <img src={img} alt="" className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          </div>

          <div>
            <div className="bg-white rounded-2xl shadow-md p-8">
              <div className="flex justify-between items-start mb-4">
                <div>
                  <span className="bg-blue-100 text-blue-700 text-sm font-semibold px-3 py-1 rounded-full capitalize">{vehicle.type}</span>
                  <h1 className="text-3xl font-bold text-gray-900 mt-2">{vehicle.name}</h1>
                  <div className="flex items-center gap-1 text-gray-500 text-sm mt-1"><FaMapMarkerAlt className="text-blue-500" />{vehicle.location}</div>
                </div>
                <div className="text-right">
                  <div className="flex items-center gap-1 justify-end"><FaStar className="text-orange-400" /><span className="font-bold text-lg">{vehicle.rating}</span></div>
                  <div className="text-gray-500 text-sm">({vehicle.reviews} reviews)</div>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 my-6 bg-gray-50 rounded-xl p-4">
                {[
                  { icon: FaUsers, label: `${vehicle.seats} Seats` },
                  { icon: FaGasPump, label: vehicle.fuelType },
                  { icon: FaCog, label: vehicle.transmission },
                  { icon: FaMapMarkerAlt, label: vehicle.mileage },
                ].map(({ icon: Icon, label }) => (
                  <div key={label} className="flex items-center gap-2 text-gray-600"><Icon className="text-blue-600" /><span>{label}</span></div>
                ))}
              </div>

              <div className="mb-6">
                <h3 className="font-bold text-gray-900 mb-3">Features</h3>
                <div className="flex flex-wrap gap-2">
                  {vehicle.features.map((f) => (
                    <span key={f} className="flex items-center gap-1 bg-green-50 text-green-700 px-3 py-1 rounded-full text-sm"><FaCheck className="text-xs" />{f}</span>
                  ))}
                </div>
              </div>

              <div className="border-t pt-6 flex justify-between items-center">
                <div><span className="text-4xl font-bold text-blue-700">${vehicle.pricePerDay}</span><span className="text-gray-500">/day</span></div>
                <button onClick={handleBook} disabled={!vehicle.available}
                  className={`btn-primary text-lg px-8 ${!vehicle.available ? 'opacity-50 cursor-not-allowed' : ''}`}>
                  {vehicle.available ? 'Book Now' : 'Not Available'}
                </button>
              </div>
            </div>
          </div>
        </div>

        {reviews.length > 0 && (
          <div className="mt-12 bg-white rounded-2xl shadow-md p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Customer Reviews</h2>
            <div className="space-y-6">
              {reviews.map((review) => (
                <div key={review.id} className="border-b pb-6 last:border-0">
                  <div className="flex items-center gap-3 mb-2">
                    <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center font-bold text-blue-700">{review.userName[0]}</div>
                    <div>
                      <div className="font-semibold">{review.userName}</div>
                      <div className="text-gray-500 text-sm">{review.createdAt}</div>
                    </div>
                    <div className="ml-auto flex items-center gap-1">{[...Array(review.rating)].map((_, i) => <FaStar key={i} className="text-orange-400 text-sm" />)}</div>
                  </div>
                  <p className="text-gray-600">{review.comment}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
