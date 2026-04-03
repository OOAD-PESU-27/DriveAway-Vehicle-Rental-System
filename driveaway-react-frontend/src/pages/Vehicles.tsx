import { useState, useMemo } from 'react';
import { FaSearch } from 'react-icons/fa';
import VehicleCard from '../components/vehicles/VehicleCard';
import VehicleFilter from '../components/vehicles/VehicleFilter';
import { mockVehicles } from '../data/mockData';
import { motion } from 'framer-motion';

const defaultFilters = { type: 'all', minPrice: 0, maxPrice: 500, transmission: 'all', sortBy: 'default' };

export default function Vehicles() {
  const [filters, setFilters] = useState(defaultFilters);
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    let result = mockVehicles.filter((v) => {
      const matchType = filters.type === 'all' || v.type === filters.type;
      const matchPrice = v.pricePerDay <= filters.maxPrice;
      const matchTrans = filters.transmission === 'all' || v.transmission === filters.transmission;
      const matchSearch = !search || v.name.toLowerCase().includes(search.toLowerCase()) || v.brand.toLowerCase().includes(search.toLowerCase());
      return matchType && matchPrice && matchTrans && matchSearch;
    });
    if (filters.sortBy === 'price-asc') result = [...result].sort((a, b) => a.pricePerDay - b.pricePerDay);
    else if (filters.sortBy === 'price-desc') result = [...result].sort((a, b) => b.pricePerDay - a.pricePerDay);
    else if (filters.sortBy === 'rating') result = [...result].sort((a, b) => b.rating - a.rating);
    return result;
  }, [filters, search]);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="bg-gradient-to-r from-blue-900 to-blue-700 py-16">
        <div className="max-w-7xl mx-auto px-4">
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
            <h1 className="text-4xl font-bold text-white mb-4">Our Fleet</h1>
            <p className="text-blue-200 text-lg">Find your perfect vehicle from our premium collection</p>
          </motion.div>
          <div className="mt-6 max-w-xl">
            <div className="relative">
              <FaSearch className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" />
              <input type="text" value={search} onChange={(e) => setSearch(e.target.value)}
                placeholder="Search by brand or model..."
                className="w-full pl-12 pr-4 py-4 rounded-2xl border-0 focus:ring-2 focus:ring-orange-400 outline-none text-gray-900 shadow-lg" />
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-10">
        <div className="flex flex-col lg:flex-row gap-8">
          <div className="lg:w-72 flex-shrink-0">
            <VehicleFilter filters={filters} onFilterChange={setFilters} onReset={() => setFilters(defaultFilters)} />
          </div>

          <div className="flex-1">
            <div className="flex justify-between items-center mb-6">
              <p className="text-gray-600"><span className="font-bold text-gray-900">{filtered.length}</span> vehicles found</p>
            </div>
            {filtered.length === 0 ? (
              <div className="text-center py-20 text-gray-500">
                <p className="text-5xl mb-4">🚗</p>
                <p className="text-xl font-semibold">No vehicles found</p>
                <p className="mt-2">Try adjusting your filters</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                {filtered.map((vehicle, i) => <VehicleCard key={vehicle.id} vehicle={vehicle} index={i} />)}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
