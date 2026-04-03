import Hero from '../components/home/Hero';
import FeaturedVehicles from '../components/home/FeaturedVehicles';
import Stats from '../components/home/Stats';
import Testimonials from '../components/home/Testimonials';
import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';

export default function Home() {
  return (
    <div>
      <Hero />
      <FeaturedVehicles />
      <Stats />
      <Testimonials />
      <section className="py-20 bg-gradient-to-r from-blue-900 to-blue-700">
        <div className="max-w-4xl mx-auto px-4 text-center">
          <motion.div initial={{ opacity: 0, y: 20 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }}>
            <h2 className="text-4xl md:text-5xl font-bold text-white mb-6">Ready to Hit the Road?</h2>
            <p className="text-blue-200 text-xl mb-10">Book your perfect vehicle today and enjoy the freedom of the open road.</p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/vehicles" className="btn-secondary text-center">Browse Vehicles</Link>
              <Link to="/register" className="bg-white text-blue-700 font-semibold py-3 px-6 rounded-lg hover:bg-blue-50 transition-all text-center">Get Started Free</Link>
            </div>
          </motion.div>
        </div>
      </section>
    </div>
  );
}
