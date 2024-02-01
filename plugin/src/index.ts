import { ConfigPlugin, withInfoPlist, withAndroidManifest } from "@expo/config-plugins";

interface ButtonConfigPluginProps {
  iosAppId: string;
  androidAppId: string;
}

const withButtonConfig: ConfigPlugin<ButtonConfigPluginProps> = (
  config,
  { iosAppId, androidAppId },
) => {
  config = withInfoPlist(config, (config) => {
    config.modResults["ButtonSdkAppId"] = iosAppId;
    return config;
  });

  // Modify Android AndroidManifest.xml
  config = withAndroidManifest(config, async (config) => {
    const mainApplication = config?.modResults?.manifest?.application?.[0];

    // Define the meta-data element for Button SDK App ID
    const metaDataElement = {
      $: {
        'android:name': 'com.usebutton.sdk.ApplicationId',
        'android:value': androidAppId,
      },
    };

    // Add the meta-data element if it doesn't already exist
    if (mainApplication && !mainApplication?.['meta-data']) {
      mainApplication['meta-data'] = [];
    }
    mainApplication?.['meta-data']?.push(metaDataElement);

    return config;
  });

  return config;
};

export default withButtonConfig;
