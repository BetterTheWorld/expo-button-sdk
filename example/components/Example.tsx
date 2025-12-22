import {
  startPurchasePath,
  initializeSDK,
  hidePip,
  showPip,
  type StartPurchasePathOptions,
} from "@flipgive/expo-button-sdk";
import React, { useEffect, useState, useCallback } from "react";
import {
  ActivityIndicator,
  Alert,
  FlatList,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { DemoButton } from "./DemoButton";
import { PipControls } from "./PipControls";
import { ActionButtons } from "./ActionButtons";
import { DEMO_CONFIGS, type DemoConfig } from "./demoConfigs";

type DemoItem =
  | { type: "header" }
  | { type: "demo"; id: string; title: string; options: StartPurchasePathOptions }
  | { type: "pip-controls" }
  | { type: "actions" };

const TOKEN = process.env.EXPO_PUBLIC_BUTON_SDK_TOKEN_EXAMPLE ?? "";

export function Example() {
  const [isSDKInitialized, setIsSDKInitialized] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isPipHidden, setIsPipHidden] = useState(false);

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

  const handleDemoPress = useCallback((options: StartPurchasePathOptions) => {
    if (!TOKEN) {
      Alert.alert("Error", "Token not configured");
      return;
    }
    startPurchasePath(options);
  }, []);

  const handlePipToggle = useCallback(() => {
    if (isPipHidden) {
      showPip();
      setIsPipHidden(false);
    } else {
      hidePip();
      setIsPipHidden(true);
    }
  }, [isPipHidden]);

  const data: DemoItem[] = [
    { type: "header" },
    ...DEMO_CONFIGS.map((config: DemoConfig) => ({
      type: "demo" as const,
      id: config.id,
      title: config.title,
      options: config.options,
    })),
    { type: "pip-controls" },
    { type: "actions" },
  ];

  const renderItem = useCallback(
    ({ item }: { item: DemoItem }) => {
      switch (item.type) {
        case "header":
          return <Text style={styles.header}>Expo Button SDK Example</Text>;
        case "demo":
          return (
            <DemoButton
              title={item.title}
              onPress={() => handleDemoPress(item.options)}
            />
          );
        case "pip-controls":
          return (
            <PipControls isPipHidden={isPipHidden} onToggle={handlePipToggle} />
          );
        case "actions":
          return <ActionButtons />;
        default:
          return null;
      }
    },
    [handleDemoPress, isPipHidden, handlePipToggle]
  );

  const keyExtractor = useCallback((item: DemoItem, index: number) => {
    if (item.type === "demo") return item.id;
    return `${item.type}-${index}`;
  }, []);

  if (isLoading && !isSDKInitialized) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" />
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <FlatList
        data={data}
        renderItem={renderItem}
        keyExtractor={keyExtractor}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#fff",
  },
  loadingContainer: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  listContent: {
    paddingVertical: 20,
    paddingHorizontal: 16,
    gap: 12,
  },
  header: {
    fontSize: 20,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 8,
  },
});
