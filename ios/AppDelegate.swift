import ExpoModulesCore
import Button

public class AppDelegate: ExpoAppDelegateSubscriber {
    public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        if let buttonAppID = Bundle.main.object(forInfoDictionaryKey: "ButtonSdkAppId") as? String {
            
            Button.configure(applicationId: buttonAppID) { error in
                if let error = error {
                    print("🔴 Error ButtonSdk: \(error.localizedDescription)")
                } else {
                    print("🟢 Success ButtonSdk: Configuration successful.")
                }
            }
        }
        return true
    }
}
