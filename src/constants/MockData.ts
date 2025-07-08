// Mock data for testing promotion functionality
// Generic structure for any company using this SDK

export interface Promotion {
  id: string;
  title: string;
  subtitle?: string;
  code?: string;
  createdAt: string;
}

export interface PromotionData {
  merchantName: string;
  rewardText?: string;
  featuredPromotion?: Promotion;
  promotions: Promotion[];
}

export const MOCK_PROMOTION_DATA: PromotionData = {
  merchantName: "Sample Store",
  rewardText: "2% Cashback",
  featuredPromotion: {
    id: "featured-1",
    title: "Special Sale - Up to 65% off",
    subtitle: "Limited time offer",
    code: "",
    createdAt: "2025-07-07T22:00:00Z"
  },
  promotions: [
    {
      id: "promo-1",
      title: "New Sale Styles - Up to 50% off",
      subtitle: "Fresh arrivals",
      code: "",
      createdAt: "2022-12-02T08:00:00Z"
    },
    {
      id: "promo-2",
      title: "Youth Collection - Up to 50% off",
      subtitle: "Kids & teens",
      code: "",
      createdAt: "2022-06-15T07:00:00Z"
    },
    {
      id: "promo-3",
      title: "Staff Favorites - 20% off",
      subtitle: "Curated selection",
      code: "FAVES",
      createdAt: "2024-04-23T20:48:20Z"
    },
    {
      id: "promo-4",
      title: "Summer Clearance - Up to 50% off",
      subtitle: "End of season",
      code: "",
      createdAt: "2025-07-01T16:00:00Z"
    }
  ]
};

// Helper function to determine if promotion is "NEW!"
export const isPromotionNew = (createdAt: string): boolean => {
  const createdDate = new Date(createdAt);
  const now = new Date();
  const twoDaysAgo = new Date(now.getTime() - (2 * 24 * 60 * 60 * 1000));
  return createdDate >= twoDaysAgo;
};