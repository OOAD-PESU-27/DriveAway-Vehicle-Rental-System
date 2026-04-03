import { FaFilter, FaTimes } from 'react-icons/fa';

interface FilterState {
  type: string;
  minPrice: number;
  maxPrice: number;
  transmission: string;
  sortBy: string;
}

interface Props {
  filters: FilterState;
  onFilterChange: (filters: FilterState) => void;
  onReset: () => void;
}

const vehicleTypes = ['all', 'sedan', 'suv', 'luxury', 'economy', 'truck', 'van'];

export default function VehicleFilter({ filters, onFilterChange, onReset }: Props) {
  const update = (key: keyof FilterState, value: string | number) =>
    onFilterChange({ ...filters, [key]: value });

  return (
    <div className="bg-white rounded-2xl shadow-md p-6">
      <div className="flex justify-between items-center mb-6">
        <h3 className="font-bold text-gray-900 text-lg flex items-center gap-2"><FaFilter className="text-blue-700" />Filters</h3>
        <button onClick={onReset} className="text-sm text-red-500 hover:text-red-700 flex items-center gap-1"><FaTimes />Reset</button>
      </div>

      <div className="space-y-6">
        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-3">Vehicle Type</label>
          <div className="flex flex-wrap gap-2">
            {vehicleTypes.map((type) => (
              <button key={type} onClick={() => update('type', type)}
                className={`px-3 py-1.5 rounded-full text-sm font-medium capitalize transition-colors ${filters.type === type ? 'bg-blue-700 text-white' : 'bg-gray-100 text-gray-600 hover:bg-blue-50'}`}>
                {type === 'all' ? 'All Types' : type}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-3">Price Range: ${filters.minPrice} - ${filters.maxPrice}/day</label>
          <input type="range" min="0" max="500" value={filters.maxPrice}
            onChange={(e) => update('maxPrice', parseInt(e.target.value))}
            className="w-full accent-blue-700" />
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-3">Transmission</label>
          <div className="flex gap-2">
            {['all', 'automatic', 'manual'].map((t) => (
              <button key={t} onClick={() => update('transmission', t)}
                className={`flex-1 py-2 rounded-xl text-sm font-medium capitalize transition-colors ${filters.transmission === t ? 'bg-blue-700 text-white' : 'bg-gray-100 text-gray-600 hover:bg-blue-50'}`}>
                {t === 'all' ? 'All' : t}
              </button>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-gray-700 mb-3">Sort By</label>
          <select value={filters.sortBy} onChange={(e) => update('sortBy', e.target.value)}
            className="w-full border border-gray-200 rounded-xl px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 outline-none">
            <option value="default">Default</option>
            <option value="price-asc">Price: Low to High</option>
            <option value="price-desc">Price: High to Low</option>
            <option value="rating">Highest Rated</option>
          </select>
        </div>
      </div>
    </div>
  );
}
