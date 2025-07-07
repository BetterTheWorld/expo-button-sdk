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
  showExitConfirmation?: boolean;
}

export type Identifier = string;

export interface ButtonSDKStatus {
  isInitialized: boolean;
}
