import ExpoModulesCore
import Button

public class ExpoButtonSdkModule: Module {
    public func definition() -> ModuleDefinition {
        Name("ExpoButtonSdk")
        AsyncFunction("startPurchasePath") { [weak self] (options: [Any], promise: Promise) in
            guard let self = self,
                  let options = options.first as? NSDictionary else {
                promise.reject("InvalidArguments", "startPurchasePath expects a dictionary of options.")
                return
            }
            
            guard let urlString = options["url"] as? String,
                  let url = URL(string: urlString) else {
                promise.reject("InvalidURL", "The URL provided is invalid.")
                return
            }
            
#if DEBUG
            print("expo-button-sdk startPurchasePath: url: \(url)")
#endif
            let browserConfig = BrowserConfig()
            browserConfig.title = options["headerTitle"] as? String
            browserConfig.subtitle = options["headerSubtitle"] as? String

            let purchasePathExtension = PurchasePathExtensionCustom(options: options)
            Button.purchasePath.extension = purchasePathExtension
            let request = PurchasePathRequest(url: url)
            
            if let token = options["token"] as? String {
#if DEBUG
                print("expo-button-sdk startPurchasePath: token: \(token)")
#endif
                request.pubRef = token
            }
            
            Button.purchasePath.fetch(request: request) { purchasePath, error in
                if let error = error {
                    promise.reject("FetchError", error.localizedDescription)
                } else {
                    purchasePath?.start()
                    promise.resolve(nil)
                }
            }
        }
        
        Function("clearAllData") {
#if DEBUG
            print("expo-button-sdk clearAllData")
#endif
            
            Button.clearAllData()
        }

        
        Function("setIdentifier") { (identifier: String) in
#if DEBUG
            print("react-native-button-sdk setIdentifier \(identifier)")
#endif
            
            Button.user.setIdentifier(identifier)
        }

    }
}
