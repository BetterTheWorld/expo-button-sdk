# expo-button-sdk

Button sdk for expo https://developer.usebutton.com/

# Installation in managed Expo projects

For [managed](https://docs.expo.dev/archive/managed-vs-bare/) Expo projects, please follow the installation instructions in the [API documentation for the latest stable release](#api-documentation). If you follow the link and there is no documentation available then this library is not yet usable within managed projects &mdash; it is likely to be included in an upcoming Expo SDK release.

### Add the package to your npm dependencies

npm
```
npm install @flipgive/expo-button-sdk
```

yarn
```
yarn add @flipgive/expo-button-sdk
```

pnpm
```
pnpm add @flipgive/expo-button-sdk
```

# Configuration

Modify `app.json` or `app.config.json`

```json
plugins: [
      "...",
      [
        "@flipgive/expo-button-sdk",
        {
          iosAppId: "my_ios_id",
          androidAppId: "my_android_id",
        },
      ],
      "...."
]
```
# Run in expo dev client

`npx expo prebuild --clean`

and

`npx expo run:ios` or `npx expo run:android`


# API

## Basic Usage

```typescript
import {
  startPurchasePath,
  clearAllData,
  setIdentifier,
  initializeSDK,
} from "@flipgive/expo-button-sdk";

// Initialize the SDK (recommended on app start)
await initializeSDK();

// Fetch a purchase path from Button and start the browser
startPurchasePath({
  url:                   "https://the.button.url", // required
  token:                 "my-tracking-token", // required
  headerTitle:           "My Button Browser Title",
  headerSubtitle:        "My Button Browser Subtitle",
  headerTitleColor:      "#FFFFFF", // only hexadecimal (6 CHARS) format accepted
  headerSubtitleColor:   "#FFFFFF",
  headerBackgroundColor: "#FFFFFF",
  headerTintColor:       "#FFFFFF",
  footerBackgroundColor: "#FFFFFF",
  footerTintColor:       "#FFFFFF"
});

// On user login
setIdentifier(id); // id required

// On user logout
clearAllData();
```

## Advanced Features

### Exit Confirmation Dialog

Show a confirmation dialog when users try to leave the purchase path:

```typescript
startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",
  exitConfirmation: {
    enabled: true,
    title: "Leave Shopping?",
    message: "Are you sure you want to leave? You might miss out on cashback.",
    stayButtonLabel: "Stay",
    leaveButtonLabel: "Leave"
  }
});
```

### Promotions with Deal Selection

Display and handle promotion deals within the purchase path:

```typescript
const promotionData = {
  merchantName: "Example Store",
  rewardText: "2% Cashback",
  featuredPromotion: {
    id: "featured-1",
    title: "Special Sale - Up to 65% off",
    subtitle: "Limited time offer",
    code: "",
    createdAt: "2025-07-07T22:00:00Z"
  },
  promotions: [
    {
      id: "promo-1",
      title: "New Sale Styles - Up to 50% off",
      subtitle: "Fresh arrivals",
      code: "SAVE20",
      createdAt: "2024-06-15T07:00:00Z"
    }
    // ... more promotions
  ]
};

startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",
  promotionData: promotionData,
  closeOnPromotionClick: true, // Default: true - closes current instance when promotion is clicked
  onPromotionClick: async (promotionId: string) => {
    console.log('Promotion clicked:', promotionId);
    
    // Convert promotion to purchase intent via your API
    const response = await fetch(`/api/promotions/${promotionId}/intent`);
    const intent = await response.json();
    
    return {
      url: intent.url,
      token: intent.token
    };
  }
});
```

### Promotion Features

- **🏷️ Badge**: Shows promotion count in header with tag icon
- **⭐ Featured**: Highlights featured promotions
- **🆕 New Badge**: Shows "NEW!" for promotions created in last 2 days
- **ActionSheet**: Native iOS list for promotion selection
- **Auto-close**: Configurable closing of current instance when selecting new promotion

# Contributing

Contributions are very welcome! Please refer to guidelines described in the [contributing guide]( https://github.com/expo/expo#contributing).
