import { Identifier, StartPurchasePathOptions } from "./ExpoButtonSdk.types";
import ExpoButtonSdkModule from "./ExpoButtonSdkModule";

// Store the current listener to avoid accumulation
let currentPromotionListener: any = null;

export async function startPurchasePath(options: StartPurchasePathOptions) {
  // Clean up previous listener if exists
  if (currentPromotionListener) {
    currentPromotionListener.remove();
    currentPromotionListener = null;
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

  return await ExpoButtonSdkModule.startPurchasePath(sanitizedOptions);
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
