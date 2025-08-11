# expo-button-sdk

Button sdk for expo https://developer.usebutton.com/

## Compatibility

| Package Version | Expo SDK Version |
|----------------|------------------|
| 0.3.2          | 50 - 52          |
| 1.0.0+         | 53+              |

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
  footerTintColor:       "#FFFFFF",
  promotionBadgeLabel:   "Deals", // optional: customize promotion badge label
  promotionListTitle:    "Available Deals", // optional: customize promotion list title
  promotionBadgeFontSize: 12 // optional: customize promotion badge font size (default: 11)
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

Display and handle promotion deals within the purchase path with modern card-based UI:

```typescript
const promotionData = {
  merchantName: "Example Store",
  rewardText: "2% Cashback",
  featuredPromotion: {
    id: "featured-1",
    description: "Special Sale - Up to 65% off 4% Cashback",
    couponCode: "SALE65",
    startsAt: "2025-07-10T09:00:00Z",
    endsAt: "2025-07-25T23:59:00Z"
  },
  promotions: [
    {
      id: "promo-1",
      description: "New Sale Styles - Up to 50% off 3% Cashback",
      couponCode: "SAVE20",
      startsAt: "2025-07-15T09:00:00Z",
      endsAt: "2025-07-30T23:59:00Z"
    }
    // ... more promotions
  ]
};

startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",
  promotionData: promotionData,
  closeOnPromotionClick: true, // Default: true - closes current instance when promotion is clicked
  promotionBadgeLabel: "Deals", // Customize badge label (default: "Offers")
  promotionListTitle: "Available Deals", // Customize list title (default: "Promotions")
  promotionBadgeFontSize: 13, // Customize badge font size (default: 11)
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

### Enhanced Promotion Features

#### 🎨 **Modern Card-based UI**
- **Card Design**: Clean, rounded cards with subtle borders and consistent spacing
- **Cross-platform**: Identical design and behavior on iOS and Android
- **Material Design**: Follows modern UI principles with proper shadows and animations

#### 🏷️ **Smart Time-based Labels**
- **NEW!** - Shows green badge for promotions less than 3 days old
- **THIS WEEK!** - Shows green badge for promotions 3-7 days old
- **Automatic Calculation**: Based on `startsAt` date, not manual text parsing
- **Clean Text**: Removes duplicate labels from descriptions automatically

#### 💰 **Structured Information Display**
- **Cashback Highlighting**: Automatically extracts and displays cashback percentages
- **Promo Codes**: Displays coupon codes with tag icons in styled containers
- **Time Remaining**: Shows countdown ("ends in 3d", "ends tomorrow", etc.)
- **Visual Hierarchy**: Clear separation between title, benefits, and metadata

#### 🎯 **Enhanced User Experience**
- **Bottom Sheet**: Native bottom sheet presentation with smooth animations
- **Tap Feedback**: Subtle ripple effects and touch feedback
- **Dismiss Animation**: Smooth slide-down animation when closing
- **Loading States**: Global loader during promotion navigation
- **Consistent Icons**: Same tag SVG icon across header badge and promotion codes

#### 📱 **Cross-platform Consistency**
- **Unified Design**: Identical layout, spacing, and colors on iOS and Android
- **Same Logic**: Identical date calculations and text processing
- **Responsive**: Adapts to different screen sizes and orientations

### Promotion Customization

You can customize the text labels used in the promotion UI:

```typescript
startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",
  promotionData: promotionData,
  
  // Customize the badge label (appears in header)
  promotionBadgeLabel: "Ofert@", // Default: "Offers"
  
  // Customize the list title (appears in promotion modal)
  promotionListTitle: "Promos!", // Default: "Promotions" (iOS) or "Available Promotions" (Android)
  
  // Customize the badge font size (affects both text and icon scaling)
  promotionBadgeFontSize: 12, // Default: 11, affects badge text size and icon scaling
  
  onPromotionClick: async (promotionId: string) => {
    // Handle promotion selection
    return { url: newUrl, token: newToken };
  }
});
```

**Supported Languages**: These labels can be customized for any language or branding needs.

### Badge Font Size Customization

The `promotionBadgeFontSize` prop allows you to customize the size of the promotion badge text and automatically scales the icon proportionally:

```typescript
startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",
  promotionData: promotionData,
  
  // Font size examples:
  promotionBadgeFontSize: 10,  // Smaller badge (icon scales to ~10.9px)
  promotionBadgeFontSize: 11,  // Default size (icon 12px on iOS, 14dp on Android)
  promotionBadgeFontSize: 13,  // Larger badge (icon scales to ~14.2px)
  promotionBadgeFontSize: 15,  // Extra large (icon scales to ~16.4px)
});
```

**Scaling Behavior:**
- **Text**: Direct font size control (e.g., 11pt, 13pt, 15pt)
- **Icon**: Automatically scales proportionally (scale factor = fontSize / 11)
- **Stroke Width**: Icon stroke thickness also scales to maintain visual consistency
- **Cross-platform**: Consistent scaling behavior on both iOS and Android

# Contributing

Contributions are very welcome! Please refer to guidelines described in the [contributing guide]( https://github.com/expo/expo#contributing).
