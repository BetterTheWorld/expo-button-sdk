import React from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

interface PipControlsProps {
  isPipHidden: boolean;
  onToggle: () => void;
}

export function PipControls({ isPipHidden, onToggle }: PipControlsProps) {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>PIP Controls:</Text>
      <View style={styles.row}>
        <Pressable style={styles.button} onPress={onToggle}>
          <Text style={styles.buttonText}>
            {isPipHidden ? "Show PIP" : "Hide PIP"}
          </Text>
        </Pressable>
        <Pressable style={styles.button} onPress={onToggle}>
          <Text style={styles.buttonText}>Toggle PIP</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    marginVertical: 8,
    padding: 16,
    backgroundColor: "#f5f5f5",
    borderRadius: 12,
  },
  title: {
    fontSize: 16,
    fontWeight: "bold",
    marginBottom: 12,
    textAlign: "center",
  },
  row: {
    flexDirection: "row",
    justifyContent: "space-around",
    gap: 12,
  },
  button: {
    backgroundColor: "#34C759",
    paddingVertical: 10,
    paddingHorizontal: 20,
    borderRadius: 8,
    flex: 1,
    alignItems: "center",
  },
  buttonText: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "600",
  },
});
