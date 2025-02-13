import {
  startPurchasePath,
  clearAllData,
  setIdentifier,
  initializeSDK,
} from "@flipgive/expo-button-sdk";
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
