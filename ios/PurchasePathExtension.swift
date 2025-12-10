import ExpoModulesCore
import Button

class PurchasePathExtensionCustom: NSObject, PurchasePathExtension {
    
    var headerTitle: String?
    var headerSubtitle: String?
    var headerTitleColor: UIColor?
    var headerSubtitleColor: UIColor?
    var headerBackgroundColor: UIColor?
    var headerTintColor: UIColor?
    var footerBackgroundColor: UIColor?
    var footerTintColor: UIColor?
    // Exit confirmation configuration
    var exitConfirmationEnabled: Bool = false
    var exitConfirmationTitle: String?
    var exitConfirmationMessage: String?
    var stayButtonLabel: String?
    var leaveButtonLabel: String?
    // Promotion manager
    private var promotionManager: PromotionManager?
    var closeOnPromotionClick: Bool = true
    // Promotion labels
    var promotionBadgeLabel: String?
    var promotionListTitle: String?
    var promotionBadgeFontSize: CGFloat = 11.0
    private var options: NSDictionary?
    private var currentBrowser: BrowserInterface?
    private var pipManager: PictureInPictureManager?
    
    init(options: NSDictionary) {
        self.options = options
        super.init()
        self.headerTitle = options["headerTitle"] as? String
        self.headerSubtitle = options["headerSubtitle"] as? String
        
        // Parse color values
        self.headerTitleColor = (options["headerTitleColor"] as? String).flatMap { UIColor(hex: $0) }
        self.headerSubtitleColor = (options["headerSubtitleColor"] as? String).flatMap { UIColor(hex: $0) }
        self.headerBackgroundColor = (options["headerBackgroundColor"] as? String).flatMap { UIColor(hex: $0) }
        self.headerTintColor = (options["headerTintColor"] as? String).flatMap { UIColor(hex: $0) }
        self.footerBackgroundColor = (options["footerBackgroundColor"] as? String).flatMap { UIColor(hex: $0) }
        self.footerTintColor = (options["footerTintColor"] as? String).flatMap { UIColor(hex: $0) }
        
        // Parse exit confirmation config
        if let exitConfirmationConfig = options["exitConfirmation"] as? NSDictionary {
            self.exitConfirmationEnabled = exitConfirmationConfig["enabled"] as? Bool ?? false
            self.exitConfirmationTitle = exitConfirmationConfig["title"] as? String
            self.exitConfirmationMessage = exitConfirmationConfig["message"] as? String
            self.stayButtonLabel = exitConfirmationConfig["stayButtonLabel"] as? String
            self.leaveButtonLabel = exitConfirmationConfig["leaveButtonLabel"] as? String
        }
        
        // Parse closeOnPromotionClick option (default: true)
        self.closeOnPromotionClick = options["closeOnPromotionClick"] as? Bool ?? true
        
        // Parse promotion label options
        self.promotionBadgeLabel = options["promotionBadgeLabel"] as? String
        self.promotionListTitle = options["promotionListTitle"] as? String
        self.promotionBadgeFontSize = options["promotionBadgeFontSize"] as? CGFloat ?? 11.0
        
        // Initialize promotion manager if promotion data is provided
        if let promotionData = options["promotionData"] as? NSDictionary {
            self.promotionManager = PromotionManager(
                promotionData: promotionData, 
                onPromotionClickCallback: { _, _ in },
                badgeLabel: self.promotionBadgeLabel,
                listTitle: self.promotionListTitle,
                badgeFontSize: self.promotionBadgeFontSize
            )
        }
    }
    
    func setPromotionClickCallback(_ callback: @escaping (String, BrowserInterface?) -> Void) {
        promotionManager?.setOnPromotionClickCallback(callback)
    }
    
    func closeBrowserIfNeeded(_ browser: BrowserInterface) {
        if closeOnPromotionClick {
            browser.dismiss()
        }
    }
    
    // MARK: - Promotion Button Position Control
    
    /// Hide the promotion button (useful when it conflicts with system UI)
    func hidePromotionButton() {
        promotionManager?.hideButton()
    }
    
    /// Show the promotion button
    func showPromotionButton() {
        promotionManager?.showButton()
    }
    
    /// Manually check and update the button position
    func updatePromotionButtonPosition() {
        promotionManager?.updateButtonPosition()
    }
    
    /// Debug method to check button position with detailed logging
    func debugPromotionButtonPosition() {
        promotionManager?.debugCheckPosition()
    }
    
    /// Force hide button for testing
    func forceHidePromotionButton() {
        promotionManager?.forceHideButton()
    }
    
    /// Force show button for testing
    func forceShowPromotionButton() {
        promotionManager?.forceShowButton()
    }
    
