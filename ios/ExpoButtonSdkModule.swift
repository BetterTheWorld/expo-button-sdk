import ExpoModulesCore
import Button

public class ExpoButtonSdkModule: Module {
    private var currentPurchasePathExtension: PurchasePathExtensionCustom?
    
    public static func requiresMainQueueSetup() -> Bool {
            return true
        }
    
    private func setupNotificationObserver() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleHeaderButtonClick),
            name: NSNotification.Name("onHeaderButtonClick"),
            object: nil
        )
    }
    
    @objc private func handleHeaderButtonClick(_ notification: Notification) {
        if let action = notification.userInfo?["action"] as? String {
            sendEvent("onHeaderButtonClick", ["action": action])
        }
    }
    
    public func definition() -> ModuleDefinition {
        Name("ExpoButtonSdk")
        
        Events("onPromotionClick", "onHeaderButtonClick")
        
        Function("minimizeBrowser") {
            // No longer needed - handled by native button
        }
        
        Function("maximizeBrowser") {
            // No longer needed - handled by native button
        }
        
        Function("toggleBrowserSize") {
            // No longer needed - handled by native button
        }
        
        Function("hidePip") {
            currentPurchasePathExtension?.hidePip()
        }
        
        Function("showPip") {
            currentPurchasePathExtension?.showPip()
        }
        
        AsyncFunction("initializeSDK") { (promise: Promise) in
            if ButtonSDKDelegate.isConfigured {
                promise.resolve(true)
                return
            }
            
            if let buttonAppID = Bundle.main.object(forInfoDictionaryKey: "ButtonSdkAppId") as? String {
                Button.configure(applicationId: buttonAppID) { error in
                    if let error = error {
                        promise.reject("InitError", error.localizedDescription)
                    } else {
                        print("🟢 Success ButtonSdk: Configuration successful. [initializeSDK]")
                        ButtonSDKDelegate.isConfigured = true
                        promise.resolve(true)
                    }
                }
            } else {
                promise.reject("ConfigError", "ButtonSdkAppId not found in Info.plist")
            }
        }
        
        AsyncFunction("startPurchasePath") { [weak self] (options: [String: Any], promise: Promise) in
            guard let self = self else {
                promise.reject("InvalidArguments", "startPurchasePath expects a dictionary of options.")
                return
            }
            let options = options as NSDictionary
            
            // Ensure Button SDK is configured
            guard ButtonSDKDelegate.isConfigured else {
                promise.reject("SDKNotConfigured", "Button SDK must be configured before calling startPurchasePath")
                return
            }
            
            // Define the start logic as a closure to be called after cleanup
            let startNewPath = {
                DispatchQueue.main.async {
                    guard let urlString = options["url"] as? String,
                          let url = URL(string: urlString) else {
                        promise.reject("InvalidURL", "The URL provided is invalid.")
                        return
                    }
                    
        #if DEBUG
                    print("expo-button-sdk startPurchasePath: url: \(url)")
        #endif

                    let purchasePathExtension = PurchasePathExtensionCustom(options: options)
                    
                    // Store reference for potential cleanup
                    self.currentPurchasePathExtension = purchasePathExtension
                    
                    // Set up promotion click callback with immediate browser dismiss
                    purchasePathExtension.setPromotionClickCallback { [weak self] promotionId, browser in

                        // Close browser immediately if needed
                        if purchasePathExtension.closeOnPromotionClick {
                            browser?.dismiss()
                        } else {
                            // Don't hide loader here - let navigation methods handle it
                            print("🔄 Loader management delegated to navigation methods (closeOnPromotionClick=false)")
                        }

                        // Send event to JavaScript
                        self?.sendEvent("onPromotionClick", [
                            "promotionId": promotionId,
                            "closeOnPromotionClick": purchasePathExtension.closeOnPromotionClick
                        ])
                    }
                    
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
                        } else if let purchasePath = purchasePath {
                            purchasePath.start()
                            promise.resolve(nil)
                        }
                    }
                }
            }

            // Cleanup existing purchase path if any (closes PiP and Browser)
            if let existingExtension = self.currentPurchasePathExtension {
                print("expo-button-sdk: Cleaning up existing extension before starting new one")
                existingExtension.cleanup(completion: startNewPath)
            } else {
                // No existing extension, start immediately
                startNewPath()
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
        
        Function("closePurchasePath") {
#if DEBUG
            print("expo-button-sdk closePurchasePath")
#endif
        }

    }
}
