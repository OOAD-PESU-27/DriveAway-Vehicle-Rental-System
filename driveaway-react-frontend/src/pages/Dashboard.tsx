import { useState } from 'react';
import { motion } from 'framer-motion';
import { FaCar, FaHistory, FaCreditCard, FaUser, FaCalendar } from 'react-icons/fa';
import { useAuthStore } from '../store/authStore';
import { useBookingStore } from '../store/bookingStore';
import { mockBookings } from '../data/mockData';
import { useNavigate } from 'react-router-dom';

type Tab = 'overview' | 'bookings' | 'history' | 'payments' | 'profile';

export default function Dashboard() {
  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const { user } = useAuthStore();
  const { bookings } = useBookingStore();
  const navigate = useNavigate();
  const allBookings = [...mockBookings, ...bookings];

  const tabs = [
    { id: 'overview' as Tab, icon: FaCar, label: 'Overview' },
    { id: 'bookings' as Tab, icon: FaCalendar, label: 'My Bookings' },
    { id: 'history' as Tab, icon: FaHistory, label: 'History' },
    { id: 'payments' as Tab, icon: FaCreditCard, label: 'Payments' },
    { id: 'profile' as Tab, icon: FaUser, label: 'Profile' },
  ];

  const statusColor: Record<string, string> = {
    confirmed: 'bg-blue-100 text-blue-700',
    completed: 'bg-green-100 text-green-700',
    cancelled: 'bg-red-100 text-red-700',
    pending: 'bg-yellow-100 text-yellow-700',
    active: 'bg-purple-100 text-purple-700',
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-gradient-to-r from-blue-900 to-blue-700 py-12">
        <div className="max-w-7xl mx-auto px-4">
          <h1 className="text-3xl font-bold text-white">Welcome back, {user?.name || 'User'}! 👋</h1>
          <p className="text-blue-200 mt-2">Manage your rentals and bookings</p>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex flex-col lg:flex-row gap-8">
          <div className="lg:w-64">
            <div className="bg-white rounded-2xl shadow-md p-4">
              <div className="text-center pb-4 border-b mb-4">
                <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center text-2xl font-bold text-blue-700 mx-auto mb-2">
                  {user?.name?.[0] || 'U'}
                </div>
                <div className="font-bold text-gray-900">{user?.name}</div>
                <div className="text-gray-500 text-sm">{user?.email}</div>
              </div>
              <nav className="space-y-1">
                {tabs.map(({ id, icon: Icon, label }) => (
                  <button key={id} onClick={() => setActiveTab(id)}
                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-colors ${activeTab === id ? 'bg-blue-700 text-white' : 'text-gray-600 hover:bg-gray-100'}`}>
                    <Icon /> {label}
                  </button>
                ))}
              </nav>
            </div>
          </div>

          <div className="flex-1">
            {activeTab === 'overview' && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
                  {[
                    { label: 'Total Rentals', value: allBookings.length, color: 'bg-blue-500' },
                    { label: 'Active', value: allBookings.filter((b) => b.status === 'confirmed').length, color: 'bg-green-500' },
                    { label: 'Completed', value: allBookings.filter((b) => b.status === 'completed').length, color: 'bg-purple-500' },
                    { label: 'Total Spent', value: `$${allBookings.reduce((s, b) => s + b.totalAmount, 0)}`, color: 'bg-orange-500' },
                  ].map((s) => (
                    <div key={s.label} className={`${s.color} text-white rounded-2xl p-5`}>
                      <div className="text-2xl font-bold">{s.value}</div>
                      <div className="text-sm opacity-80 mt-1">{s.label}</div>
                    </div>
                  ))}
                </div>
                <div className="bg-white rounded-2xl shadow-md p-6">
                  <h3 className="font-bold text-gray-900 text-lg mb-4">Recent Bookings</h3>
                  {allBookings.slice(0, 3).map((b) => (
                    <div key={b.id} className="flex items-center justify-between py-3 border-b last:border-0">
                      <div className="flex items-center gap-3">
                        <div className="bg-blue-100 p-2 rounded-lg"><FaCar className="text-blue-700" /></div>
                        <div>
                          <div className="font-medium text-gray-900">{b.vehicle?.name || 'Vehicle'}</div>
                          <div className="text-gray-500 text-sm">{b.startDate} → {b.endDate}</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="font-bold text-blue-700">${b.totalAmount}</div>
                        <span className={`text-xs px-2 py-1 rounded-full ${statusColor[b.status] || 'bg-gray-100 text-gray-600'}`}>{b.status}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </motion.div>
            )}

            {(activeTab === 'bookings' || activeTab === 'history') && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-white rounded-2xl shadow-md p-6">
                <h3 className="font-bold text-gray-900 text-xl mb-6">{activeTab === 'bookings' ? 'My Bookings' : 'Rental History'}</h3>
                {allBookings.length === 0 ? (
                  <div className="text-center py-12 text-gray-500">
                    <FaCar className="text-4xl mx-auto mb-3 text-gray-300" />
                    <p>No bookings yet</p>
                    <button onClick={() => navigate('/vehicles')} className="btn-primary mt-4">Browse Vehicles</button>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {allBookings.map((b) => (
                      <div key={b.id} className="border rounded-xl p-4 hover:shadow-md transition-shadow">
                        <div className="flex justify-between items-start">
                          <div>
                            <div className="font-bold text-gray-900">{b.vehicle?.name || 'Vehicle'}</div>
                            <div className="text-gray-500 text-sm mt-1">{b.pickupLocation} · {b.startDate} to {b.endDate}</div>
                          </div>
                          <div className="text-right">
                            <div className="font-bold text-blue-700 text-lg">${b.totalAmount}</div>
                            <span className={`text-xs px-2 py-1 rounded-full ${statusColor[b.status] || 'bg-gray-100'}`}>{b.status}</span>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </motion.div>
            )}

            {activeTab === 'payments' && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-white rounded-2xl shadow-md p-6">
                <h3 className="font-bold text-gray-900 text-xl mb-6">Payment History</h3>
                {allBookings.map((b) => (
                  <div key={b.id} className="flex justify-between items-center py-4 border-b last:border-0">
                    <div>
                      <div className="font-medium">{b.vehicle?.name || 'Vehicle'}</div>
                      <div className="text-gray-500 text-sm">{b.createdAt?.split('T')[0]}</div>
                    </div>
                    <div className="text-right">
                      <div className="font-bold text-gray-900">${b.totalAmount}</div>
                      <span className={`text-xs px-2 py-1 rounded-full ${b.paymentStatus === 'paid' ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>{b.paymentStatus}</span>
                    </div>
                  </div>
                ))}
              </motion.div>
            )}

            {activeTab === 'profile' && (
              <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="bg-white rounded-2xl shadow-md p-8">
                <h3 className="font-bold text-gray-900 text-xl mb-6">Profile Settings</h3>
                <form className="space-y-5 max-w-md">
                  {[
                    { label: 'Full Name', value: user?.name || '', field: 'name' },
                    { label: 'Email', value: user?.email || '', field: 'email' },
                    { label: 'Phone', value: user?.phone || '', field: 'phone' },
                  ].map(({ label, value, field }) => (
                    <div key={field}>
                      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
                      <input defaultValue={value} className="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none" />
                    </div>
                  ))}
                  <button type="button" className="btn-primary">Save Changes</button>
                </form>
              </motion.div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