    /// Show toast if there was a promo code copied during navigation
    private func showToastIfPromoCodeWasCopied() {
        // Check if there's a copied promo code to show toast for
        if let _ = UIPasteboard.general.string, let promotionManager = self.promotionManager {
            // Check if we had a promo code that was supposed to be copied
            // We'll use a simple check - if clipboard has content and we just finished navigation
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                // Try to show toast from promotion manager (it has the logic)
                promotionManager.showCopiedToastIfNeeded()
            }
        }
    }

    
    @objc func browserDidInitialize(_ browser: BrowserInterface) {
        currentBrowser = browser
        
        browser.header.title.text = self.headerTitle
        browser.header.subtitle.text = self.headerSubtitle
        browser.header.title.color = self.headerTitleColor
        browser.header.subtitle.color = self.headerSubtitleColor
        browser.header.backgroundColor = self.headerBackgroundColor
        browser.header.tintColor = self.headerTintColor
        browser.footer.backgroundColor = self.footerBackgroundColor
        browser.footer.tintColor = self.footerTintColor
        
        // Add minimize button if Picture-in-Picture mode is enabled  
        var pipEnabled = false
        
        if let animationConfig = options?["animationConfig"] as? [String: Any] {
            // Check new pictureInPicture config
            if let pipConfig = animationConfig["pictureInPicture"] as? [String: Any],
               let enabled = pipConfig["enabled"] as? Bool {
                pipEnabled = enabled
            }
            // Check legacy pictureInPictureMode
            else if let legacyMode = animationConfig["pictureInPictureMode"] as? Bool {
                pipEnabled = legacyMode
            }
        }
        
        if pipEnabled {
            let optionsDict = options as? [String: Any] ?? [:]
            pipManager = PictureInPictureManager(options: optionsDict)
            pipManager?.addMinimizeButton(to: browser)
        }
        
        promotionManager?.setupPromotionsBadge(for: browser)
        self.promotionManager?.showPendingPromoCodeToast()
    }
    
    // PiP functionality moved to PictureInPictureManager
    
    func browser(_ browser: BrowserInterface, didNavigateTo page: BrowserPage) {
#if DEBUG
        print("expo-button-sdk didNavigateTo - URL loaded, will hide loader with 2s delay")
#endif
        // Wait 2 seconds after URL loads to ensure Button SDK window is fully visible
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            // Don't hide loader here - it will be hidden after 3 seconds by PromotionManager
            print("🔄 Navigation complete (2s delay) - loader managed by PromotionManager")
        }
    }
    
    func browser(_ browser: BrowserInterface, didNavigateToProduct page: ProductPage) {
#if DEBUG
        print("expo-button-sdk didNavigateToProduct - URL loaded, will hide loader with 2s delay")
#endif
        // Wait 2 seconds after URL loads to ensure Button SDK window is fully visible
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            // Don't hide loader here - it will be hidden after 3 seconds by PromotionManager
            print("🔄 Product navigation complete (2s delay) - loader managed by PromotionManager")
        }
    }
    
    func browser(_ browser: BrowserInterface, didNavigateToPurchase page: PurchasePage) {
#if DEBUG
        print("expo-button-sdk didNavigateToPurchase - URL loaded, will hide loader with 2s delay")
#endif
        // Wait 2 seconds after URL loads to ensure Button SDK window is fully visible
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            // Don't hide loader here - it will be hidden after 3 seconds by PromotionManager
            print("🔄 Purchase navigation complete (2s delay) - loader managed by PromotionManager")
        }
    }
    
    func browserDidClose() {
        print("expo-button-sdk browserDidClose")
        
        // Clean up PiP resources when browser closes
        pipManager?.cleanup()
        pipManager = nil
        
        // Clean up references
        currentBrowser = nil
        
        print("🔄 Browser closed - loader management delegated to navigation methods")
    }
    
    func shouldCloseBrowser(_ browser: BrowserInterface) -> Bool {
#if DEBUG
        print("expo-button-sdk shouldCloseBrowser called, exitConfirmationEnabled: \(exitConfirmationEnabled)")
#endif
        if exitConfirmationEnabled {
            // Use the new BrowserAlertManager with configurable labels
            BrowserAlertManager.showExitConfirmationAlert(
                browser: browser, 
                title: self.exitConfirmationTitle, 
                message: self.exitConfirmationMessage,
                stayButtonLabel: self.stayButtonLabel,
                leaveButtonLabel: self.leaveButtonLabel
            ) { shouldLeave in
                if shouldLeave {
                    browser.dismiss()
                }
            }
            return false // Always prevent automatic closure
        }
        return true
    }
    
    // Utility to convert hex string to UIColor
    func colorFromHex(_ hex: String) -> UIColor? {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
        
        var rgb: UInt64 = 0
        
        Scanner(string: hexSanitized).scanHexInt64(&rgb)
        
        let red = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
        let green = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
        let blue = CGFloat(rgb & 0x0000FF) / 255.0
        
        return UIColor(red: red, green: green, blue: blue, alpha: 1.0)
    }
}

extension UIColor {
    convenience init?(hex: String) {
        let r, g, b, a: CGFloat
        
        if hex.hasPrefix("#") {
            let start = hex.index(hex.startIndex, offsetBy: 1)
            let hexColor = String(hex[start...])
            
            if hexColor.count == 6 {
                let scanner = Scanner(string: hexColor)
                var hexNumber: UInt64 = 0
                
                if scanner.scanHexInt64(&hexNumber) {
                    r = CGFloat((hexNumber & 0xff0000) >> 16) / 255
                    g = CGFloat((hexNumber & 0x00ff00) >> 8) / 255
                    b = CGFloat(hexNumber & 0x0000ff) / 255
                    a = 1.0
                    
                    self.init(red: r, green: g, blue: b, alpha: a)
                    return
                }
            }
        }
        
        return nil
    }
}
