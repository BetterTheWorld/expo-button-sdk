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

    // Enable Picture-in-Picture support for the main activity
    const mainActivity = mainApplication?.activity?.find(
      (activity) => activity.$["android:name"] === ".MainActivity"
    );
    if (mainActivity) {
      mainActivity.$["android:supportsPictureInPicture"] = "true";
      // Ensure configuration changes are handled to prevent activity restart on PiP
      // Adding 'smallestScreenSize|screenLayout|screenSize' is recommended for PiP
      const existingConfigChanges = mainActivity.$["android:configChanges"] || "";
      const requiredChanges = ["smallestScreenSize", "screenLayout", "screenSize", "orientation"];
      const newConfigChanges = requiredChanges
        .filter((change) => !existingConfigChanges.includes(change))
        .join("|");
      
      if (newConfigChanges) {
        mainActivity.$["android:configChanges"] = existingConfigChanges 
          ? `${existingConfigChanges}|${newConfigChanges}`
          : newConfigChanges;
      }
    }

    return config;
  });

  return config;
};

export default withButtonConfig;
