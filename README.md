# expo-button-sdk

Button sdk for expo

# Installation in managed Expo projects

For [managed](https://docs.expo.dev/archive/managed-vs-bare/) Expo projects, please follow the installation instructions in the [API documentation for the latest stable release](#api-documentation). If you follow the link and there is no documentation available then this library is not yet usable within managed projects &mdash; it is likely to be included in an upcoming Expo SDK release.

# Installation in bare React Native projects

For bare React Native projects, you must ensure that you have [installed and configured the `expo` package](https://docs.expo.dev/bare/installing-expo-modules/) before continuing.

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

# Run in bare React Native projects

### Configure for iOS

Run `npx pod-install` after installing the npm package.


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
  headerTitleColor:      "#FFFFF", // only hexadecimal format accepted
  headerSubtitleColor:   "#FFFFF",
  headerBackgroundColor: "#FFFFF",
  headerTintColor:       "#FFFFF",
  footerBackgroundColor: "#FFFFF",
  footerTintColor:       "#FFFFF"
});

// On user login
setIdentifier(id); // id required

// On user logout
clearAllData();
```

# Contributing

Contributions are very welcome! Please refer to guidelines described in the [contributing guide]( https://github.com/expo/expo#contributing).
