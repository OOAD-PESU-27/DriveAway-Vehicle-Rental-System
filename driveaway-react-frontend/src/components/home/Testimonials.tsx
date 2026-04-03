import { motion } from 'framer-motion';
import { FaStar, FaQuoteLeft } from 'react-icons/fa';

const testimonials = [
  { name: 'Sarah Johnson', role: 'Business Traveler', avatar: 'https://i.pravatar.cc/100?img=1', rating: 5, text: 'DriveAway made my business trips so much easier! The cars are always clean and in perfect condition. Highly recommended!' },
  { name: 'Michael Chen', role: 'Family Vacationer', avatar: 'https://i.pravatar.cc/100?img=3', rating: 5, text: 'We rented an SUV for our family vacation and it was perfect. Spacious, comfortable, and great value for money.' },
  { name: 'Emma Davis', role: 'Adventure Seeker', avatar: 'https://i.pravatar.cc/100?img=5', rating: 5, text: "The Jeep Wrangler we rented for our off-road adventure was incredible. DriveAway's service was top-notch from start to finish." },
];

export default function Testimonials() {
  return (
    <section className="py-20 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-16">
          <motion.div initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}>
            <span className="text-blue-700 font-semibold text-sm uppercase tracking-wider">Testimonials</span>
            <h2 className="section-title mt-2">What Our Customers Say</h2>
            <p className="section-subtitle">Join thousands of happy customers who trust DriveAway</p>
          </motion.div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {testimonials.map((t, i) => (
            <motion.div key={t.name} initial={{ opacity: 0, y: 30 }} whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }} transition={{ delay: i * 0.1 }}
              className="bg-gray-50 rounded-2xl p-8 relative">
              <FaQuoteLeft className="text-blue-200 text-4xl absolute top-6 right-6" />
              <div className="flex items-center gap-1 mb-4">
                {[...Array(t.rating)].map((_, j) => <FaStar key={j} className="text-orange-400" />)}
              </div>
              <p className="text-gray-600 mb-6 leading-relaxed">"{t.text}"</p>
              <div className="flex items-center gap-3">
                <img src={t.avatar} alt={t.name} className="w-12 h-12 rounded-full object-cover" />
                <div>
                  <div className="font-semibold text-gray-900">{t.name}</div>
                  <div className="text-sm text-gray-500">{t.role}</div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
