import { startPurchasePath } from "expo-button-sdk";
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
            startPurchasePath({});
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
  }
});
