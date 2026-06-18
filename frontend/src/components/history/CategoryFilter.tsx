import { motion } from 'framer-motion'
import { categoryConfig } from '@/pages/insights/constants'

export type CategoryFilterValue = string | null

interface CategoryFilterProps {
  categories: { category: string; carbonKg: number; count: number }[]
  selected: CategoryFilterValue
  onChange: (value: CategoryFilterValue) => void
}

export default function CategoryFilter({
  categories,
  selected,
  onChange,
}: CategoryFilterProps) {
  // Sort by carbon descending
  const sorted = [...categories].sort((a, b) => b.carbonKg - a.carbonKg)

  return (
    <div className="flex flex-wrap gap-2">
      {/* All button */}
      <motion.button
        onClick={() => onChange(null)}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium transition-all ${
          selected === null
            ? 'bg-emerald-600 text-white shadow-sm'
            : 'bg-cream-50 text-ink-muted hover:bg-cream-100 border border-border-light'
        }`}
      >
        <span>All</span>
        <span className={`text-[10px] px-1 py-0.5 rounded ${
          selected === null ? 'bg-emerald-500' : 'bg-surface'
        }`}>
          {categories.reduce((sum, c) => sum + c.count, 0)}
        </span>
      </motion.button>

      {/* Category buttons */}
      {sorted.map((cat) => {
        const config = categoryConfig[cat.category] ?? categoryConfig.OTHER
        const label = cat.category.charAt(0) + cat.category.slice(1).toLowerCase()
        const isSelected = selected === cat.category

        return (
          <motion.button
            key={cat.category}
            onClick={() => onChange(isSelected ? null : cat.category)}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-medium transition-all ${
              isSelected
                ? 'bg-emerald-600 text-white shadow-sm'
                : 'bg-cream-50 text-ink-muted hover:bg-cream-100 border border-border-light'
            }`}
          >
            <div className={`w-4 h-4 rounded ${isSelected ? 'bg-emerald-500' : config.bg} flex items-center justify-center`}>
              {config.icon}
            </div>
            <span>{label}</span>
            <span className={`text-[10px] px-1 py-0.5 rounded ${
              isSelected ? 'bg-emerald-500' : 'bg-surface'
            }`}>
              {cat.count}
            </span>
          </motion.button>
        )
      })}
    </div>
  )
}
