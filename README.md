# expo-button-sdk

Button sdk for expo

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


```typescript
import {
  startPurchasePath,
  clearAllData,
  setIdentifier,
} from "@flipgive/expo-button-sdk";

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

# Contributing

Contributions are very welcome! Please refer to guidelines described in the [contributing guide]( https://github.com/expo/expo#contributing).
