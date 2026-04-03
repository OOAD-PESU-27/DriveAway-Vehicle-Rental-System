import { motion } from 'framer-motion';
import { FaSearch, FaChevronDown } from 'react-icons/fa';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function Hero() {
  const [location, setLocation] = useState('');
  const navigate = useNavigate();

  return (
    <section className="relative min-h-screen flex items-center overflow-hidden">
      <div className="absolute inset-0 z-0">
        <img
          src="https://images.unsplash.com/photo-1492144534655-ae79c964c9d7?w=1600"
          alt="Hero"
          className="w-full h-full object-cover"
        />
        <div className="absolute inset-0 bg-gradient-to-r from-blue-900/90 via-blue-900/70 to-transparent" />
      </div>

      <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="max-w-2xl">
          <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8 }}>
            <span className="inline-block bg-orange-500 text-white text-sm font-semibold px-4 py-2 rounded-full mb-6">🚗 Premium Vehicle Rentals</span>
            <h1 className="text-5xl md:text-7xl font-extrabold text-white leading-tight mb-6">
              Drive Your <span className="text-orange-400">Dream</span> Car Today
            </h1>
            <p className="text-xl text-blue-100 mb-10 leading-relaxed">
              Choose from 500+ premium vehicles. Best prices guaranteed. Free cancellation available.
            </p>
          </motion.div>

          <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8, delay: 0.2 }}
            className="bg-white rounded-2xl p-6 shadow-2xl">
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <label className="block text-sm font-medium text-gray-700 mb-1">Pickup Location</label>
                <div className="relative">
                  <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="City, airport, or address"
                    className="w-full pl-10 pr-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  />
                </div>
              </div>
              <button
                onClick={() => navigate('/vehicles')}
                className="btn-primary sm:self-end whitespace-nowrap"
              >
                Search Vehicles
              </button>
            </div>
          </motion.div>

          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.5 }}
            className="flex flex-wrap gap-6 mt-8 text-white">
            {[['500+', 'Vehicles'], ['50+', 'Locations'], ['4.9★', 'Rating'], ['24/7', 'Support']].map(([num, label]) => (
              <div key={label} className="text-center">
                <div className="text-2xl font-bold text-orange-400">{num}</div>
                <div className="text-sm text-blue-200">{label}</div>
              </div>
            ))}
          </motion.div>
        </div>
      </div>

      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 z-10 animate-bounce">
        <FaChevronDown className="text-white text-2xl opacity-70" />
      </div>
    </section>
  );
}
