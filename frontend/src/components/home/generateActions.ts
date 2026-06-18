import type { CarbonAnalyticsResponse, CarbonInsightResponse, AICarbonCoachResponse } from '@/types/activity'

export interface ActionRecommendation {
  id: string
  type: 'opportunity' | 'quick_win' | 'habit' | 'challenge' | 'upload'
  title: string
  description: string
  estimatedSavingKg: number | null
  confidence: 'high' | 'medium' | 'low'
  whyItMatters: string
  ctaLabel: string
  ctaLink: string
  icon: string
  priority: number // 1 = highest
}

const CATEGORY_ACTIONS: Record<string, Omit<ActionRecommendation, 'id' | 'estimatedSavingKg' | 'priority'>[]> = {
  FOOD: [
    {
      type: 'opportunity',
      title: 'Try plant-based meals 2x per week',
      description: 'Your food emissions are significant. Swapping meat for plant-based options 2 days a week can make a real difference.',
      confidence: 'high',
      whyItMatters: 'Food accounts for a large share of personal emissions. Plant-based meals typically produce 5-10x less carbon.',
      ctaLabel: 'See meal ideas →',
      ctaLink: '/coach',
      icon: '🥦',
    },
    {
      type: 'quick_win',
      title: 'Reduce food waste this week',
      description: 'Plan meals before shopping. Wasted food means wasted emissions from production, transport, and disposal.',
      confidence: 'medium',
      whyItMatters: 'About 8-10% of global emissions come from food waste. Even small reductions help.',
      ctaLabel: 'Get tips →',
      ctaLink: '/coach',
      icon: '🍎',
    },
  ],
  FUEL: [
    {
      type: 'opportunity',
      title: 'Combine errands into fewer trips',
      description: 'Planning your route to hit multiple stops saves fuel and cuts emissions significantly.',
      confidence: 'high',
      whyItMatters: 'Short trips are the least fuel-efficient. Combining errands can reduce fuel use by 20-30%.',
      ctaLabel: 'Plan smarter →',
      ctaLink: '/coach',
      icon: '🚗',
    },
    {
      type: 'quick_win',
      title: 'Check your tire pressure',
      description: 'Under-inflated tires increase fuel consumption by 3-5%. A quick check could save you money and carbon.',
      confidence: 'medium',
      whyItMatters: 'Proper tire maintenance is one of the easiest wins for reducing fuel emissions.',
      ctaLabel: 'Learn more →',
      ctaLink: '/coach',
      icon: '🛞',
    },
  ],
  TRANSPORT: [
    {
      type: 'opportunity',
      title: 'Consider cycling for short commutes',
      description: 'Trips under 5km are perfect for cycling. Zero emissions, free exercise, and often faster in traffic.',
      confidence: 'high',
      whyItMatters: 'Transport is one of the largest emission sources. Short trips are the easiest to replace.',
      ctaLabel: 'Find routes →',
      ctaLink: '/coach',
      icon: '🚲',
    },
    {
      type: 'quick_win',
      title: 'Use public transit once a week',
      description: 'Replacing one car trip per week with public transit can cut your transport emissions noticeably.',
      confidence: 'medium',
      whyItMatters: 'Public transit produces 4-5x less carbon per passenger mile than solo driving.',
      ctaLabel: 'Check options →',
      ctaLink: '/coach',
      icon: '🚌',
    },
  ],
  ELECTRICITY: [
    {
      type: 'opportunity',
      title: 'Switch to LED bulbs in high-use rooms',
      description: 'LED bulbs use 75% less energy and last 25x longer. Focus on rooms you use most.',
      confidence: 'high',
      whyItMatters: 'Lighting accounts for 15% of home electricity. LEDs are the fastest payback.',
      ctaLabel: 'Calculate savings →',
      ctaLink: '/coach',
      icon: '💡',
    },
    {
      type: 'quick_win',
      title: 'Unplug idle electronics tonight',
      description: 'Standby power accounts for 5-10% of home electricity. Unplug chargers, adapters, and appliances when not in use.',
      confidence: 'medium',
      whyItMatters: 'Phantom loads add up. This is literally free savings with zero lifestyle change.',
      ctaLabel: 'See what to unplug →',
      ctaLink: '/coach',
      icon: '🔌',
    },
  ],
  FLIGHT: [
    {
      type: 'opportunity',
      title: 'Offset your next flight',
      description: 'Flights have high emissions. Quality carbon offsets can neutralize the impact while you work on reduction.',
      confidence: 'medium',
      whyItMatters: 'A single round-trip flight can equal months of ground transport emissions.',
      ctaLabel: 'Explore offsets →',
      ctaLink: '/coach',
      icon: '✈️',
    },
  ],
  SHOPPING: [
    {
      type: 'quick_win',
      title: 'Choose second-hand first',
      description: 'Before buying new, check if you can find it used. Avoiding new production saves significant emissions.',
      confidence: 'medium',
      whyItMatters: 'Manufacturing accounts for most product emissions. Buying used eliminates production impact.',
      ctaLabel: 'Get ideas →',
      ctaLink: '/coach',
      icon: '♻️',
    },
  ],
  WATER: [
    {
      type: 'quick_win',
      title: 'Fix that leaky faucet',
      description: 'A dripping faucet wastes thousands of liters per year — and the energy used to treat and pump that water.',
      confidence: 'high',
      whyItMatters: 'Water treatment is energy-intensive. Every liter saved reduces emissions.',
      ctaLabel: 'Learn how →',
      ctaLink: '/coach',
      icon: '🚰',
    },
  ],
  GAS: [
    {
      type: 'quick_win',
      title: 'Lower your thermostat by 1°C',
      description: 'A small temperature drop saves 10% on heating energy with barely noticeable comfort change.',
      confidence: 'high',
      whyItMatters: 'Heating is often the largest home energy use. Small adjustments compound over months.',
      ctaLabel: 'See more tips →',
      ctaLink: '/coach',
      icon: '🌡️',
    },
  ],
}

