export interface ExitConfirmationConfig {
  enabled: boolean;
  title?: string;
  message?: string;
  stayButtonLabel?: string;
  leaveButtonLabel?: string;
}

export interface Promotion {
  id: string;
  description: string;
  couponCode?: string;
  startsAt: string;
  endsAt: string;
}

export interface PromotionData {
  merchantName: string;
  rewardText?: string;
  featuredPromotion?: Promotion;
  promotions: Promotion[];
}

export interface StartPurchasePathOptions {
  url: string;
  token: string;
  headerTitle?: string;
  headerSubtitle?: string;
  headerTitleColor?: string;
  headerSubtitleColor?: string;
  headerBackgroundColor?: string;
  headerTintColor?: string;
  footerBackgroundColor?: string;
  footerTintColor?: string;
  exitConfirmation?: ExitConfirmationConfig;
  promotionData?: PromotionData;
  onPromotionClick?: (
    promotionId: string
  ) => Promise<{ url: string; token: string }>;
  closeOnPromotionClick?: boolean; // Default: true - whether to close current instance when promotion is clicked
  promotionBadgeLabel?: string; // Label for the promotion badge (e.g., "Offers", "Deals")
  promotionListTitle?: string; // Title for the promotion list modal (e.g., "Promotions", "Available Offers")
  promotionBadgeFontSize?: number; // Font size for the promotion badge text (e.g., 11, 12, 13)
}

export type Identifier = string;

export interface ButtonSDKStatus {
  isInitialized: boolean;
}
