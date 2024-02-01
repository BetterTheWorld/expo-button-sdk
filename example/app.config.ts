import { ExpoConfig, ConfigContext } from "@expo/config";

module.exports = ({ config }: ConfigContext): ExpoConfig => {
  return {
    ...config,
    name: "expo-button-sdk-example",
    slug: "expo-button-sdk-example",
    plugins: [
      "./plugins/withTrustLocalCerts",
      [
        "../app.plugin.js",
        {
          iosAppId: process.env.EXPO_PUBLIC_BUTON_SDK_IOS_APP_ID,
          androidAppId: process.env.EXPO_PUBLIC_BUTON_SDK_ANDROID_APP_ID,
        },
      ],
      [
        "expo-build-properties",
        {
          "android": {
            "usesCleartextTraffic": false
          }
        }
      ]
    ],
  };
};
