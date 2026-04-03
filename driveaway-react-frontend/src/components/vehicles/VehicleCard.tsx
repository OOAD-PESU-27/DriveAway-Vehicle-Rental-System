import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FaStar, FaUsers, FaGasPump, FaCog, FaMapMarkerAlt, FaHeart } from 'react-icons/fa';
import type { Vehicle } from '../../types';
import { useState } from 'react';

interface Props { vehicle: Vehicle; index?: number; }

export default function VehicleCard({ vehicle, index = 0 }: Props) {
  const [liked, setLiked] = useState(false);

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.05 }}>
      <div className="card group">
        <div className="relative h-52 overflow-hidden">
          <img src={vehicle.image} alt={vehicle.name} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
          <div className="absolute top-3 left-3 bg-blue-700 text-white text-xs px-3 py-1 rounded-full capitalize">{vehicle.type}</div>
          <button onClick={() => setLiked(!liked)} className="absolute top-3 right-3 bg-white/90 p-2 rounded-full hover:bg-white transition-colors">
            <FaHeart className={liked ? 'text-red-500' : 'text-gray-400'} />
          </button>
          {!vehicle.available && <div className="absolute inset-0 bg-black/50 flex items-center justify-center"><span className="bg-red-500 text-white font-semibold px-4 py-2 rounded-full">Not Available</span></div>}
        </div>
        <div className="p-5">
          <div className="flex justify-between items-start mb-3">
            <div>
              <h3 className="font-bold text-gray-900 text-lg">{vehicle.name}</h3>
              <div className="flex items-center gap-1 text-gray-500 text-sm mt-1"><FaMapMarkerAlt className="text-blue-500" /><span>{vehicle.location}</span></div>
            </div>
            <div className="flex items-center bg-orange-50 px-2 py-1 rounded-lg"><FaStar className="text-orange-400 text-sm" /><span className="ml-1 font-semibold text-sm text-orange-600">{vehicle.rating}</span></div>
          </div>
          <div className="grid grid-cols-3 gap-2 text-gray-500 text-sm mb-4 bg-gray-50 rounded-xl p-3">
            <span className="flex items-center gap-1 justify-center"><FaUsers className="text-blue-500" />{vehicle.seats} seats</span>
            <span className="flex items-center gap-1 justify-center"><FaGasPump className="text-green-500" />{vehicle.fuelType}</span>
            <span className="flex items-center gap-1 justify-center"><FaCog className="text-purple-500" />{vehicle.transmission === 'automatic' ? 'Auto' : 'Manual'}</span>
          </div>
          <div className="flex justify-between items-center">
            <div><span className="text-3xl font-bold text-blue-700">${vehicle.pricePerDay}</span><span className="text-gray-500 text-sm">/day</span></div>
            {vehicle.available ? (
              <Link to={`/vehicles/${vehicle.id}`} className="btn-primary text-sm py-2 px-5">Book Now</Link>
            ) : (
              <button disabled className="bg-gray-200 text-gray-500 text-sm py-2 px-5 rounded-lg cursor-not-allowed">Unavailable</button>
            )}
          </div>
        </div>
      </div>
    </motion.div>
  );
}
