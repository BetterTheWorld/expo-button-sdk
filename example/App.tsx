import {
  startPurchasePath,
  clearAllData,
  setIdentifier,
} from "expo-button-sdk";
import {
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
} from "react-native";

export default function App() {
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
              footerBackgroundColor: "#48B1BF",
              footerTintColor: "#FFFFFF",
              headerBackgroundColor: "#48B1BF",
              headerSubtitle: "Gives 1%",
              headerSubtitleColor: "#FFFFFF",
              headerTintColor: "#FFFFFF",
              headerTitle: "Gap",
              headerTitleColor: "#FFFFFF",
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
    gap: 10
  },
});
