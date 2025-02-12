import ExpoModulesCore
import Button

public class ButtonSDKDelegate: ExpoAppDelegateSubscriber {
    public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        // Get the Button SDK App ID from Info.plist
        if let buttonAppID = Bundle.main.object(forInfoDictionaryKey: "ButtonSdkAppId") as? String {
            Button.configure(applicationId: buttonAppID) { error in
                if let error = error {
                    print("🔴 Error ButtonSdk: \(error.localizedDescription)")
                } else {
                    print("🟢 Success ButtonSdk: Configuration successful.")
                }
            }
        } else {
            print("⚠️ Warning: ButtonSdkAppId not found in Info.plist")
        }
        
        return true
    }
}
