import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FaCar, FaBars, FaTimes, FaSignOutAlt } from 'react-icons/fa';
import { useAuthStore } from '../../store/authStore';

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const { isAuthenticated, user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="bg-white shadow-lg sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          <Link to="/" className="flex items-center space-x-2">
            <FaCar className="text-blue-700 text-2xl" />
            <span className="text-xl font-bold text-blue-700">DriveAway</span>
          </Link>

          <div className="hidden md:flex items-center space-x-8">
            <Link to="/" className="text-gray-600 hover:text-blue-700 font-medium transition-colors">Home</Link>
            <Link to="/vehicles" className="text-gray-600 hover:text-blue-700 font-medium transition-colors">Vehicles</Link>
            {isAuthenticated ? (
              <>
                <Link to="/dashboard" className="text-gray-600 hover:text-blue-700 font-medium transition-colors">Dashboard</Link>
                <div className="flex items-center space-x-3">
                  <span className="text-gray-700 font-medium">{user?.name}</span>
                  <button onClick={handleLogout} className="flex items-center space-x-1 text-red-500 hover:text-red-700 font-medium transition-colors">
                    <FaSignOutAlt /> <span>Logout</span>
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-3">
                <Link to="/login" className="text-gray-600 hover:text-blue-700 font-medium transition-colors">Login</Link>
                <Link to="/register" className="btn-primary text-sm py-2 px-4">Register</Link>
              </div>
            )}
          </div>

          <button className="md:hidden" onClick={() => setIsOpen(!isOpen)}>
            {isOpen ? <FaTimes className="text-2xl" /> : <FaBars className="text-2xl" />}
          </button>
        </div>
      </div>

      {isOpen && (
        <div className="md:hidden bg-white border-t border-gray-100 px-4 py-4 space-y-3">
          <Link to="/" className="block text-gray-600 hover:text-blue-700 font-medium py-2" onClick={() => setIsOpen(false)}>Home</Link>
          <Link to="/vehicles" className="block text-gray-600 hover:text-blue-700 font-medium py-2" onClick={() => setIsOpen(false)}>Vehicles</Link>
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" className="block text-gray-600 hover:text-blue-700 font-medium py-2" onClick={() => setIsOpen(false)}>Dashboard</Link>
              <button onClick={handleLogout} className="block text-red-500 font-medium py-2">Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" className="block text-gray-600 hover:text-blue-700 font-medium py-2" onClick={() => setIsOpen(false)}>Login</Link>
              <Link to="/register" className="block btn-primary text-center py-2" onClick={() => setIsOpen(false)}>Register</Link>
            </>
          )}
        </div>
      )}
    </nav>
  );
}
