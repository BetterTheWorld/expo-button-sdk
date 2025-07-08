import { Identifier, StartPurchasePathOptions } from "./ExpoButtonSdk.types";
import ExpoButtonSdkModule from "./ExpoButtonSdkModule";

export async function startPurchasePath(options: StartPurchasePathOptions) {
  // Set up the event listener if callback is provided
  if (options.onPromotionClick) {
    ExpoButtonSdkModule.addListener(
      "onPromotionClick",
      async (event: { promotionId: string; closeOnPromotionClick: boolean }) => {
        try {
          const result = await options.onPromotionClick!(event.promotionId);
          
          // Close current purchase path if the option is enabled (default: true)
          const shouldClose = event.closeOnPromotionClick ?? options.closeOnPromotionClick ?? true;
          if (shouldClose) {
            await closePurchasePath();
          }
          
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

    // Clean up listener when purchase path closes
    // Note: This cleanup logic might need adjustment based on Button SDK lifecycle
  }

  return await ExpoButtonSdkModule.startPurchasePath(options);
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
