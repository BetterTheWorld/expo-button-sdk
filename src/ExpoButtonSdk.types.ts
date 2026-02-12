export interface ExitConfirmationConfig {
  enabled: boolean;
  title?: string;
  message?: string;
  stayButtonLabel?: string;
  leaveButtonLabel?: string;
  /** Color of exit confirmation title.
   * @default "#1C1C1C" */
  titleColor?: string;
  /** Text color of stay button.
   * @default "#FFFFFF" */
  stayButtonTextColor?: string;
  /** Background color of stay button.
   * @default "#074a7b" */
  stayButtonBackgroundColor?: string;
  /** Text color of leave button.
   * @default "#677080" */
  leaveButtonTextColor?: string;
  /** Background color of leave button.
   * @default "#FFFFFF" */
  leaveButtonBackgroundColor?: string;
  /** Border/line color for buttons.
   * @default "#D3D9E0" */
  buttonBorderColor?: string;
  /** Color of exit confirmation message/body text.
   * @default "#282B30" (Android) / rgba(40,42,45) (iOS) */
  messageColor?: string;
  /** Font size for the title.
   * @default 20 */
  titleFontSize?: number;
  /** Font size for the message/body text.
   * @default 14 */
  messageFontSize?: number;
  /** Font size for the button labels.
   * @default 14 (iOS) / 12 (Android) */
  buttonFontSize?: number;
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

export interface AndroidAspectRatio {
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
  /** Font family for the earn text label. Must be a font bundled in the host app.
   * Example: "OpenSans-SemiBold"
   * @default system font */
  earnTextFontFamily?: string;
  /** Font size for the earn text label.
   * @default 12 */
  earnTextFontSize?: number;
  /** Font weight for the earn text label. Uses CSS-style numeric values.
   * "normal"=400, "bold"=700, or numeric "100"-"900".
   * Ignored if earnTextFontFamily already includes weight (e.g. "OpenSans-SemiBold").
   * @default "600" */
  earnTextFontWeight?: string;
  /** Line height for the earn text label.
   * @default auto */
  earnTextLineHeight?: number;
  /** Bottom margin of the earn text label from the PIP window bottom edge.
   * @default 28 (iOS points) / 8 (Android dp) */
  earnTextMargin?: number;
  androidAspectRatio?: AndroidAspectRatio;
  /** Inset/padding of overlay buttons (close, chevron) from PIP window edges.
   * @default 10 (iOS points) / 8 (Android dp) */
  pipOverlayInset?: number;
  /** Size of the close (X) button in the PIP window.
   * @default 20 (iOS points) / 20 (Android dp) */
  pipCloseButtonSize?: number;
  /** Width of the chevron icon in the PIP window.
   * @default 15 (iOS points) / 20 (Android dp) */
  pipChevronSize?: number;
  /** Height of the chevron icon in the PIP window.
   * If not set, defaults to pipChevronSize * 10/18.
   * @default auto */
  pipChevronHeight?: number;
  /** Extra stroke width for the chevron icon to make it bolder.
   * 0 = original FA6 Regular weight. Higher values = thicker.
   * @default 0 */
  pipChevronStrokeWidth?: number;
  /** Extra stroke width for the close (X) icon to make it bolder.
   * 0 = original FA6 Regular weight. Higher values = thicker.
   * @default 0 */
  pipCloseStrokeWidth?: number;
  /**
   * When true, tapping anywhere on the PiP floating window restores the browser.
   * When false, only tapping the chevron restores and only the X closes.
   * The rest of the PiP surface is drag-only.
   * @default true
   */
  pipTapToRestore?: boolean;
  /**
   * Android only: When true, PiP will hide when app goes to background
   * and restore when app comes back to foreground.
   * @platform android
   * @default false
   */
  hideOnAppBackground?: boolean;
  /**
   * Android only: When true, uses native Android PiP API (enterPictureInPictureMode).
   * When false, uses a simulated PiP with a draggable floating overlay within the app,
   * similar to the iOS implementation.
   * @platform android
   * @default true
   */
  useNativePip?: boolean;
}

export interface AnimationConfig {
  pictureInPictureMode?: boolean;
  minimizedScale?: number;
  minimizedPosition?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  pictureInPicture?: PictureInPictureConfig;
}

export interface CoverImageConfig {
  uri?: string;
  source?: string;
  base64?: string;
  scaleType?: 'cover' | 'contain' | 'center' | 'stretch';
  backgroundColor?: string;
  padding?: number;
  /** Explicit width for the image inside the PIP window. If not set, fills available space minus padding. */
  width?: number;
  /** Explicit height for the image inside the PIP window. If not set, fills available space minus padding. */
  height?: number;
}

export interface StartPurchasePathOptions {
  url: string;
  token: string;
  /** General font family applied to exit confirmation text and PiP earn text fallback.
   * Must be a font bundled in the host app.
   * Example: "OpenSans-SemiBold" */
  fontFamily?: string;
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
  onClose?: () => void;
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
    
    // Upper bound only — small sizes are valid for PiP windows
    if (config.size.width > 1000) {
      errors.push('pictureInPicture.size.width must be less than 1000px');
    }
    if (config.size.height > 1000) {
      errors.push('pictureInPicture.size.height must be less than 1000px');
    }
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
}
