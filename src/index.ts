import {
  Identifier,
  StartPurchasePathOptions,
  ButtonSDKStatus,
} from "./ExpoButtonSdk.types";
import ExpoButtonSdkModule from "./ExpoButtonSdkModule";

export async function startPurchasePath(options: StartPurchasePathOptions) {
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
