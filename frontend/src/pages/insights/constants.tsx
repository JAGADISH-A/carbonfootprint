import {
  Car,
  Smartphone,
  TreePine,
  Lightbulb,
  Zap,
  Flame,
  PlaneTakeoff,
  ShoppingBag,
  UtensilsCrossed,
  Droplets,
  Home,
  Leaf,
} from 'lucide-react'

const stagger = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.07 } },
}

const fadeUp = {
  hidden: { opacity: 0, y: 16 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] as const },
  },
}

const categoryConfig: Record<string, { icon: React.ReactNode; color: string; bg: string }> = {
  ELECTRICITY:   { icon: <Zap className="w-4 h-4" />,          color: 'text-amber-600',   bg: 'bg-amber-50' },
  FUEL:          { icon: <Flame className="w-4 h-4" />,        color: 'text-orange-600',  bg: 'bg-orange-50' },
  FLIGHT:        { icon: <PlaneTakeoff className="w-4 h-4" />, color: 'text-blue-600',    bg: 'bg-blue-50' },
  SHOPPING:      { icon: <ShoppingBag className="w-4 h-4" />,  color: 'text-purple-600',  bg: 'bg-purple-50' },
  FOOD:          { icon: <UtensilsCrossed className="w-4 h-4" />, color: 'text-emerald-600', bg: 'bg-emerald-50' },
  TRANSPORT:     { icon: <Leaf className="w-4 h-4" />,         color: 'text-teal-600',    bg: 'bg-teal-50' },
  GAS:           { icon: <Flame className="w-4 h-4" />,        color: 'text-red-600',     bg: 'bg-red-50' },
  WATER:         { icon: <Droplets className="w-4 h-4" />,     color: 'text-cyan-600',    bg: 'bg-cyan-50' },
  ACCOMMODATION: { icon: <Home className="w-4 h-4" />,         color: 'text-indigo-600',  bg: 'bg-indigo-50' },
  OTHER:         { icon: <ShoppingBag className="w-4 h-4" />,  color: 'text-gray-600',    bg: 'bg-gray-50' },
}

interface Equivalent {
  icon: React.ReactNode
  label: string
  sublabel: string
  value: (kg: number) => string
  color: string
  bg: string
}

const equivalents: Equivalent[] = [
  { icon: <Car className="w-5 h-5" />,          label: 'Driving',              sublabel: 'equivalent distance',  value: (kg) => `${Math.round(kg * 4.2)} km`,                    color: 'text-orange-600',  bg: 'bg-orange-50' },
  { icon: <Smartphone className="w-5 h-5" />,   label: 'Phone charges',        sublabel: 'smartphones charged',  value: (kg) => `${Math.round(kg * 120)}`,                        color: 'text-blue-600',   bg: 'bg-blue-50' },
  { icon: <TreePine className="w-5 h-5" />,     label: 'Trees to offset',      sublabel: 'needed per year',      value: (kg) => `${Math.max(1, Math.ceil(kg / 21))}`,             color: 'text-emerald-600',bg: 'bg-emerald-50' },
  { icon: <Lightbulb className="w-5 h-5" />,    label: 'LED bulb days',        sublabel: 'of continuous light',  value: (kg) => `${Math.round(kg * 14)}`,                         color: 'text-amber-600',  bg: 'bg-amber-50' },
]

export { equivalents, categoryConfig, stagger, fadeUp }
export type { Equivalent }
