import { motion } from 'framer-motion';
import { FaCar, FaUsers, FaMapMarkerAlt, FaAward } from 'react-icons/fa';

const stats = [
  { icon: FaCar, value: '500+', label: 'Premium Vehicles', color: 'bg-blue-100 text-blue-700' },
  { icon: FaUsers, value: '50,000+', label: 'Happy Customers', color: 'bg-orange-100 text-orange-700' },
  { icon: FaMapMarkerAlt, value: '50+', label: 'Locations', color: 'bg-green-100 text-green-700' },
  { icon: FaAward, value: '4.9/5', label: 'Average Rating', color: 'bg-purple-100 text-purple-700' },
];

export default function Stats() {
  return (
    <section className="py-16 bg-blue-700">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          {stats.map((stat, i) => (
            <motion.div key={stat.label} initial={{ opacity: 0, scale: 0.9 }} whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }} transition={{ delay: i * 0.1 }} className="text-center text-white">
              <div className="flex justify-center mb-3">
                <div className="bg-white/20 p-4 rounded-full"><stat.icon className="text-3xl" /></div>
              </div>
              <div className="text-4xl font-extrabold mb-1">{stat.value}</div>
              <div className="text-blue-200 text-sm">{stat.label}</div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
