import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { FaStar, FaUsers, FaGasPump, FaCog } from 'react-icons/fa';
import { mockVehicles } from '../../data/mockData';

export default function FeaturedVehicles() {
  const featured = mockVehicles.slice(0, 4);

  return (
    <section className="py-20 bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <motion.div initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}>
            <span className="text-blue-700 font-semibold text-sm uppercase tracking-wider">Our Fleet</span>
            <h2 className="section-title mt-2">Featured Vehicles</h2>
            <p className="section-subtitle">Handpicked premium cars for your perfect journey</p>
          </motion.div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {featured.map((vehicle, index) => (
            <motion.div key={vehicle.id} initial={{ opacity: 0, y: 30 }} whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }} transition={{ delay: index * 0.1 }}>
              <Link to={`/vehicles/${vehicle.id}`} className="card block group">
                <div className="relative h-48 overflow-hidden">
                  <img src={vehicle.image} alt={vehicle.name} className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500" />
                  <div className="absolute top-3 left-3 bg-blue-700 text-white text-xs px-3 py-1 rounded-full capitalize">{vehicle.type}</div>
                  {!vehicle.available && <div className="absolute inset-0 bg-black/50 flex items-center justify-center"><span className="text-white font-semibold">Not Available</span></div>}
                </div>
                <div className="p-4">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-bold text-gray-900 text-lg">{vehicle.name}</h3>
                    <div className="flex items-center text-orange-500 text-sm"><FaStar /><span className="ml-1 font-semibold">{vehicle.rating}</span></div>
                  </div>
                  <div className="flex items-center gap-4 text-gray-500 text-sm mb-4">
                    <span className="flex items-center gap-1"><FaUsers />{vehicle.seats}</span>
                    <span className="flex items-center gap-1"><FaGasPump />{vehicle.fuelType}</span>
                    <span className="flex items-center gap-1"><FaCog />{vehicle.transmission}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <div><span className="text-2xl font-bold text-blue-700">${vehicle.pricePerDay}</span><span className="text-gray-500 text-sm">/day</span></div>
                    <span className="text-blue-700 text-sm font-semibold group-hover:underline">Book Now →</span>
                  </div>
                </div>
              </Link>
            </motion.div>
          ))}
        </div>

        <div className="text-center mt-12">
          <Link to="/vehicles" className="btn-primary inline-block">View All Vehicles</Link>
        </div>
      </div>
    </section>
  );
}
