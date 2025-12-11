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
    const metaDataName = 'com.usebutton.sdk.ApplicationId';
    const metaDataElement = {
      $: {
        'android:name': metaDataName,
        'android:value': androidAppId,
      },
    };

    // Add the meta-data element if it doesn't already exist
    if (mainApplication) {
      if (!mainApplication['meta-data']) {
        mainApplication['meta-data'] = [];
      }
      
      // Check if it already exists to avoid duplicates
      const existingMeta = mainApplication['meta-data'].find(
        (item: any) => item.$['android:name'] === metaDataName
      );
      
      if (!existingMeta) {
        mainApplication['meta-data'].push(metaDataElement);
      } else {
        // Update existing value just in case
        existingMeta.$['android:value'] = androidAppId;
      }
    }

    // Enable Picture-in-Picture support for the main activity
    // We find the main activity by looking for the LAUNCHER category in intent-filters
    // This is more robust than looking for ".MainActivity"
    const mainActivity = mainApplication?.activity?.find((activity: any) => {
      const intentFilters = activity['intent-filter'] || [];
      return intentFilters.some((filter: any) => {
        const categories = filter.category || [];
        return categories.some((category: any) => 
          category.$['android:name'] === 'android.intent.category.LAUNCHER'
        );
      });
    });

    if (mainActivity) {
      mainActivity.$["android:supportsPictureInPicture"] = "true";
      
      // Ensure configuration changes are handled to prevent activity restart on PiP
      const existingConfigChanges = mainActivity.$["android:configChanges"] || "";
      const requiredChanges = ["smallestScreenSize", "screenLayout", "screenSize", "orientation"];
      
      // Filter out changes that are already present
      const changesToAdd = requiredChanges.filter(change => 
        !existingConfigChanges.includes(change)
      );
      
      if (changesToAdd.length > 0) {
        mainActivity.$["android:configChanges"] = existingConfigChanges 
          ? `${existingConfigChanges}|${changesToAdd.join("|")}`
          : changesToAdd.join("|");
      }
    }

    return config;
  });

  return config;
};

export default withButtonConfig;
