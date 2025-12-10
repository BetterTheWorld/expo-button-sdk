import ExpoModulesCore
import Button
import UIKit
import WebKit

class PromotionManager: NSObject, ScrollVisibilityObserver {
    
    private var promotionData: NSDictionary?
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?
    private weak var currentBrowser: BrowserInterface?
    private var badgeLabel: String
    private var listTitle: String
    private var badgeFontSize: CGFloat
    private var badgeView: UIView?
    private var isButtonHidden: Bool = false
    
    // Shared promo code state across all instances
    private static var sharedPendingPromoCode: String?
    private static var copiedPromoCode: String?
    
    init(promotionData: NSDictionary?, onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?, badgeLabel: String? = nil, listTitle: String? = nil, badgeFontSize: CGFloat = 11.0) {
        self.promotionData = promotionData
        self.onPromotionClickCallback = onPromotionClickCallback
        self.badgeLabel = badgeLabel ?? "Offers"
        self.listTitle = listTitle ?? "Promotions"
        self.badgeFontSize = badgeFontSize
        super.init()
    }

    func setOnPromotionClickCallback(_ callback: @escaping (String, BrowserInterface?) -> Void) {
        self.onPromotionClickCallback = callback
    }

    func setupPromotionsBadge(for browser: BrowserInterface) {
        guard let promotionData = self.promotionData else { return }

        print("🔄 setupPromotionsBadge called, sharedPendingPromoCode: \(PromotionManager.sharedPendingPromoCode ?? "nil")")

        self.currentBrowser = browser

        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        let featuredPromotion = promotionData["featuredPromotion"] as? NSDictionary
        let totalCount = promotions.count + (featuredPromotion != nil ? 1 : 0)
        
        if totalCount > 0 {
            let badgeView = PromotionUIFactory.createPromotionBadge(count: totalCount, fontSize: self.badgeFontSize, label: self.badgeLabel)
            self.badgeView = badgeView
            browser.header.customActionView = badgeView
            
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(promotionsBadgeTapped))
            badgeView.addGestureRecognizer(tapGesture)
            badgeView.isUserInteractionEnabled = true
            
            setupPositionMonitoring()
            
            // Use the new BrowserScrollEventBus
            BrowserScrollEventBus.shared.addVisibilityObserver(self)
            BrowserScrollEventBus.shared.startMonitoring(browser: browser)
            
            badgeView.alpha = 1.0
            isButtonHidden = false
        }
    }
    
    @objc private func promotionsBadgeTapped() {
        showPromotionsList()
    }
    
    // MARK: - ScrollVisibilityObserver Implementation
    
    func onScrollVisibilityChanged(_ event: ScrollVisibilityEvent) {
        
        // Only control our badge if it's the direct customActionView
        // If it's been wrapped by PictureInPictureManager, let that handle the visibility
        if let badgeView = self.badgeView,
           let browser = currentBrowser as? BrowserInterface,
           browser.header.customActionView === badgeView {
            setButtonVisibility(event.shouldShow)
        } else {
            print("📡 PromotionManager: Badge is wrapped by another manager, skipping visibility control")
        }
    }
    
    public func debugCheckPosition() {
        print("🔍 Manual position check triggered")
        BrowserScrollEventBus.shared.checkVisibilityNow()
    }
    
    public func forceHideButton() {
        print("🔍 Force hiding button for test")
        setButtonVisibility(false)
    }
    
    public func forceShowButton() {
        print("🔍 Force showing button for test")
        setButtonVisibility(true)
    }
    
    public func setButtonVisibility(_ visible: Bool) {
        guard let badgeView = self.badgeView else { return }
        
        isButtonHidden = !visible
        
        UIView.animate(withDuration: 0.3) {
            badgeView.alpha = visible ? 1.0 : 0.0
        }
        
        print("🔍 Button visibility set to: \(visible ? "visible" : "hidden")")
    }
    
    public func updateButtonPosition() {
        BrowserScrollEventBus.shared.checkVisibilityNow()
    }
    
    public func hideButton() {
        setButtonVisibility(false)
    }
    
    public func showButton() {
        setButtonVisibility(true)
    }
    
    private func savePromoCodeForPromotion(promotionId: String) {
        guard let promotionData = self.promotionData else {
            print("🔄 No promotion data available")
            return
        }
        
        PromotionManager.sharedPendingPromoCode = nil
        
        if let featuredPromotion = promotionData["featuredPromotion"] as? [String: Any],
           let featuredId = featuredPromotion["id"] as? String, featuredId == promotionId {
            let promoCode = featuredPromotion["couponCode"] as? String ?? featuredPromotion["code"] as? String
            if let code = promoCode, !code.isEmpty {
                PromotionManager.sharedPendingPromoCode = code
                print("🔄 Saved promo code for featured promotion: \(code)")
                return
            }
        }
        
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        for promotion in promotions {
            if let id = promotion["id"] as? String, id == promotionId {
                let promoCode = promotion["couponCode"] as? String ?? promotion["code"] as? String
                if let code = promoCode, !code.isEmpty {
                    PromotionManager.sharedPendingPromoCode = code
                    print("🔄 Saved promo code for promotion: \(code)")
                    return
                }
            }
        }
        
        print("🔄 No promo code found for promotion ID: \(promotionId)")
    }
    
    public func showPendingPromoCodeToast() {
        guard let promoCode = PromotionManager.sharedPendingPromoCode else {
            print("🔄 No pending promo code to show")
            return
        }
        
        UIPasteboard.general.string = promoCode
        PromotionManager.copiedPromoCode = promoCode
        print("🔄 Promo code copied: \(promoCode)")
        
        PersistentToastManager.shared.showToast(message: "✓ \(promoCode) copied", duration: 2.0)
        print("🔄 Toast shown immediately when Button SDK opened")
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            GlobalLoaderManager.shared.hideLoader()
            print("🔄 Loader hidden after 2 seconds (forced)")
        }
        
        PromotionManager.sharedPendingPromoCode = nil
    }
    
    public func showCopiedToastIfNeeded() {
        guard let copiedCode = PromotionManager.copiedPromoCode else {
            print("🔄 No copied promo code to show toast for")
            return
        }
        
        PersistentToastManager.shared.showToast(message: "✓ \(copiedCode) copied", duration: 2.0)
        print("🔄 Showed success toast for copied promo code: \(copiedCode)")
        
        PromotionManager.copiedPromoCode = nil
    }
    
    // MARK: - Position Monitoring
    
    private func setupPositionMonitoring() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(orientationChanged),
            name: UIDevice.orientationDidChangeNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(keyboardWillShow),
            name: UIResponder.keyboardWillShowNotification,
            object: nil
        )
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(keyboardWillHide),
            name: UIResponder.keyboardWillHideNotification,
            object: nil
        )
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        BrowserScrollEventBus.shared.removeVisibilityObserver(self)
    }
    
    @objc private func orientationChanged() {
        // Event bus handles orientation changes automatically
    }
    
    @objc private func appDidBecomeActive() {
        // Event bus handles app state changes automatically
    }
    
    @objc private func keyboardWillShow() {
        // Event bus handles keyboard events automatically
    }
    
    @objc private func keyboardWillHide() {
        // Event bus handles keyboard events automatically
    }
    
    private func showPromotionsList() {
        guard let promotionData = self.promotionData else { return }
        
        let promotionsVC = PromotionBottomSheetViewController()
        promotionsVC.promotionData = promotionData
        promotionsVC.listTitle = self.listTitle
        promotionsVC.onPromotionSelected = { [weak self] (promotionId: String) in
            self?.savePromoCodeForPromotion(promotionId: promotionId)
            
            if let promoCode = PromotionManager.sharedPendingPromoCode, !promoCode.isEmpty {
                GlobalLoaderManager.shared.showCopyLoader(promoCode: promoCode)
                print("🔄 Copy loader shown for promotion with code: \(promoCode)")
            } else {
                GlobalLoaderManager.shared.showLoader(message: "Loading promotion...")
                print("🔄 Generic loader shown for promotion without code")
                
                DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
                    GlobalLoaderManager.shared.hideLoader()
                    print("🔄 Generic loader hidden after 2 seconds (forced)")
                }
            }
            
            if let browser = self?.currentBrowser {
                print("🔄 Forcing browser close before promotion")
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    browser.dismiss()
                    
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        print("🔄 Executing promotion callback after forced browser close")
                        self?.onPromotionClickCallback?(promotionId, self?.currentBrowser)
                    }
                }
            } else {
                print("🔄 No current browser - executing promotion callback immediately")
                self?.onPromotionClickCallback?(promotionId, self?.currentBrowser)
            }
        }
        
        if let viewController = getTopMostViewController() {
            promotionsVC.modalPresentationStyle = UIModalPresentationStyle.pageSheet
            if #available(iOS 15.0, *) {
                if let sheet = promotionsVC.sheetPresentationController {
                    sheet.detents = [UISheetPresentationController.Detent.medium(), UISheetPresentationController.Detent.large()]
                    sheet.prefersGrabberVisible = true
                }
            }
            viewController.present(promotionsVC, animated: true)
        }
    }
    
    private func createPromotionActionTitle(promotion: [String: Any], isFeature: Bool, rewardText: String?) -> String {
        var title = promotion["title"] as? String ?? "Promotion"
        
        if let createdAt = promotion["createdAt"] as? String, isPromotionNew(createdAt) {
            title = "🆕 \(title)"
        }
        
        if isFeature {
            title = "⭐ \(title)"
        }
        
        if let reward = rewardText, !reward.isEmpty {
            title = "\(title)\n\(reward)"
        }
        
        if let code = promotion["code"] as? String, !code.isEmpty {
            title = "\(title)\nCode: \(code)"
        }
        
        return title
    }
    
    private func isPromotionNew(_ createdAt: String) -> Bool {
        let formatter = ISO8601DateFormatter()
        guard let createdDate = formatter.date(from: createdAt) else {
            return false
        }
        
        let twoDaysAgo = Calendar.current.date(byAdding: .day, value: -2, to: Date()) ?? Date()
        return createdDate >= twoDaysAgo
    }
    
    private func getCurrentViewController() -> UIViewController? {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let window = windowScene.windows.first else {
            return nil
        }
        
        var viewController = window.rootViewController
        while let presentedViewController = viewController?.presentedViewController {
            viewController = presentedViewController
        }
        
        return viewController
    }
    
    private func getTopMostViewController() -> UIViewController? {
        var keyWindow: UIWindow?
        
        if #available(iOS 13.0, *) {
            keyWindow = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }
        } else {
            keyWindow = UIApplication.shared.keyWindow
        }
        
        guard let window = keyWindow else {
            return getCurrentViewController()
        }
        
        var topViewController = window.rootViewController
        
        while let presentedViewController = topViewController?.presentedViewController {
            topViewController = presentedViewController
        }
        
        if let navigationController = topViewController as? UINavigationController {
            topViewController = navigationController.visibleViewController
        }
        
        if let tabBarController = topViewController as? UITabBarController {
            topViewController = tabBarController.selectedViewController
        }
        
        return topViewController
    }
}
