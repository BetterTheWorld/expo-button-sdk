import {
  OpenURLOptions,
  StartPurchasePathOptions,
} from "./ExpoButtonSdk.types";
import ExpoButtonSdkModule from "./ExpoButtonSdkModule";

export async function startPurchasePath(options: StartPurchasePathOptions) {
  return await ExpoButtonSdkModule.StartPurchasePathOptions(options);
}

export function clearAllData() {
  return ExpoButtonSdkModule.clearAllData();
}

export function openURL(options: OpenURLOptions) {
  return ExpoButtonSdkModule.openURL(options);
}