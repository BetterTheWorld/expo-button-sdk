import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';

import { ExpoButtonSdkViewProps } from './ExpoButtonSdk.types';

const NativeView: React.ComponentType<ExpoButtonSdkViewProps> =
  requireNativeViewManager('ExpoButtonSdk');

export default function ExpoButtonSdkView(props: ExpoButtonSdkViewProps) {
  return <NativeView {...props} />;
}
