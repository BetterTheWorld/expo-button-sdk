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

### Picture in Picture (PiP)

Enable Picture-in-Picture mode to keep the browser visible while users navigate your app.

**Note on Android:** Android uses Native PiP, which means the OS controls the window size and position. Custom `size` and `position` props are ignored on Android.

```typescript
startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",

  animationConfig: {
    pictureInPicture: {
      enabled: true,

      // Use simulated PiP on Android (default: true for native PiP)
      useNativePip: false,

      // Custom initial position and size (iOS & simulated Android)
      position: { x: 20, y: 100 },
      size: { width: 150, height: 200 },

      // Overlay button colors (iOS & simulated Android)
      chevronColor: "#FFFFFF",

      // Overlay button sizing & styling
      pipOverlayInset: 10,        // Inset from PiP window edges (default: 10pt iOS / 8dp Android)
      pipCloseButtonSize: 20,     // Size of the close (X) button (default: 20)
      pipChevronSize: 15,         // Width of the chevron icon (default: 15pt iOS / 20dp Android)
      pipChevronHeight: 10,       // Height of the chevron icon (default: auto, based on chevronSize)
      pipChevronStrokeWidth: 0.4, // Extra stroke to make chevron bolder (default: 0)
      pipCloseStrokeWidth: 0.1,   // Extra stroke to make X bolder (default: 0)

      // Earn label overlay on minimized PiP
      earnText: "Earn 2%",
      earnTextColor: "#FFFFFF",
      earnTextBackgroundColor: "#99000000",
      earnTextFontFamily: "OpenSans-SemiBold", // Must be bundled in host app
      earnTextFontSize: 14,                     // Default: 12
      earnTextFontWeight: "600",                // "100"-"900", "normal", "bold"
      earnTextLineHeight: 16,                   // Default: auto
      earnTextMargin: 8,                        // Bottom margin from PiP edge (default: 28pt iOS / 8dp Android)

      // Android native PiP only
      androidAspectRatio: { width: 3, height: 2 },
      hideOnAppBackground: true,
    }
  },

  // Optional: Cover image for the minimized PiP window (iOS & Android)
  coverImage: {
    uri: "https://example.com/logo.png",
    // OR
    // source: "local_asset_name" (iOS only)
    // OR
    // base64: "..." (iOS only)

    scaleType: "cover",          // "cover" | "contain" | "center" | "stretch"
    backgroundColor: "#FFFFFF",  // Background color visible with padding or transparent images
    padding: 8,                  // Padding around the image (points on iOS, dp on Android)
    width: 120,                  // Explicit image width (optional, fills available space by default)
    height: 80,                  // Explicit image height (optional, fills available space by default)
  },

  // Optional: Callback when browser is closed
  onClose: () => {
    console.log('Browser closed');
  }
});
```

#### PiP Options

