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

    init(options: NSDictionary) {
        super.init()
        self.headerTitle = RCTConvert.nsString(options["headerTitle"])
        self.headerSubtitle = RCTConvert.nsString(options["headerSubtitle"])
        self.headerTitleColor = RCTConvert.uiColor(options["headerTitleColor"])
        self.headerSubtitleColor = RCTConvert.uiColor(options["headerSubtitleColor"])
        self.headerBackgroundColor = RCTConvert.uiColor(options["headerBackgroundColor"])
        self.headerTintColor = RCTConvert.uiColor(options["headerTintColor"])
        self.footerBackgroundColor = RCTConvert.uiColor(options["footerBackgroundColor"])
        self.footerTintColor = RCTConvert.uiColor(options["footerTintColor"])
    }

    func browserDidInitialize(browser: BrowserInterface) {
        browser.header.title.text = self.headerTitle
        browser.header.subtitle.text = self.headerSubtitle
        browser.header.title.color = self.headerTitleColor
        browser.header.subtitle.color = self.headerSubtitleColor
        browser.header.backgroundColor = self.headerBackgroundColor
        browser.header.tintColor = self.headerTintColor
        browser.footer.backgroundColor = self.footerBackgroundColor
        browser.footer.tintColor = self.footerTintColor
    }

    func browserWillNavigate(browser: BrowserInterface) {
        browser.hideTopCard()
    }

    func browserDidClose() {
        #if DEBUG
        print("react-native-button-sdk browserDidClose")
        #endif
    }
}
