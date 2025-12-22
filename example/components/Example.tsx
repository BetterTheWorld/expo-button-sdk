import {
  startPurchasePath,
  clearAllData,
  setIdentifier,
  initializeSDK,
  hidePip,
  showPip,
} from "@flipgive/expo-button-sdk";
import { MOCK_PROMOTION_DATA } from "../../src/constants/MockData";
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native";

export function Example() {
  const [isSDKInitialized, setIsSDKInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isPipHidden, setIsPipHidden] = useState(false);

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
                scaleType: "stretch",
                backgroundColor: "#1a1a2e",
                padding: 8,
              },
              exitConfirmation: {
                enabled: true,
                title: "Are you sure you want to leave?",
                message:
                  "You might miss out on exclusive offers and lose your progress.",
                stayButtonLabel: "Stay",
                leaveButtonLabel: "Leave",
              },
              onClose: () => {
                Alert.alert("close");
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
        <Button
          title="Open Browser with PIP Controls"
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
              headerSubtitle: "Gives 5%",
              headerSubtitleColor: "#FFE599",
              headerTintColor: "#FFFFFF",
              headerTitle: "Nike Store",
              headerTitleColor: "#FFFFFF",
              url: "https://www.nike.com",
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              animationConfig: {
                pictureInPicture: {
                  enabled: true,
                  chevronColor: "#FFFFFF",
                  earnText: "Earn 5% cashback",
                  earnTextColor: "#FFFFFF",
                  earnTextBackgroundColor: "#FF3453",
                },
              },
              coverImage: {
                uri: "https://images.unsplash.com/photo-1542291026-7eec264c27ff",
                scaleType: "center",
                backgroundColor: "white",
              },
            });
          }}
        />
        <View style={styles.pipControlsContainer}>
          <Text style={styles.pipControlsTitle}>PIP Controls:</Text>
          <View style={styles.pipButtonsRow}>
            <Button
              title={isPipHidden ? "Show PIP" : "Hide PIP"}
              onPress={() => {
                if (isPipHidden) {
                  showPip();
                  setIsPipHidden(false);
                } else {
                  hidePip();
                  setIsPipHidden(true);
                }
              }}
            />
            <Button
              title="Toggle PIP"
              onPress={() => {
                if (isPipHidden) {
                  showPip();
                  setIsPipHidden(false);
                } else {
                  hidePip();
                  setIsPipHidden(true);
                }
              }}
            />
          </View>
        </View>
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
  pipControlsContainer: {
    marginVertical: 10,
    padding: 15,
    backgroundColor: "#f0f0f0",
    borderRadius: 8,
    width: "90%",
  },
  pipControlsTitle: {
    fontSize: 16,
    fontWeight: "bold",
    marginBottom: 10,
    textAlign: "center",
  },
  pipButtonsRow: {
    flexDirection: "row",
    justifyContent: "space-around",
    gap: 10,
  },
});
