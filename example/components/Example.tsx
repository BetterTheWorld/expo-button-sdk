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
              url: process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE,
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
              url: process.env.EXPO_PUBLIC_BUTON_SDK_URL_EXAMPLE,
              token: process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE,
              promotionData: MOCK_PROMOTION_DATA,
              closeOnPromotionClick: true, // Default: true - closes current instance when promotion is clicked
              onPromotionClick: async (promotionId: string) => {
                console.log("Promotion clicked:", promotionId);

                // Simulate API call - in real app this would be your GraphQL mutation
                await new Promise((resolve) => setTimeout(resolve, 1000));

                return {
                  url: `https://example.com/promotion/${promotionId}`,
                  token: "new-token-for-promotion",
                };
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
