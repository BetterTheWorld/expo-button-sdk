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
    var showExitConfirmation: Bool = false
    
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
        self.showExitConfirmation = options["showExitConfirmation"] as? Bool ?? false
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
    }
    
    func browserDidClose() {
#if DEBUG
        print("expo-button-sdk browserDidClose")
#endif
    }
    
    func shouldCloseBrowser(_ browser: BrowserInterface) -> Bool {
#if DEBUG
        print("expo-button-sdk shouldCloseBrowser called, showExitConfirmation: \(showExitConfirmation)")
#endif
        if showExitConfirmation {
            // Use the new BrowserAlertManager
            BrowserAlertManager.showExitConfirmationAlert(browser: browser) { shouldLeave in
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
