import UIKit
import Button

class BrowserAlertManager {
    
    static func showExitConfirmationAlert(browser: BrowserInterface, title: String?, message: String?, completion: @escaping (Bool) -> Void) {
#if DEBUG
        print("expo-button-sdk showExitConfirmationAlert called")
#endif
        
        DispatchQueue.main.async {
            let alert = UIAlertController(
                title: title ?? "Are you sure you want to leave?",
                message: message ?? "You may lose your progress and any available deals.",
                preferredStyle: .alert
            )
            
            alert.addAction(UIAlertAction(title: "Stay", style: .cancel) { _ in
                completion(false) // User chose to stay
            })
            
            alert.addAction(UIAlertAction(title: "Leave", style: .destructive) { _ in
                completion(true) // User chose to leave
            })
            
            if let topViewController = BrowserAlertManager.getTopViewController() {
#if DEBUG
                print("expo-button-sdk presenting alert on: \(topViewController)")
#endif
                topViewController.present(alert, animated: true, completion: nil)
            } else {
#if DEBUG
                print("expo-button-sdk ERROR: No top view controller found")
#endif
                completion(false) // Default to staying if we can't present
            }
        }
    }
    
    private static func getTopViewController() -> UIViewController? {
        // Try multiple approaches to find the top view controller
        
        // Approach 1: Using key window
        if let keyWindow = UIApplication.shared.windows.first(where: { $0.isKeyWindow }),
           let rootViewController = keyWindow.rootViewController {
            var topViewController = rootViewController
            while let presentedViewController = topViewController.presentedViewController {
                topViewController = presentedViewController
            }
            return topViewController
        }
        
        // Approach 2: Using window scene
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = windowScene.windows.first {
            var topViewController = window.rootViewController
            while let presentedViewController = topViewController?.presentedViewController {
                topViewController = presentedViewController
            }
            return topViewController
        }
        
        // Approach 3: Using shared application windows
        for window in UIApplication.shared.windows {
            if window.isKeyWindow {
                var topViewController = window.rootViewController
                while let presentedViewController = topViewController?.presentedViewController {
                    topViewController = presentedViewController
                }
                return topViewController
            }
        }
        
        return nil
    }
}