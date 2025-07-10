export interface ExitConfirmationConfig {
  enabled: boolean;
  title?: string;
  message?: string;
  stayButtonLabel?: string;
  leaveButtonLabel?: string;
}

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
  onPromotionClick?: (promotionId: string) => Promise<{ url: string; token: string }>;
  closeOnPromotionClick?: boolean; // Default: true - whether to close current instance when promotion is clicked
  promotionBadgeLabel?: string; // Label for the promotion badge (e.g., "Offers", "Deals")
  promotionListTitle?: string; // Title for the promotion list modal (e.g., "Promotions", "Available Offers")
}

export type Identifier = string;

export interface ButtonSDKStatus {
  isInitialized: boolean;
}
