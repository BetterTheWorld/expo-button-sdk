import * as React from 'react';

import { ExpoButtonSdkViewProps } from './ExpoButtonSdk.types';

export default function ExpoButtonSdkView(props: ExpoButtonSdkViewProps) {
  return (
    <div>
      <span>{props.name}</span>
    </div>
  );
}
