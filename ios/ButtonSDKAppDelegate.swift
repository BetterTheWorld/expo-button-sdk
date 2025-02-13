import ExpoModulesCore
import Button

public class ButtonSDKDelegate: ExpoAppDelegateSubscriber {
    static var isConfigured = false
    
    private func configureButtonSDK() {
        guard !ButtonSDKDelegate.isConfigured else { return }
        
        if let buttonAppID = Bundle.main.object(forInfoDictionaryKey: "ButtonSdkAppId") as? String {
            Button.configure(applicationId: buttonAppID) { error in
                if let error = error {
                    print("🔴 Error ButtonSdk: \(error.localizedDescription)")
                } else {
                    print("🟢 Success ButtonSdk: Configuration successful.")
                    ButtonSDKDelegate.isConfigured = true
                }
            }
        } else {
            print("⚠️ Warning: ButtonSdkAppId not found in Info.plist")
        }
    }

    public func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        configureButtonSDK()
        return true
    }
    
    public func applicationDidBecomeActive(_ application: UIApplication) {
        configureButtonSDK()
    }
}
