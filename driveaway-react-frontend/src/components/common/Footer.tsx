import { Link } from 'react-router-dom';
import { FaCar, FaFacebook, FaTwitter, FaInstagram, FaLinkedin, FaPhone, FaEnvelope, FaMapMarkerAlt } from 'react-icons/fa';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-white mt-16">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
          <div>
            <div className="flex items-center space-x-2 mb-4">
              <FaCar className="text-blue-400 text-2xl" />
              <span className="text-xl font-bold text-white">DriveAway</span>
            </div>
            <p className="text-gray-400 text-sm leading-relaxed">Your trusted vehicle rental partner. Premium cars, unbeatable prices, and exceptional service.</p>
            <div className="flex space-x-3 mt-6">
              {[FaFacebook, FaTwitter, FaInstagram, FaLinkedin].map((Icon, i) => (
                <a key={i} href="#" className="bg-gray-800 hover:bg-blue-700 p-2 rounded-full transition-colors"><Icon /></a>
              ))}
            </div>
          </div>
          <div>
            <h4 className="font-semibold text-white mb-4 text-lg">Quick Links</h4>
            <div className="space-y-2">
              {[['/', 'Home'], ['/vehicles', 'Vehicles'], ['/dashboard', 'Dashboard'], ['/login', 'Login']].map(([to, label]) => (
                <Link key={to} to={to} className="block text-gray-400 hover:text-white transition-colors text-sm">{label}</Link>
              ))}
            </div>
          </div>
          <div>
            <h4 className="font-semibold text-white mb-4 text-lg">Vehicle Types</h4>
            <div className="space-y-2 text-gray-400 text-sm">
              {['Sedans', 'SUVs', 'Luxury Cars', 'Economy Cars', 'Trucks', 'Vans'].map((t) => (
                <p key={t} className="hover:text-white cursor-pointer transition-colors">{t}</p>
              ))}
            </div>
          </div>
          <div>
            <h4 className="font-semibold text-white mb-4 text-lg">Contact Us</h4>
            <div className="space-y-3 text-gray-400 text-sm">
              <div className="flex items-center space-x-2"><FaPhone className="text-blue-400" /><span>+1 (555) 123-4567</span></div>
              <div className="flex items-center space-x-2"><FaEnvelope className="text-blue-400" /><span>info@driveaway.com</span></div>
              <div className="flex items-center space-x-2"><FaMapMarkerAlt className="text-blue-400" /><span>123 Main St, New York, NY</span></div>
            </div>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-12 pt-8 text-center text-gray-500 text-sm">
          <p>© 2024 DriveAway. All rights reserved. | Privacy Policy | Terms of Service</p>
        </div>
      </div>
    </footer>
  );
}
