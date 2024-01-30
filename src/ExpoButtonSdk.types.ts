export interface StartPurchasePathOptions {
  url?: string;
  token?: string;
  headerTitle?: string;
  headerSubtitle?: string;
  headerTitleColor?: string;
  headerSubtitleColor?: string;
  headerBackgroundColor?: string;
  headerTintColor?: string;
  footerBackgroundColor?: string;
  footerTintColor?: string;
}

export interface OpenURLOptions {
  url: string;
  title: string;
  subtitle: string;
}

export type Identifier = string; // or number, depending on what the identifier is
