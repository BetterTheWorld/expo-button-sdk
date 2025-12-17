import {
  startPurchasePath,
  clearAllData,
  setIdentifier,
  initializeSDK,
} from "@flipgive/expo-button-sdk";
import { MOCK_PROMOTION_DATA } from "../../src/constants/MockData";
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
} from "react-native";

export function Example() {
  const [isSDKInitialized, setIsSDKInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  console.info("isSDKInitialized", isSDKInitialized);

  useEffect(() => {
    async function initialize() {
      try {
        await initializeSDK();
        setIsSDKInitialized(true);
      } catch (error) {
        console.error("Failed to initialize Button SDK:", error);
      } finally {
        setIsLoading(false);
      }
    }

    initialize();
  }, []);

  if (isLoading && !isSDKInitialized) {
    return <ActivityIndicator />;
  }

  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.parentContainer}>
        <Text>Expo button sdk example</Text>
        <Button
          title="Open Browser"
          onPress={() => {
            if (
              !process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE ||
              !process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE
            ) {
              return;
            }

            startPurchasePath({
              footerBackgroundColor: "#FF3453",
              footerTintColor: "#FF3453",
              headerBackgroundColor: "#FF3453",
              headerSubtitle: "Gives 10%",
              headerSubtitleColor: "#FFE599",
              headerTintColor: "#",
              headerTitle: "Gapo",
              headerTitleColor: "#347796",
              url: "https://www.adidas.com/us",
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              exitConfirmation: {
                enabled: true,
                title: "Are you sure you want to leave?",
                message:
                  "You might miss out on exclusive offers and lose your progress.",
                stayButtonLabel: "Stay",
                leaveButtonLabel: "Leave",
              },
            });
          }}
        />
        <Button
          title="Open Browser with Picture-in-Picture (Default)"
          onPress={() => {
            if (
              !process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE ||
              !process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE
            ) {
              return;
            }

            startPurchasePath({
              footerBackgroundColor: "#FF3453",
              footerTintColor: "#FF3453",
              headerBackgroundColor: "#FF3453",
              headerSubtitle: "Gives 10%",
              headerSubtitleColor: "#FFE599",
              headerTintColor: "#",
              headerTitle: "Gapo",
              headerTitleColor: "#347796",
              url: "https://www.adidas.com/us",
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              animationConfig: {
                pictureInPicture: {
                  enabled: true,
                },
              },
              exitConfirmation: {
                enabled: true,
                title: "Are you sure you want to leave?",
                message:
                  "You might miss out on exclusive offers and lose your progress.",
                stayButtonLabel: "Stay",
                leaveButtonLabel: "Leave",
              },
            });
          }}
        />
        <Button
          title="Open PiP Square (YouTube Style)"
          onPress={() => {
            if (
              !process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE ||
              !process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE
            ) {
              return;
            }

            startPurchasePath({
              footerBackgroundColor: "#FF3453",
              footerTintColor: "#FF3453",
              headerBackgroundColor: "#FF3453",
              headerSubtitle: "Gives 10%",
              headerSubtitleColor: "#FFE599",
              headerTintColor: "#",
              headerTitle: "Gapo",
              headerTitleColor: "#347796",
              url: process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE,
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              animationConfig: {
                pictureInPicture: {
                  enabled: true,
                  // No size/position = defaults to square 120x120, bottom-right
                },
              },
              exitConfirmation: {
                enabled: true,
                title: "Are you sure you want to leave?",
                message:
                  "You might miss out on exclusive offers and lose your progress.",
                stayButtonLabel: "Stay",
                leaveButtonLabel: "Leave",
              },
            });
          }}
        />
        <Button
          title="Open Browser with Custom PiP Position & Size"
          onPress={() => {
            if (
              !process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE ||
              !process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE
            ) {
              return;
            }

            startPurchasePath({
              footerBackgroundColor: "#FF3453",
              footerTintColor: "#FF3453",
              headerBackgroundColor: "#FF3453",
              headerSubtitle: "Gives 10%",
              headerSubtitleColor: "#FFE599",
              headerTintColor: "#",
              headerTitle: "Gapo",
              headerTitleColor: "#347796",
              url: "https://www.adidas.com/us",
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              animationConfig: {
                pictureInPicture: {
                  enabled: true,
                  position: { x: 50, y: 100 },
                  size: { width: 180, height: 120 }, // Rectangular like YouTube
                  earnText: "Earn 2%",
                },
              },
              coverImage: {
                uri: "https://placecats.com/millie_neo/300/200",
                // Alternative options:
                // source: "my-local-image"  // from app bundle
                // base64: "iVBORw0KGgoAAAANSUhEUgA..." // base64 string
              },
              exitConfirmation: {
                enabled: true,
                title: "Are you sure you want to leave?",
                message:
                  "You might miss out on exclusive offers and lose your progress.",
                stayButtonLabel: "Stay",
                leaveButtonLabel: "Leave",
              },
            });
          }}
        />
        <Button
          title="Open Browser with Promotions"
          onPress={() => {
            if (
              !process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE ||
              !process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE
            ) {
              return;
            }

            startPurchasePath({
              headerTitleColor: "#ffffff",
              headerSubtitleColor: "#ffffff",
              headerBackgroundColor: "#074A7B",
              headerTintColor: "#ffffff",
              footerTintColor: "#ffffff",
              footerBackgroundColor: "#074A7B",
              headerSubtitle: "Gives 10%",
              headerTitle: "Gapo",
              promotionBadgeFontSize: 14,
              promotionBadgeLabel: "2 deals",
              promotionListTitle: "De@ls!",
              url: "https://www.adidas.com/us",
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              promotionData: MOCK_PROMOTION_DATA,
              closeOnPromotionClick: true, // Default: true - closes current instance when promotion is clicked
              onPromotionClick: async (promotionId: string) => {
                console.log("Promotion clicked:", promotionId);

                // Simulate API call - in real app this would be your GraphQL mutation
                await new Promise((resolve) => setTimeout(resolve, 1000));

                return {
                  url: `https://example.com`,
                  token: "new-token-for-promotion",
                };
              },
              exitConfirmation: {
                enabled: true,
                title: "Leaving so soon?",
                message: "If you exit now, you might miss your cashback.",
                stayButtonLabel: "Keep shopping",
                leaveButtonLabel: "Leave anyway",
              },
              headerLeftIcon:
                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAAGUjEkAAAABklEQVQoU2NkYGAAAABQABCA6gQm5gAAAABJRU5ErkJggg==",
              headerRightButtons: [
                {
                  icon: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA8AAAAPCAYAAAAGUjEkAAAABklEQVQoU2NkYGAAAABQABCA6gQm5gAAAABJRU5ErkJggg==",
                  action: "close",
                },
                { title: "⭐", action: "favorite" },
              ],
              onHeaderButtonClick: (action: string) => {
                console.log("Header Button:", action);
              },
              animationConfig: {
                pictureInPicture: {
                  enabled: true,
                  position: { x: 20, y: 200 },
                  size: { width: 300, height: 450 },
                  earnText: "Earn 2%",
                },
              },
            });
          }}
        />
        <Button title="Clear Data" onPress={clearAllData} />
        <Button
          title="Sing in user"
          onPress={() => {
            setIdentifier("user-identifier");
          }}
        />
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  parentContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    gap: 10,
  },
});
