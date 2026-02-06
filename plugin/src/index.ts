import { ConfigPlugin, withInfoPlist, withAndroidManifest } from "@expo/config-plugins";

interface ButtonConfigPluginProps {
  iosAppId: string;
  androidAppId: string;
  supportsPictureInPicture?: boolean;
}

const withButtonConfig: ConfigPlugin<ButtonConfigPluginProps> = (
  config,
  { iosAppId, androidAppId, supportsPictureInPicture = false },
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

    // Only add PiP support when explicitly enabled
    if (supportsPictureInPicture && mainApplication) {
      // Add REORDER_TASKS permission
      const manifest = config.modResults.manifest;
      if (!manifest['uses-permission']) {
        manifest['uses-permission'] = [];
      }
      const hasReorderPerm = manifest['uses-permission'].some(
        (perm: any) => perm.$['android:name'] === 'android.permission.REORDER_TASKS'
      );
      if (!hasReorderPerm) {
        manifest['uses-permission'].push({
          $: { 'android:name': 'android.permission.REORDER_TASKS' },
        });
      }

      // Enable PiP on MainActivity
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

        const existingConfigChanges = mainActivity.$["android:configChanges"] || "";
        const requiredChanges = ["smallestScreenSize", "screenLayout", "screenSize", "orientation"];
        const changesToAdd = requiredChanges.filter(change =>
          !existingConfigChanges.includes(change)
        );

        if (changesToAdd.length > 0) {
          mainActivity.$["android:configChanges"] = existingConfigChanges
            ? `${existingConfigChanges}|${changesToAdd.join("|")}`
            : changesToAdd.join("|");
        }
      }

      // Add WebViewActivity override with PiP support
      if (!mainApplication.activity) {
        mainApplication.activity = [];
      }
      const webViewActivityName = 'com.usebutton.sdk.internal.WebViewActivity';
      const existingWebView = mainApplication.activity.find(
        (activity: any) => activity.$['android:name'] === webViewActivityName
      );
      if (!existingWebView) {
        mainApplication.activity.push({
          $: {
            'android:name': webViewActivityName,
            'android:supportsPictureInPicture': 'true',
            'android:configChanges': 'screenSize|smallestScreenSize|screenLayout|orientation',
            'tools:replace': 'android:supportsPictureInPicture,android:configChanges',
          },
        });
      }
    }

    return config;
  });

  return config;
};

export default withButtonConfig;
