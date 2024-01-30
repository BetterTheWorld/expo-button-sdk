import {
  ConfigPlugin,
  withAppDelegate,
  withMainApplication,
} from "@expo/config-plugins";

interface ButtonConfigPluginProps {
  iosAppId: string;
  androidAppId: string;
}

const withButtonConfig: ConfigPlugin<ButtonConfigPluginProps> = (
  config,
  { iosAppId, androidAppId },
) => {
  config = withAppDelegate(config, async (config) => {
    if (config.modResults.path.endsWith(".mm")) {
      config.modResults.contents = modifyAppDelegate(
        config.modResults.contents,
        iosAppId,
      );

      return config;
    }

    throw new Error(
      "Cannot add Button SDK to AppDelegate because it is not written in Swift",
    );
  });

  config = withMainApplication(config, async (config) => {
    const isKotlin =
      config.modResults.language === "kt" ||
      config.modResults.path.endsWith(".kt");
    if (isKotlin) {
      config.modResults.contents = modifyMainApplication(
        config.modResults.contents,
        androidAppId,
      );
    } else {
      throw new Error(
        "Android MainApplication is not in Kotlin. Button SDK config requires Kotlin.",
      );
    }
    return config;
  });

  return config;
};

const modifyAppDelegate = (appDelegate: string, appId: string): string => {
  const importStatement = "#import <Button/Button.h>\n";
  const buttonConfigCode = `[Button configureWithApplicationId:@"${appId}" completion:nil]`;

  // Add the import statement at the top if not present
  if (!appDelegate.includes(importStatement.trim())) {
    appDelegate = importStatement + appDelegate;
  }

  // Add the Button SDK configuration after the didFinishLaunchingWithOptions function
  if (!appDelegate.includes("Button.configure(withApplicationId:")) {
    const didFinishLaunchingWithOptionsPattern = /- \(BOOL\)application:\(UIApplication \*\)application didFinishLaunchingWithOptions:\(NSDictionary \*\)launchOptions\s*\{/;

    appDelegate = appDelegate.replace(
      didFinishLaunchingWithOptionsPattern,
      (match) => {
        return `${match}\n  // Button SDK configuration\n  ${buttonConfigCode}`;
      },
    );
  }

  return appDelegate;
};

const modifyMainApplication = (
  mainApplication: string,
  androidAppId: string,
): string => {
  const importStatement = "import com.usebutton.sdk.Button\n";
  const buttonConfigCode = `if (BuildConfig.DEBUG) {
    Button.debug().setLoggingEnabled(true);
  }
  Button.configure(this, "${androidAppId}");\n`;

  // Add the import statement at the top if not present
  if (!mainApplication.includes(importStatement.trim())) {
    mainApplication = importStatement + mainApplication;
  }

  // Add the Button SDK configuration in the onCreate method
  if (!mainApplication.includes("Button.configure(this,")) {
    const onCreatePattern = /override fun onCreate\(\) \{/g;
    mainApplication = mainApplication.replace(onCreatePattern, (match) => {
      return `${match}\n    // Button SDK configuration\n    ${buttonConfigCode}`;
    });
  }

  return mainApplication;
};

export default withButtonConfig;