| Option | Platform | Default | Description |
|--------|----------|---------|-------------|
| `enabled` | All | — | Enable PiP mode |
| `useNativePip` | Android | `true` | When `false`, uses a simulated draggable floating overlay instead of the native Android PiP API |
| `position` | iOS & simulated Android | — | Initial position `{ x, y }` |
| `size` | iOS & simulated Android | — | Initial size `{ width, height }` |
| `chevronColor` | iOS & simulated Android | `"#FFFFFF"` | Color of the close (X) and chevron icons |
| `pipOverlayInset` | iOS & simulated Android | `10` (iOS) / `8` (Android) | Inset/padding of overlay buttons from PiP window edges |
| `pipCloseButtonSize` | iOS & simulated Android | `20` | Size of the close (X) button in points/dp |
| `pipChevronSize` | iOS & simulated Android | `15` (iOS) / `20` (Android) | Width of the chevron icon in points/dp |
| `pipChevronHeight` | iOS & simulated Android | auto | Height of the chevron icon. If not set, defaults to `pipChevronSize * 10/18` |
| `pipChevronStrokeWidth` | iOS & simulated Android | `0` | Extra stroke width for the chevron to make it bolder. `0` = original FA6 Regular weight |
| `pipCloseStrokeWidth` | iOS & simulated Android | `0` | Extra stroke width for the close (X) to make it bolder. `0` = original FA6 Regular weight |
| `earnText` | All | — | Text displayed on minimized PiP overlay |
| `earnTextColor` | All | `"#FFFFFF"` | Color of earn text |
| `earnTextBackgroundColor` | All | — | Background color of earn text label |
| `earnTextFontFamily` | iOS & simulated Android | system font | Font family for earn text. Must be a font bundled in the host app (e.g., `"OpenSans-SemiBold"`) |
| `earnTextFontSize` | iOS & simulated Android | `12` | Font size for earn text |
| `earnTextFontWeight` | iOS & simulated Android | `"600"` | Font weight: `"normal"`, `"bold"`, or numeric `"100"`–`"900"` |
| `earnTextLineHeight` | iOS & simulated Android | auto | Line height for earn text |
| `earnTextMargin` | iOS & simulated Android | `28` (iOS) / `8` (Android) | Bottom margin of earn text from PiP window edge |
| `androidAspectRatio` | Android native PiP | — | Aspect ratio for native PiP window `{ width, height }` |
| `hideOnAppBackground` | Android native PiP | `false` | Auto-hide PiP when app goes to background |

#### Cover Image Options

| Option | Platform | Default | Description |
|--------|----------|---------|-------------|
| `uri` | iOS & Android | — | Remote image URL |
| `source` | iOS only | — | Local asset name |
| `base64` | iOS only | — | Base64 encoded image data |
| `scaleType` | iOS & Android | `"cover"` | Image scaling: `cover`, `contain`, `center`, `stretch` |
| `backgroundColor` | iOS & Android | transparent | Background color (visible with padding or transparent images) |
| `padding` | iOS & Android | `0` | Padding around the image in points/dp |
| `width` | iOS & Android | auto | Explicit width for the image. If not set, fills available space minus padding |
| `height` | iOS & Android | auto | Explicit height for the image. If not set, fills available space minus padding |

#### Platform Differences

- **iOS**: Custom floating window with cover image, close button (X), chevron-up, and earn label overlays. Tap chevron or PiP area to restore. Tap X to close completely. Draggable.
- **Android (simulated, `useNativePip: false`)**: Floating overlay with the same behavior as iOS — close button, chevron, earn label, draggable.
- **Android (native, `useNativePip: true`)**: Native system PiP with cover image and earn label overlay. Uses system controls. With `hideOnAppBackground: true`, PiP auto-hides when leaving the app and reappears when returning.

#### Programmatic PiP Control

You can programmatically hide and show the PiP window. This is useful when you want to temporarily hide the PiP (e.g., during checkout flows or when displaying important content).

```typescript
import { hidePip, showPip } from "@flipgive/expo-button-sdk";

// Hide the PiP window (moves it off-screen on iOS, moves task to back on Android)
hidePip();

// Show the PiP window again
showPip();
```

**Platform Behavior:**

| Method | iOS | Android |
|--------|-----|---------|
| `hidePip()` | Moves PiP window off-screen instantly | Moves PiP task to background |
| `showPip()` | Restores PiP window to last position | Brings PiP task to front and re-enters PiP mode |

**Notes:**
- These functions only work when PiP mode is active (`isMinimized = true`)
- On Android, `showPip()` briefly shows the activity in fullscreen before re-entering PiP mode
- Starting a new purchase path while PiP is hidden will automatically clean up the hidden PiP state

### onClose Callback

Get notified when the browser is closed:

```typescript
startPurchasePath({
  url: "https://the.button.url",
  token: "my-tracking-token",
  onClose: () => {
    console.log('Browser was closed');
    // Perform cleanup, analytics, or navigation
  }
});
```

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
