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
    
    init(options: NSDictionary) {
        super.init()
        self.headerTitle = options["headerTitle"] as? String
        self.headerSubtitle = options["headerSubtitle"] as? String
        
        // Assuming color values are provided as hex strings:
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
        
        // Initialize promotion manager if promotion data is provided
        if let promotionData = options["promotionData"] as? NSDictionary {
            self.promotionManager = PromotionManager(promotionData: promotionData, onPromotionClickCallback: { _ in })
        }
    }
    
    func setPromotionClickCallback(_ callback: @escaping (String) -> Void) {
        promotionManager?.setOnPromotionClickCallback(callback)
    }
    
    func closeBrowserIfNeeded(_ browser: BrowserInterface) {
        if closeOnPromotionClick {
            browser.dismiss()
        }
    }

    
    @objc func browserDidInitialize(_ browser: BrowserInterface) {
#if DEBUG
        print("expo-button-sdk browserDidInitialize")
#endif
        
        browser.header.title.text = self.headerTitle
        browser.header.subtitle.text = self.headerSubtitle
        browser.header.title.color = self.headerTitleColor
        browser.header.subtitle.color = self.headerSubtitleColor
        browser.header.backgroundColor = self.headerBackgroundColor
        browser.header.tintColor = self.headerTintColor
        browser.footer.backgroundColor = self.footerBackgroundColor
        browser.footer.tintColor = self.footerTintColor
        
        // Setup promotions badge if available
        promotionManager?.setupPromotionsBadge(for: browser)
    }
    
    func browserDidClose() {
#if DEBUG
        print("expo-button-sdk browserDidClose")
#endif
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
