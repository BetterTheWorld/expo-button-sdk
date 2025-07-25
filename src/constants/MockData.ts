// Mock data for testing promotion functionality
// Updated structure to match real API data format

export interface Promotion {
  id: string;
  couponCode: string;
  description: string;
  endsAt: string;
  startsAt: string;
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
    id: "9482",
    couponCode: "SHOP20",
    description: "THIS WEEK! Take 20% off Sitewide 4% Cashback",
    endsAt: "2025-07-21T03:59:00Z",
    startsAt: "2025-07-11T17:00:00Z",
  },
  promotions: [
    {
      id: "1001",
      couponCode: "SAVE15",
      description: "TODAY! New Sale Styles - Up to 50% off 3% Cashback",
      endsAt: "2025-07-18T23:59:00Z",
      startsAt: "2025-07-17T09:00:00Z",
    },
    {
      id: "1002",
      couponCode: "YOUTH50",
      description: "Youth Collection - Up to 50% off 2% Cashback",
      endsAt: "2025-07-25T23:59:00Z",
      startsAt: "2025-07-15T09:00:00Z",
    },
    {
      id: "1003",
      couponCode: "FAVES",
      description: "Staff Favorites - 20% off 5% Cashback",
      endsAt: "2025-07-22T23:59:00Z",
      startsAt: "2025-07-16T09:00:00Z",
    },
    {
      id: "1004",
      couponCode: "SUMMER",
      description: "Summer Clearance - Up to 50% off 2% Cashback",
      endsAt: "2025-07-30T23:59:00Z",
      startsAt: "2025-07-01T09:00:00Z",
    },
    {
      id: "1005",
      couponCode: "FLASH25",
      description: "Flash Sale - 25% off everything 4% Cashback",
      endsAt: "2025-07-19T12:00:00Z",
      startsAt: "2025-07-17T09:00:00Z",
    },
    {
      id: "1006",
      couponCode: "WEEKEND",
      description: "Weekend Special - Buy one get one 50% off 3% Cashback",
      endsAt: "2025-07-20T23:59:00Z",
      startsAt: "2025-07-17T09:00:00Z",
    },
    {
      id: "1007",
      couponCode: "NEWBIE",
      description: "New Customer - 30% off first order 5% Cashback",
      endsAt: "2025-07-31T23:59:00Z",
      startsAt: "2025-07-01T09:00:00Z",
    },
    {
      id: "1008",
      couponCode: "LOYALTY",
      description: "Loyalty Members - Extra 15% off 6% Cashback",
      endsAt: "2025-07-28T23:59:00Z",
      startsAt: "2025-07-10T09:00:00Z",
    },
  ],
};

// Helper function to determine if promotion is "NEW!" based on startsAt date
export const isPromotionNew = (startsAt: string): boolean => {
  const startDate = new Date(startsAt);
  const now = new Date();
  const twoDaysAgo = new Date(now.getTime() - 2 * 24 * 60 * 60 * 1000);
  return startDate >= twoDaysAgo;
};
