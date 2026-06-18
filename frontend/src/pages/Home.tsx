import {
  WelcomeHero,
  CarbonSummary,
  UploadCTA,
  CoachPreview,
  QuickInsights,
  RecentActivity,
  SustainabilityTip,
} from '@/components/home'

export default function Home() {
  return (
    <div className="space-y-6">
      {/* 1. Welcome Hero — full width */}
      <WelcomeHero />

      {/* 2. Carbon Summary + Upload CTA + Coach Preview */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* Left: Carbon Summary (big card) */}
        <div className="lg:col-span-1">
          <CarbonSummary />
        </div>

        {/* Right: Upload CTA + Coach Preview stacked */}
        <div className="lg:col-span-2 flex flex-col gap-5">
          <UploadCTA />
          <CoachPreview />
        </div>
      </div>

      {/* 3. Quick Insights — 3 cards */}
      <QuickInsights />

      {/* 4. Recent Activity — timeline */}
      <RecentActivity />

      {/* 5. Sustainability Tip — green banner */}
      <SustainabilityTip />
    </div>
  )
}