function generateFromAnalytics(
  analytics: CarbonAnalyticsResponse,
  insights: CarbonInsightResponse | null,
  coach: AICarbonCoachResponse | null
): ActionRecommendation[] {
  const actions: ActionRecommendation[] = []
  const categories = analytics.categoryTotals ?? []
  const totalKg = Number(analytics.totalCarbonKg ?? 0)
  const avgDaily = Number(analytics.averageDailyKg ?? 0)
  const activityCount = analytics.activityCount ?? 0

  // ── If no data, prompt upload ───────────────────────────────────────
  if (activityCount === 0) {
    actions.push({
      id: 'upload-first',
      type: 'upload',
      title: 'Upload your first receipt',
      description: 'I need some data to help you. Upload a receipt, bank statement, or email confirmation to get started.',
      estimatedSavingKg: null,
      confidence: 'high',
      whyItMatters: 'You cannot reduce what you do not measure. One receipt is all it takes to start.',
      ctaLabel: 'Upload now →',
      ctaLink: '/upload',
      icon: '📄',
      priority: 1,
    })
    return actions
  }

  // ── Biggest opportunity: top category ───────────────────────────────
  if (categories.length > 0) {
    const topCat = categories[0]
    const catActions = CATEGORY_ACTIONS[topCat.category] ?? CATEGORY_ACTIONS.OTHER ?? []
    if (catActions.length > 0) {
      const primary = catActions[0]
      // Estimate saving as 15-25% of that category's emissions
      const estimatedSaving = Number(topCat.carbonKg ?? 0) * 0.2
      actions.push({
        ...primary,
        id: `opportunity-${topCat.category}`,
        estimatedSavingKg: estimatedSaving,
        priority: 1,
      })
    }

    // Add secondary action if available
    if (catActions.length > 1 && categories.length > 0) {
      const secondary = catActions[1]
      const estimatedSaving = Number(topCat.carbonKg ?? 0) * 0.1
      actions.push({
        ...secondary,
        id: `quickwin-${topCat.category}`,
        estimatedSavingKg: estimatedSaving,
        priority: 2,
      })
    }
  }

  // ── Second biggest category quick win ───────────────────────────────
  if (categories.length > 1) {
    const secondCat = categories[1]
    const catActions = CATEGORY_ACTIONS[secondCat.category] ?? CATEGORY_ACTIONS.OTHER ?? []
    if (catActions.length > 0) {
      const action = catActions[0]
      const estimatedSaving = Number(secondCat.carbonKg ?? 0) * 0.15
      actions.push({
        ...action,
        id: `category-${secondCat.category}`,
        estimatedSavingKg: estimatedSaving,
        priority: 3,
      })
    }
  }

  // ── Habit-based: high daily average ─────────────────────────────────
  if (avgDaily > 5 && actions.length < 4) {
    actions.push({
      id: 'habit-daily-high',
      type: 'habit',
      title: 'Set a daily carbon budget',
      description: `You're averaging ${avgDaily.toFixed(1)} kg/day. Setting a target of ${(avgDaily * 0.8).toFixed(1)} kg/day could save ${(avgDaily * 0.2 * 30).toFixed(0)} kg this month.`,
      estimatedSavingKg: avgDaily * 0.2 * 30,
      confidence: 'medium',
      whyItMatters: 'Tracking against a target makes reduction tangible. Small daily choices add up to big monthly savings.',
      ctaLabel: 'Set my target →',
      ctaLink: '/coach',
      icon: '🎯',
      priority: 4,
    })
  }

  // ── AI Coach recommendation ─────────────────────────────────────────
  if (coach?.recommendations?.[0] && actions.length < 4) {
    actions.push({
      id: 'ai-recommendation',
      type: 'challenge',
      title: coach.recommendations[0],
      description: 'Your AI coach analyzed your patterns and found this opportunity.',
      estimatedSavingKg: totalKg * 0.05,
      confidence: 'medium',
      whyItMatters: 'AI-powered analysis considers your specific patterns, not generic advice.',
      ctaLabel: 'See all recommendations →',
      ctaLink: '/coach',
      icon: '🤖',
      priority: 5,
    })
  }

  // ── Weekly challenge from coach ─────────────────────────────────────
  if (coach?.weeklyChallenge && actions.length < 4) {
    actions.push({
      id: 'weekly-challenge',
      type: 'challenge',
      title: coach.weeklyChallenge,
      description: 'A fun challenge to try this week based on your profile.',
      estimatedSavingKg: totalKg * 0.03,
      confidence: 'low',
      whyItMatters: 'Challenges make sustainability fun. Even attempting one builds lasting habits.',
      ctaLabel: 'Accept challenge →',
      ctaLink: '/coach',
      icon: '🏆',
      priority: 6,
    })
  }

  // ── Insights-based warning ──────────────────────────────────────────
  if (insights?.warnings?.[0] && actions.length < 4) {
    actions.push({
      id: 'insight-warning',
      type: 'opportunity',
      title: insights.warnings[0],
      description: 'This pattern was detected in your recent activity.',
      estimatedSavingKg: totalKg * 0.08,
      confidence: 'high',
      whyItMatters: 'Early intervention prevents small issues from becoming habits.',
      ctaLabel: 'Address this →',
      ctaLink: '/coach',
      icon: '⚠️',
      priority: 7,
    })
  }

  return actions.sort((a, b) => a.priority - b.priority).slice(0, 4)
}

export function useActionRecommendations(
  analytics: CarbonAnalyticsResponse | null,
  insights: CarbonInsightResponse | null,
  coach: AICarbonCoachResponse | null
): ActionRecommendation[] {
  if (!analytics) return []

  // Sort categories by carbonKg descending for priority
  const sortedAnalytics = {
    ...analytics,
    categoryTotals: [...(analytics.categoryTotals ?? [])].sort(
      (a, b) => Number(b.carbonKg ?? 0) - Number(a.carbonKg ?? 0)
    ),
  }

  return generateFromAnalytics(sortedAnalytics, insights, coach)
}
