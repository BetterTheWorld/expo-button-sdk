import { NativeModulesProxy, EventEmitter, Subscription } from 'expo-modules-core';

// Import the native module. On web, it will be resolved to ExpoButtonSdk.web.ts
// and on native platforms to ExpoButtonSdk.ts
import ExpoButtonSdkModule from './ExpoButtonSdkModule';
import ExpoButtonSdkView from './ExpoButtonSdkView';
import { ChangeEventPayload, ExpoButtonSdkViewProps } from './ExpoButtonSdk.types';

// Get the native constant value.
export const PI = ExpoButtonSdkModule.PI;

export function hello(): string {
  return ExpoButtonSdkModule.hello();
}

export async function setValueAsync(value: string) {
  return await ExpoButtonSdkModule.setValueAsync(value);
}

const emitter = new EventEmitter(ExpoButtonSdkModule ?? NativeModulesProxy.ExpoButtonSdk);

export function addChangeListener(listener: (event: ChangeEventPayload) => void): Subscription {
  return emitter.addListener<ChangeEventPayload>('onChange', listener);
}

export { ExpoButtonSdkView, ExpoButtonSdkViewProps, ChangeEventPayload };
