export interface ExitConfirmationConfig {
  enabled: boolean;
  title?: string;
  message?: string;
  stayButtonLabel?: string;
  leaveButtonLabel?: string;
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
}

export type Identifier = string;

export interface ButtonSDKStatus {
  isInitialized: boolean;
}
