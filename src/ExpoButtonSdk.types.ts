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

export interface HeaderButton {
  icon?: string;
  title?: string;
  action: string;
}

export interface PictureInPicturePosition {
  x: number;
  y: number;
}

export interface PictureInPictureSize {
  width: number;
  height: number;
}

export interface PictureInPictureConfig {
  enabled: boolean;
  position?: PictureInPicturePosition;
  size?: PictureInPictureSize;
  chevronColor?: string;
  earnText?: string;
  earnTextColor?: string;
  earnTextBackgroundColor?: string;
}

export interface AnimationConfig {
  pictureInPictureMode?: boolean;
  minimizedScale?: number;
  minimizedPosition?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  pictureInPicture?: PictureInPictureConfig;
}

export interface CoverImageConfig {
  uri?: string;
  source?: string; // For local assets in iOS bundle
  base64?: string;
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
  headerLeftIcon?: string;
  headerRightButtons?: HeaderButton[];
  footerBackgroundColor?: string;
  footerTintColor?: string;
  exitConfirmation?: ExitConfirmationConfig;
  promotionData?: PromotionData;
  onPromotionClick?: (
    promotionId: string
  ) => Promise<{ url: string; token: string }>;
  onHeaderButtonClick?: (action: string) => void;
  closeOnPromotionClick?: boolean;
  promotionBadgeLabel?: string;
  promotionListTitle?: string;
  promotionBadgeFontSize?: number;
  animationConfig?: AnimationConfig;
  coverImage?: CoverImageConfig;
}

export type Identifier = string;

export interface ButtonSDKStatus {
  isInitialized: boolean;
}

export interface PictureInPictureValidationResult {
  isValid: boolean;
  errors: string[];
}

export function validatePictureInPictureConfig(config?: PictureInPictureConfig): PictureInPictureValidationResult {
  const errors: string[] = [];
  
  if (!config) {
    return { isValid: true, errors: [] };
  }
  
  if (typeof config.enabled !== 'boolean') {
    errors.push('pictureInPicture.enabled must be a boolean');
  }
  
  if (config.position) {
    if (typeof config.position.x !== 'number' || config.position.x < 0) {
      errors.push('pictureInPicture.position.x must be a positive number');
    }
    if (typeof config.position.y !== 'number' || config.position.y < 0) {
      errors.push('pictureInPicture.position.y must be a positive number');
    }
  }
  
  if (config.size) {
    if (typeof config.size.width !== 'number' || config.size.width <= 0) {
      errors.push('pictureInPicture.size.width must be a positive number');
    }
    if (typeof config.size.height !== 'number' || config.size.height <= 0) {
      errors.push('pictureInPicture.size.height must be a positive number');
    }
    
    // Reasonable size limits
    if (config.size.width > 1000) {
      errors.push('pictureInPicture.size.width must be less than 1000px');
    }
    if (config.size.height > 1000) {
      errors.push('pictureInPicture.size.height must be less than 1000px');
    }
    if (config.size.width < 100) {
      errors.push('pictureInPicture.size.width must be at least 100px');
    }
    if (config.size.height < 100) {
      errors.push('pictureInPicture.size.height must be at least 100px');
    }
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
}
