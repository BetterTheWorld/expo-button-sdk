import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { clearAllData, setIdentifier } from "@flipgive/expo-button-sdk";

export function ActionButtons() {
  return (
    <View style={styles.container}>
      <Pressable style={styles.button} onPress={clearAllData}>
        <Text style={styles.text}>Clear Data</Text>
      </Pressable>
      <Pressable
        style={styles.button}
        onPress={() => setIdentifier("user-identifier")}
      >
        <Text style={styles.text}>Sign in user</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: "row",
    gap: 12,
    marginTop: 8,
  },
  button: {
    backgroundColor: "#FF3B30",
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    flex: 1,
    alignItems: "center",
  },
  text: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "600",
  },
});
