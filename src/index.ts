import { Identifier, StartPurchasePathOptions, validatePictureInPictureConfig } from "./ExpoButtonSdk.types";

export type {
  ExitConfirmationConfig,
  Promotion,
  PromotionData,
  HeaderButton,
  PictureInPicturePosition,
  PictureInPictureSize,
  AndroidAspectRatio,
  PictureInPictureConfig,
  AnimationConfig,
  CoverImageConfig,
  StartPurchasePathOptions,
  Identifier,
  ButtonSDKStatus,
  PictureInPictureValidationResult,
} from "./ExpoButtonSdk.types";
import ExpoButtonSdkModule from "./ExpoButtonSdkModule";

// Store the current listener to avoid accumulation
let currentPromotionListener: any = null;
let currentHeaderButtonListener: any = null;
let currentCloseListener: any = null;

export async function startPurchasePath(options: StartPurchasePathOptions) {
  // Validate PictureInPicture config first
  if (options.animationConfig?.pictureInPicture) {
    const validation = validatePictureInPictureConfig(options.animationConfig.pictureInPicture);
    if (!validation.isValid) {
      throw new Error(`Invalid PictureInPicture configuration: ${validation.errors.join(', ')}`);
    }
  }

  // Clean up previous listeners if they exist
  if (currentPromotionListener) {
    currentPromotionListener.remove();
    currentPromotionListener = null;
  }
  if (currentHeaderButtonListener) {
    currentHeaderButtonListener.remove();
    currentHeaderButtonListener = null;
  }
  if (currentCloseListener) {
    currentCloseListener.remove();
    currentCloseListener = null;
  }

  // don't send promotionData if empty
  const sanitizedOptions = { ...options };
  if (
    options.promotionData &&
    !Array.isArray(options.promotionData.promotions)
  ) {
    console.warn(
      "promotionData.promotions is not an array, removing promotionData to prevent crash"
    );
    sanitizedOptions.promotionData = undefined;
  }

  // Set up new event listener if callback is provided
  if (options.onPromotionClick) {
    currentPromotionListener = ExpoButtonSdkModule.addListener(
      "onPromotionClick",
      async (event: {
        promotionId: string;
        closeOnPromotionClick: boolean;
      }) => {
        try {
          const result = await options.onPromotionClick!(event.promotionId);

          // Note: Browser dismiss is handled automatically on native side
          // based on closeOnPromotionClick setting

          // Start new purchase path
          await startPurchasePath({
            ...options,
            url: result.url,
            token: result.token,
          });
        } catch (error) {
          console.error("Error handling promotion click:", error);
        }
      }
    );
  }

  // Set up header button listener if callback is provided
  if (options.onHeaderButtonClick) {
    currentHeaderButtonListener = ExpoButtonSdkModule.addListener(
      "onHeaderButtonClick",
      (event: { action: string }) => {
        try {
          options.onHeaderButtonClick!(event.action);
        } catch (error) {
          console.error("Error handling header button click:", error);
        }
      }
    );
  }

  // Set up close listener if callback is provided
  if (options.onClose) {
    currentCloseListener = ExpoButtonSdkModule.addListener(
      "onClose",
      () => {
        try {
          options.onClose!();
        } catch (error) {
          console.error("Error handling close:", error);
        }
      }
    );
  }

  // Remove non-serializable properties before passing to native module
  const { onPromotionClick, onHeaderButtonClick, onClose, ...tempOptions } = sanitizedOptions;

  // Deep clean undefined values that can't be serialized
  const cleanOptions = (obj: any): any => {
    if (obj === null || obj === undefined) return null;
    if (Array.isArray(obj)) return obj.map(cleanOptions);
    if (typeof obj === "object") {
      const cleaned: any = {};
      for (const [key, value] of Object.entries(obj)) {
        if (value !== undefined) {
          cleaned[key] = cleanOptions(value);
        }
      }
      return cleaned;
    }
    return obj;
  };

  const nativeOptions = cleanOptions(tempOptions);

  return await ExpoButtonSdkModule.startPurchasePath(nativeOptions);
}

export function clearAllData() {
  return ExpoButtonSdkModule.clearAllData();
}

export function setIdentifier(id: Identifier) {
  return ExpoButtonSdkModule.setIdentifier(id);
}

export async function initializeSDK(): Promise<boolean> {
  return await ExpoButtonSdkModule.initializeSDK();
}

export function closePurchasePath() {
  return ExpoButtonSdkModule.closePurchasePath();
}

export function hidePip() {
  return ExpoButtonSdkModule.hidePip();
}

export function showPip() {
  return ExpoButtonSdkModule.showPip();
}

