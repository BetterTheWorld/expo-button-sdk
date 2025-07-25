import ExpoModulesCore
import Button
import UIKit
import WebKit

class PromotionManager: NSObject {
    
    private var promotionData: NSDictionary?
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?
    private weak var currentBrowser: BrowserInterface?
    private var badgeLabel: String
    private var listTitle: String
    private var badgeView: UIView?
    private var isButtonHidden: Bool = false
    private weak var webView: WKWebView?
    private var displayLink: CADisplayLink?
    
    init(promotionData: NSDictionary?, onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?, badgeLabel: String? = nil, listTitle: String? = nil) {
        self.promotionData = promotionData
        self.onPromotionClickCallback = onPromotionClickCallback
        self.badgeLabel = badgeLabel ?? "Offers"
        self.listTitle = listTitle ?? "Promotions"
        super.init()
    }
    
    func setOnPromotionClickCallback(_ callback: @escaping (String, BrowserInterface?) -> Void) {
        self.onPromotionClickCallback = callback
    }
    
    func setupPromotionsBadge(for browser: BrowserInterface) {
        guard let promotionData = self.promotionData else { return }
        
        // Store browser reference for dismiss functionality
        self.currentBrowser = browser
        
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        let featuredPromotion = promotionData["featuredPromotion"] as? NSDictionary
        let totalCount = promotions.count + (featuredPromotion != nil ? 1 : 0)
        
        if totalCount > 0 {
            let badgeView = createPromotionBadge(count: totalCount)
            self.badgeView = badgeView
            browser.header.customActionView = badgeView
            
            // Add tap gesture to badge
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(promotionsBadgeTapped))
            badgeView.addGestureRecognizer(tapGesture)
            badgeView.isUserInteractionEnabled = true
            
            // Setup observers for position monitoring
            setupPositionMonitoring()
            
            // Setup webview scroll monitoring
            setupWebViewScrollMonitoring(browser: browser)
            
            // Ensure button is visible by default
            badgeView.alpha = 1.0
            isButtonHidden = false
            
            // Check position after a short delay to ensure layout is complete
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.checkButtonPosition()
            }
        }
    }
    
    private func createPromotionBadge(count: Int) -> UIView {
        let containerView = UIView()
        containerView.backgroundColor = UIColor(red: 0.976, green: 0.976, blue: 0.984, alpha: 1.0)
        containerView.layer.cornerRadius = 13
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        // Create icon + text stack
        let iconView = createTagIcon()
        iconView.translatesAutoresizingMaskIntoConstraints = false
        
        let titleLabel = UILabel()
        titleLabel.text = self.badgeLabel
        titleLabel.font = UIFont.systemFont(ofSize: 11)
        titleLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0) // #0b72ac
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addSubview(iconView)
        containerView.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            iconView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 6),
            iconView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 12),
            iconView.heightAnchor.constraint(equalToConstant: 12),
            
            titleLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 3),
            titleLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -6),
            titleLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            
            // Ensure containerView has appropriate height for pill design
            containerView.heightAnchor.constraint(equalToConstant: 26),
            titleLabel.topAnchor.constraint(greaterThanOrEqualTo: containerView.topAnchor, constant: 4),
            titleLabel.bottomAnchor.constraint(lessThanOrEqualTo: containerView.bottomAnchor, constant: -4)
        ])
        
        return containerView
    }
    
    private func createTagIcon() -> UIView {
        let iconView = UIView()
        iconView.backgroundColor = UIColor.clear
        
        let iconLayer = CAShapeLayer()
        let path = UIBezierPath()
        
        // Create exact SVG path from HTML (scaled to 12x12)
        let scale: CGFloat = 12.0 / 24.0 // Scale from 24x24 to 12x12
        
        // Main tag shape path: M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z
        path.move(to: CGPoint(x: 7 * scale, y: 3 * scale))
        path.addLine(to: CGPoint(x: 12 * scale, y: 3 * scale))
        path.addCurve(to: CGPoint(x: 12.586 * scale, y: 3.586 * scale), 
                     controlPoint1: CGPoint(x: 12.512 * scale, y: 3 * scale), 
                     controlPoint2: CGPoint(x: 12.391 * scale, y: 3.195 * scale))
        path.addLine(to: CGPoint(x: 19.586 * scale, y: 10.586 * scale))
        path.addCurve(to: CGPoint(x: 19.586 * scale, y: 13.414 * scale), 
                     controlPoint1: CGPoint(x: 20.367 * scale, y: 11.367 * scale), 
                     controlPoint2: CGPoint(x: 20.367 * scale, y: 12.633 * scale))
        path.addLine(to: CGPoint(x: 12.586 * scale, y: 20.414 * scale))
        path.addCurve(to: CGPoint(x: 9.758 * scale, y: 20.414 * scale), 
                     controlPoint1: CGPoint(x: 11.805 * scale, y: 21.195 * scale), 
                     controlPoint2: CGPoint(x: 10.539 * scale, y: 21.195 * scale))
        path.addLine(to: CGPoint(x: 2.758 * scale, y: 13.414 * scale))
        path.addCurve(to: CGPoint(x: 3 * scale, y: 12 * scale), 
                     controlPoint1: CGPoint(x: 2.464 * scale, y: 13.12 * scale), 
                     controlPoint2: CGPoint(x: 2.464 * scale, y: 12.288 * scale))
        path.addLine(to: CGPoint(x: 3 * scale, y: 7 * scale))
        path.addCurve(to: CGPoint(x: 7 * scale, y: 3 * scale), 
                     controlPoint1: CGPoint(x: 3 * scale, y: 4.791 * scale), 
                     controlPoint2: CGPoint(x: 4.791 * scale, y: 3 * scale))
        path.close()
        
        // Add the small dot: M7 7h.01
        let dotPath = UIBezierPath(ovalIn: CGRect(x: 7 * scale - 0.5, y: 7 * scale - 0.5, width: 1, height: 1))
        path.append(dotPath)
        
        iconLayer.path = path.cgPath
        iconLayer.fillColor = UIColor.clear.cgColor
        iconLayer.strokeColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0).cgColor // #0b72ac
        iconLayer.lineWidth = 2.0 * scale
        iconLayer.lineCap = .round
        iconLayer.lineJoin = .round
        
        iconView.layer.addSublayer(iconLayer)
        
        return iconView
    }
    
    @objc private func promotionsBadgeTapped() {
        showPromotionsList()
    }
    
    // MARK: - Position Detection Methods
    
    /// Check if the button is positioned too close to system icons
    private func checkButtonPosition() {
        guard let badgeView = self.badgeView else { 
            return 
        }
        
        // Get the button's frame in window coordinates
        let buttonFrame = badgeView.convert(badgeView.bounds, to: nil)
        let safeAreaInsets = getSafeAreaInsets()
        
        // Check if button conflicts with system areas (notch area)
        let shouldHide = isButtonInNotchArea(buttonFrame: buttonFrame, safeAreaInsets: safeAreaInsets)
        
        // Only update visibility if it changed
        if shouldHide != isButtonHidden {
            print("🔍 Button visibility changing: \(isButtonHidden ? "hidden" : "visible") -> \(shouldHide ? "hidden" : "visible")")
            setButtonVisibility(!shouldHide)
        }
    }
    
    /// Setup webview scroll monitoring to detect when button gets behind notch
    private func setupWebViewScrollMonitoring(browser: BrowserInterface) {
        print("🔍 Setting up webview scroll monitoring")
        print("🔍 Browser type: \(type(of: browser))")
        
        // Try to find webview in browser hierarchy
        if let browserView = browser as? UIView {
            print("🔍 Browser is a UIView, searching for webview...")
            printViewHierarchy(browserView, level: 0)
            
            if let webView = findWebView(in: browserView) {
                print("🔍 ✅ Found webview: \(webView)")
                
                // Store reference to webview
                self.webView = webView
                
                // Add scroll observer using KVO
                webView.scrollView.addObserver(self, forKeyPath: "contentOffset", options: [.new, .old], context: nil)
                print("🔍 ✅ KVO observer added to webview scroll")
            } else {
                print("🔍 ❌ Could not find webview in browser hierarchy")
            }
        } else {
            print("🔍 ❌ Browser is not a UIView")
        }
        
        // Alternative: Set up high-frequency position checking
        setupHighFrequencyPositionCheck()
    }
    
    /// Print view hierarchy for debugging
    private func printViewHierarchy(_ view: UIView, level: Int) {
        let indent = String(repeating: "  ", count: level)
        print("🔍 \(indent)\(type(of: view))")
        
        if view is WKWebView {
            print("🔍 \(indent)⭐ FOUND WEBVIEW!")
        }
        
        for subview in view.subviews {
            printViewHierarchy(subview, level: level + 1)
        }
    }
    
    /// Setup high-frequency position checking using DisplayLink for smooth scrolling
    private func setupHighFrequencyPositionCheck() {
        print("🔍 Setting up DisplayLink for real-time position checking")
        
        // Use DisplayLink for 60fps checking (most responsive)
        displayLink = CADisplayLink(target: self, selector: #selector(displayLinkFired))
        displayLink?.add(to: .main, forMode: .common)
        
        // Also setup timer as backup
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { [weak self] timer in
            guard let self = self else {
                timer.invalidate()
                return
            }
            
            self.checkButtonPosition()
        }
    }
    
    /// Called every frame (60fps) to check button position
    @objc private func displayLinkFired() {
        checkButtonPosition()
    }
    
    /// Find the webview in the browser view hierarchy
    private func findWebView(in view: UIView?) -> WKWebView? {
        guard let view = view else { return nil }
        
        // Check if current view is a webview
        if let webView = view as? WKWebView {
            return webView
        }
        
        // Search in subviews
        for subview in view.subviews {
            if let webView = findWebView(in: subview) {
                return webView
            }
        }
        
        return nil
    }
    
    /// KVO observer for webview scroll changes
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "contentOffset" {
            print("🔍 Webview scroll detected, checking button position")
            DispatchQueue.main.async {
                self.checkButtonPosition()
            }
        }
    }
    
    /// Get the current safe area insets for the app
    private func getSafeAreaInsets() -> UIEdgeInsets {
        if #available(iOS 11.0, *) {
            let window = UIApplication.shared.windows.first { $0.isKeyWindow }
            return window?.safeAreaInsets ?? UIEdgeInsets.zero
        }
        return UIEdgeInsets.zero
    }
    
    /// Check if the button is in the notch area (where system icons are)
    private func isButtonInNotchArea(buttonFrame: CGRect, safeAreaInsets: UIEdgeInsets) -> Bool {
        guard let window = UIApplication.shared.windows.first else { 
            print("🔍 No window found")
            return false 
        }
        
        let screenBounds = window.bounds
        let statusBarHeight = safeAreaInsets.top
        
        // Get the actual notch/status bar height
        let notchHeight = max(statusBarHeight, 44) // At least 44px for status bar
        
        // The notch area is the entire top area where system icons live
        let notchArea = CGRect(
            x: 0,
            y: 0,
            width: screenBounds.width,
            height: notchHeight
        )
        
        // Check if button overlaps with notch area
        let isInNotchArea = buttonFrame.intersects(notchArea)
        
        // ALSO check if button is in the top portion of the screen (simplified check)
        let isInTopArea = buttonFrame.minY < notchHeight
        
        // Hide if button is in the notch area OR top area
        let shouldHide = isInNotchArea || isInTopArea
        
        return shouldHide
    }
    
    /// Force check position now (for testing)
    public func debugCheckPosition() {
        print("🔍 Manual position check triggered")
        checkButtonPosition()
    }
    
    /// Force hide button for testing
    public func forceHideButton() {
        print("🔍 Force hiding button for test")
        setButtonVisibility(false)
    }
    
    /// Force show button for testing
    public func forceShowButton() {
        print("🔍 Force showing button for test")
        setButtonVisibility(true)
    }
    
    /// Set the visibility of the promotion button
    public func setButtonVisibility(_ visible: Bool) {
        guard let badgeView = self.badgeView else { return }
        
        isButtonHidden = !visible
        
        UIView.animate(withDuration: 0.3) {
            badgeView.alpha = visible ? 1.0 : 0.0
        }
        
        print("🔍 Button visibility set to: \(visible ? "visible" : "hidden")")
    }
    
    /// Public method to manually check and update button position
    public func updateButtonPosition() {
        checkButtonPosition()
    }
    
    /// Public method to force hide/show the button
    public func hideButton() {
        setButtonVisibility(false)
    }
    
    public func showButton() {
        setButtonVisibility(true)
    }
    
    // MARK: - Position Monitoring
    
    /// Setup observers for orientation changes and other events that might affect button position
    private func setupPositionMonitoring() {
        // Monitor orientation changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(orientationChanged),
            name: UIDevice.orientationDidChangeNotification,
            object: nil
        )
        
        // Monitor app state changes
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidBecomeActive),
            name: UIApplication.didBecomeActiveNotification,
            object: nil
        )
        
        // Monitor keyboard appearance (in case it affects layout)
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
    
    /// Remove all observers when the manager is deallocated
    deinit {
        NotificationCenter.default.removeObserver(self)
        
        // Remove KVO observers
        if let webView = self.webView {
            webView.scrollView.removeObserver(self, forKeyPath: "contentOffset")
        }
        
        // Remove DisplayLink
        displayLink?.invalidate()
        displayLink = nil
    }
    
    @objc private func orientationChanged() {
        // Wait for orientation change to complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.checkButtonPosition()
        }
    }
    
    @objc private func appDidBecomeActive() {
        // Re-check position when app becomes active
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.checkButtonPosition()
        }
    }
    
    @objc private func keyboardWillShow() {
        // Check if keyboard affects button position
        checkButtonPosition()
    }
    
    @objc private func keyboardWillHide() {
        // Check if button needs to be shown again
        checkButtonPosition()
    }
    
    private func showPromotionsList() {
        guard let promotionData = self.promotionData else { return }
        
        let promotionsVC = PromotionBottomSheetViewController()
        promotionsVC.promotionData = promotionData
        promotionsVC.listTitle = self.listTitle
        promotionsVC.onPromotionSelected = { [weak self] (promotionId: String) in
            // Show loader immediately when promotion is tapped
            GlobalLoaderManager.shared.showLoader(message: "Loading promotion...")
            print("🔄 Global loader shown for promotion")
            
            // FORCE close current browser first, then execute callback
            if let browser = self?.currentBrowser {
                print("🔄 Forcing browser close before promotion")
                browser.dismiss() // Force immediate close
                
                // Wait for browser to fully close, then execute callback
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    print("🔄 Executing promotion callback after forced browser close")
                    self?.onPromotionClickCallback?(promotionId, self?.currentBrowser)
                }
            } else {
                // No current browser, execute immediately
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
        
        // Add NEW badge if promotion is new
        if let createdAt = promotion["createdAt"] as? String, isPromotionNew(createdAt) {
            title = "🆕 \(title)"
        }
        
        // Add feature indicator
        if isFeature {
            title = "⭐ \(title)"
        }
        
        // Add reward text if available
        if let reward = rewardText, !reward.isEmpty {
            title = "\(title)\n\(reward)"
        }
        
        // Add coupon code if available
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
        // Try to find the key window first
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
        
        // Navigate to the topmost presented view controller
        while let presentedViewController = topViewController?.presentedViewController {
            topViewController = presentedViewController
        }
        
        // If it's a navigation controller, get the visible view controller
        if let navigationController = topViewController as? UINavigationController {
            topViewController = navigationController.visibleViewController
        }
        
        // If it's a tab bar controller, get the selected view controller
        if let tabBarController = topViewController as? UITabBarController {
            topViewController = tabBarController.selectedViewController
        }
        
        return topViewController
    }
}

// MARK: - PromotionBottomSheetViewController
// Bottom Sheet component - equivalent to Android's PromotionBottomSheet.kt
class PromotionBottomSheetViewController: UIViewController {
    
    var promotionData: NSDictionary?
    var onPromotionSelected: ((String) -> Void)?
    var listTitle: String = "Promotions"
    
    private let scrollView = UIScrollView()
    private let contentView = UIView()
    private let stackView = UIStackView()
    private var promotionIdMap: [Int: String] = [:]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupUI()
        setupPromotions()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor.systemBackground
        
        // Add title
        let titleLabel = UILabel()
        titleLabel.text = listTitle
        titleLabel.font = UIFont.boldSystemFont(ofSize: 18)
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        // Setup scroll view
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        contentView.translatesAutoresizingMaskIntoConstraints = false
        
        // Setup stack view
        stackView.axis = .vertical
        stackView.spacing = 16  // Match Android card spacing
        stackView.translatesAutoresizingMaskIntoConstraints = false
        
        // Add views
        view.addSubview(titleLabel)
        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        contentView.addSubview(stackView)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            // Title
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            
            // Scroll view
            scrollView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 20),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            
            // Content view
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            
            // Stack view
            stackView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 16),
            stackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            stackView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -30)
        ])
    }
    
    private func setupPromotions() {
        guard let promotionData = self.promotionData else { return }
        
        let rewardText = promotionData["rewardText"] as? String
        
        // Add featured promotion if available
        if let featuredPromotion = promotionData["featuredPromotion"] as? [String: Any] {
            let promotionView = createPromotionView(promotion: featuredPromotion, isFeature: true, rewardText: rewardText)
            stackView.addArrangedSubview(promotionView)
        }
        
        // Add regular promotions
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        for promotion in promotions {
            let promotionView = createPromotionView(promotion: promotion, isFeature: false, rewardText: rewardText)
            stackView.addArrangedSubview(promotionView)
        }
    }
    
    private func createPromotionView(promotion: [String: Any], isFeature: Bool, rewardText: String?) -> UIView {
        // Card container with Android-like design
        let cardContainer = UIView()
        cardContainer.backgroundColor = UIColor.systemBackground
        cardContainer.layer.cornerRadius = 12
        cardContainer.layer.borderWidth = 1
        cardContainer.layer.borderColor = UIColor(red: 0.898, green: 0.906, blue: 0.922, alpha: 1.0).cgColor // #E5E7EB
        cardContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Main container with margins
        let containerView = UIView()
        containerView.translatesAutoresizingMaskIntoConstraints = false
        cardContainer.addSubview(containerView)
        
        // Create main content stack
        let mainStack = UIStackView()
        mainStack.axis = .vertical
        mainStack.spacing = 12
        mainStack.translatesAutoresizingMaskIntoConstraints = false
        
        // Title container with time label
        let titleContainer = UIStackView()
        titleContainer.axis = .horizontal
        titleContainer.alignment = .center
        titleContainer.spacing = 8
        titleContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Get promotion data and clean it from existing time labels
        var title = promotion["description"] as? String ?? promotion["title"] as? String ?? "Promotion"
        
        // Calculate time label based on startsAt date (like Android)
        var timeLabel: String? = nil
        if let startsAt = promotion["startsAt"] as? String {
            let startDiff = calculateDaysDifference(startsAt)
            print("🔍 iOS Debug - Promotion: \(promotion["id"] ?? "unknown"), startsAt: \(startsAt), daysDiff: \(startDiff)")
            if startDiff < 3 {
                timeLabel = "NEW!"
                print("🔍 iOS Debug - Setting label to NEW!")
            } else if startDiff <= 7 { // Changed from < 7 to <= 7
                timeLabel = "THIS WEEK!"
                print("🔍 iOS Debug - Setting label to THIS WEEK!")
            }
        }
        
        // Remove time labels from title if they exist (like Android)
        let timeLabels = ["THIS WEEK!", "NEW!", "TODAY!"]
        for label in timeLabels {
            title = title.replacingOccurrences(of: label, with: "").trimmingCharacters(in: .whitespaces)
        }
        
        // Add time label if found
        if let label = timeLabel {
            print("🔍 iOS Debug - Creating label view for: \(label)")
            let labelView = UILabel()
            labelView.text = label
            labelView.font = UIFont.systemFont(ofSize: 12, weight: .bold)
            labelView.textColor = UIColor(red: 0.018, green: 0.471, blue: 0.341, alpha: 1.0) // #047857 emerald-700
            labelView.backgroundColor = UIColor(red: 0.925, green: 0.992, blue: 0.961, alpha: 1.0) // #ECFDF5 emerald-50
            labelView.layer.cornerRadius = 4
            labelView.layer.masksToBounds = true
            labelView.textAlignment = .center
            print("🔍 iOS Debug - Label view created with green background")
            
            // Add padding
            labelView.translatesAutoresizingMaskIntoConstraints = false
            let paddingView = UIView()
            paddingView.addSubview(labelView)
            paddingView.translatesAutoresizingMaskIntoConstraints = false
            
            NSLayoutConstraint.activate([
                labelView.leadingAnchor.constraint(equalTo: paddingView.leadingAnchor, constant: 8),
                labelView.trailingAnchor.constraint(equalTo: paddingView.trailingAnchor, constant: -8),
                labelView.topAnchor.constraint(equalTo: paddingView.topAnchor, constant: 2),
                labelView.bottomAnchor.constraint(equalTo: paddingView.bottomAnchor, constant: -2)
            ])
            
            titleContainer.addArrangedSubview(paddingView)
        }
        
        // Title text
        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = UIFont.systemFont(ofSize: 16)
        titleLabel.textColor = UIColor(red: 0.220, green: 0.255, blue: 0.318, alpha: 1.0) // #374151 gray-900
        titleLabel.numberOfLines = 2
        titleLabel.lineBreakMode = .byWordWrapping
        titleLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        titleContainer.addArrangedSubview(titleLabel)
        
        mainStack.addArrangedSubview(titleContainer)
        
        // Bottom section with cashback, promo code, and time
        let bottomContainer = UIStackView()
        bottomContainer.axis = .horizontal
        bottomContainer.alignment = .center
        bottomContainer.spacing = 8
        bottomContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Extract cashback from reward text or title
        var cashbackText = ""
        if let reward = rewardText, !reward.isEmpty {
            let cashbackRegex = try! NSRegularExpression(pattern: "(\\d+% Cashback)")
            let range = NSRange(location: 0, length: reward.count)
            if let match = cashbackRegex.firstMatch(in: reward, range: range) {
                cashbackText = String(reward[Range(match.range, in: reward)!])
            }
        }
        
        // Cashback text
        if !cashbackText.isEmpty {
            let cashbackLabel = UILabel()
            cashbackLabel.text = cashbackText
            cashbackLabel.font = UIFont.systemFont(ofSize: 14) // Larger than ends in (12), no bold
            cashbackLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0) // #0B72AC
            bottomContainer.addArrangedSubview(cashbackLabel)
        }
        
        // Promo code
        if let promoCode = promotion["couponCode"] as? String ?? promotion["code"] as? String, !promoCode.isEmpty {
            // Add bullet separator
            if !cashbackText.isEmpty {
                let bulletLabel = UILabel()
                bulletLabel.text = "•"
                bulletLabel.font = UIFont.systemFont(ofSize: 14)
                bulletLabel.textColor = UIColor(red: 0.419, green: 0.447, blue: 0.502, alpha: 1.0) // #6B7280 gray-500
                bottomContainer.addArrangedSubview(bulletLabel)
            }
            
            // Promo code button container with tag icon
            let promoButton = UIButton(type: .custom)
            promoButton.backgroundColor = UIColor(red: 0.937, green: 0.965, blue: 1.0, alpha: 1.0) // #EFF6FF blue-50
            promoButton.layer.cornerRadius = 8 // Increased from 4 for better button look
            promoButton.layer.masksToBounds = true
            promoButton.contentEdgeInsets = UIEdgeInsets(top: 6, left: 12, bottom: 6, right: 12) // Increased padding
            promoButton.translatesAutoresizingMaskIntoConstraints = false
            
            // Create button content stack
            let buttonStack = UIStackView()
            buttonStack.axis = .horizontal
            buttonStack.alignment = .center
            buttonStack.spacing = 4
            buttonStack.isUserInteractionEnabled = false // Let button handle interaction
            buttonStack.translatesAutoresizingMaskIntoConstraints = false
            
            // Add tag icon (same as header)
            let tagIcon = createTagIcon()
            tagIcon.translatesAutoresizingMaskIntoConstraints = false
            buttonStack.addArrangedSubview(tagIcon)
            
            // Add promo code text
            let promoLabel = UILabel()
            promoLabel.text = promoCode
            promoLabel.font = UIFont.systemFont(ofSize: 10) // Smaller font, no bold
            promoLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0) // #0B72AC
            buttonStack.addArrangedSubview(promoLabel)
            
            promoButton.addSubview(buttonStack)
            
            // Constraints for tag icon and button content
            NSLayoutConstraint.activate([
                tagIcon.widthAnchor.constraint(equalToConstant: 12),
                tagIcon.heightAnchor.constraint(equalToConstant: 12),
                
                // Button stack constraints
                buttonStack.centerXAnchor.constraint(equalTo: promoButton.centerXAnchor),
                buttonStack.centerYAnchor.constraint(equalTo: promoButton.centerYAnchor),
                buttonStack.leadingAnchor.constraint(greaterThanOrEqualTo: promoButton.leadingAnchor, constant: 12),
                buttonStack.trailingAnchor.constraint(lessThanOrEqualTo: promoButton.trailingAnchor, constant: -12)
            ])
            
            // Add button actions for press effects
            if let promotionId = promotion["id"] as? String {
                promoButton.addTarget(self, action: #selector(promoCodeTouchDown(_:)), for: .touchDown)
                promoButton.addTarget(self, action: #selector(promoCodeTouchUp(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel])
                promoButton.addTarget(self, action: #selector(promoCodeTapped(_:)), for: .touchUpInside)
                promoButton.tag = promotionId.hashValue
                promotionIdMap[promotionId.hashValue] = promotionId
            }
            
            bottomContainer.addArrangedSubview(promoButton)
        }
        
        // Time remaining
        if let endsAt = promotion["endsAt"] as? String, let timeRemaining = calculateTimeRemaining(endsAt) {
            // Add bullet separator
            if !cashbackText.isEmpty || (promotion["couponCode"] as? String ?? promotion["code"] as? String) != nil {
                let bulletLabel = UILabel()
                bulletLabel.text = "•"
                bulletLabel.font = UIFont.systemFont(ofSize: 14)
                bulletLabel.textColor = UIColor(red: 0.419, green: 0.447, blue: 0.502, alpha: 1.0) // #6B7280 gray-500
                bottomContainer.addArrangedSubview(bulletLabel)
            }
            
            let timeLabel = UILabel()
            timeLabel.text = timeRemaining
            timeLabel.font = UIFont.systemFont(ofSize: 12)
            timeLabel.textColor = UIColor(red: 0.419, green: 0.447, blue: 0.502, alpha: 1.0) // #6B7280 gray-500
            bottomContainer.addArrangedSubview(timeLabel)
        }
        
        // Add spacer to push content to left
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        bottomContainer.addArrangedSubview(spacer)
        
        mainStack.addArrangedSubview(bottomContainer)
        
        // Add tap gesture
        if let promotionId = promotion["id"] as? String {
            let tag = promotionId.hashValue
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(cardTapped(_:)))
            cardContainer.tag = tag
            promotionIdMap[tag] = promotionId
            cardContainer.addGestureRecognizer(tapGesture)
            cardContainer.isUserInteractionEnabled = true
        }
        
        containerView.addSubview(mainStack)
        
        NSLayoutConstraint.activate([
            // Card container margins (like Android)
            containerView.leadingAnchor.constraint(equalTo: cardContainer.leadingAnchor, constant: 16),
            containerView.trailingAnchor.constraint(equalTo: cardContainer.trailingAnchor, constant: -16),
            containerView.topAnchor.constraint(equalTo: cardContainer.topAnchor, constant: 16),
            containerView.bottomAnchor.constraint(equalTo: cardContainer.bottomAnchor, constant: -16),
            
            // Main stack
            mainStack.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            mainStack.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            mainStack.topAnchor.constraint(equalTo: containerView.topAnchor),
            mainStack.bottomAnchor.constraint(equalTo: containerView.bottomAnchor)
        ])
        
        return cardContainer
    }
    
    @objc private func cardTapped(_ gesture: UITapGestureRecognizer) {
        guard let view = gesture.view,
              let promotionId = promotionIdMap[view.tag] else { return }
        
        dismiss(animated: true) {
            self.onPromotionSelected?(promotionId)
        }
    }
    
    @objc private func promoCodeTouchDown(_ sender: UIButton) {
        // Scale down effect on touch down
        UIView.animate(withDuration: 0.1, animations: {
            sender.transform = CGAffineTransform(scaleX: 0.95, y: 0.95)
            sender.alpha = 0.8
        })
    }
    
    @objc private func promoCodeTouchUp(_ sender: UIButton) {
        // Scale back to normal
        UIView.animate(withDuration: 0.1, animations: {
            sender.transform = .identity
            sender.alpha = 1.0
        })
    }

    @objc private func promoCodeTapped(_ sender: UIButton) {
        guard let promotionId = promotionIdMap[sender.tag] else { return }
        
        // Get promo code from promotion data
        let promoCode = getPromoCodeForPromotionId(promotionId)
        guard let code = promoCode, !code.isEmpty else { return }
        
        // Copy to clipboard
        UIPasteboard.general.string = code
        
        // Show native alert
        showCopiedAlert(promoCode: code)
    }
    
    private func getPromoCodeForPromotionId(_ promotionId: String) -> String? {
        guard let promotionData = self.promotionData else { return nil }
        
        // Check featured promotion
        if let featuredPromotion = promotionData["featuredPromotion"] as? [String: Any],
           let id = featuredPromotion["id"] as? String, id == promotionId {
            return featuredPromotion["couponCode"] as? String ?? featuredPromotion["code"] as? String
        }
        
        // Check regular promotions
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        for promotion in promotions {
            if let id = promotion["id"] as? String, id == promotionId {
                return promotion["couponCode"] as? String ?? promotion["code"] as? String
            }
        }
        
        return nil
    }
    
    private func showCopiedAlert(promoCode: String) {
        let alert = UIAlertController(
            title: "Coupon Copied",
            message: "Your promotion code \(promoCode) was copied",
            preferredStyle: .alert
        )
        
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        
        present(alert, animated: true)
    }
    
    private func createTagIcon() -> UIView {
        let iconView = UIView()
        iconView.backgroundColor = UIColor.clear
        
        let iconLayer = CAShapeLayer()
        let path = UIBezierPath()
        
        // Create exact SVG path from HTML (scaled to 12x12)
        let scale: CGFloat = 12.0 / 24.0 // Scale from 24x24 to 12x12
        
        // Main tag shape path: M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z
        path.move(to: CGPoint(x: 7 * scale, y: 3 * scale))
        path.addLine(to: CGPoint(x: 12 * scale, y: 3 * scale))
        path.addCurve(to: CGPoint(x: 12.586 * scale, y: 3.586 * scale), 
                     controlPoint1: CGPoint(x: 12.512 * scale, y: 3 * scale), 
                     controlPoint2: CGPoint(x: 12.391 * scale, y: 3.195 * scale))
        path.addLine(to: CGPoint(x: 19.586 * scale, y: 10.586 * scale))
        path.addCurve(to: CGPoint(x: 19.586 * scale, y: 13.414 * scale), 
                     controlPoint1: CGPoint(x: 20.367 * scale, y: 11.367 * scale), 
                     controlPoint2: CGPoint(x: 20.367 * scale, y: 12.633 * scale))
        path.addLine(to: CGPoint(x: 12.586 * scale, y: 20.414 * scale))
        path.addCurve(to: CGPoint(x: 9.758 * scale, y: 20.414 * scale), 
                     controlPoint1: CGPoint(x: 11.805 * scale, y: 21.195 * scale), 
                     controlPoint2: CGPoint(x: 10.539 * scale, y: 21.195 * scale))
        path.addLine(to: CGPoint(x: 2.758 * scale, y: 13.414 * scale))
        path.addCurve(to: CGPoint(x: 3 * scale, y: 12 * scale), 
                     controlPoint1: CGPoint(x: 2.464 * scale, y: 13.12 * scale), 
                     controlPoint2: CGPoint(x: 2.464 * scale, y: 12.288 * scale))
        path.addLine(to: CGPoint(x: 3 * scale, y: 7 * scale))
        path.addCurve(to: CGPoint(x: 7 * scale, y: 3 * scale), 
                     controlPoint1: CGPoint(x: 3 * scale, y: 4.791 * scale), 
                     controlPoint2: CGPoint(x: 4.791 * scale, y: 3 * scale))
        path.close()
        
        // Add the small dot: M7 7h.01
        let dotPath = UIBezierPath(ovalIn: CGRect(x: 7 * scale - 0.5, y: 7 * scale - 0.5, width: 1, height: 1))
        path.append(dotPath)
        
        iconLayer.path = path.cgPath
        iconLayer.fillColor = UIColor.clear.cgColor
        iconLayer.strokeColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0).cgColor // #0b72ac
        iconLayer.lineWidth = 1.0 * scale // Thin stroke like Android
        iconLayer.lineCap = .round
        iconLayer.lineJoin = .round
        
        iconView.layer.addSublayer(iconLayer)
        
        return iconView
    }
    
    private func calculateDaysDifference(_ dateStr: String) -> Int {
        let formatter = ISO8601DateFormatter()
        guard let startDate = formatter.date(from: dateStr) else {
            return Int.max
        }
        
        let now = Date()
        let calendar = Calendar.current
        
        let nowComponents = calendar.dateComponents([.year, .month, .day], from: now)
        let startComponents = calendar.dateComponents([.year, .month, .day], from: startDate)
        
        guard let nowDate = calendar.date(from: nowComponents),
              let startDateOnly = calendar.date(from: startComponents) else {
            return Int.max
        }
        
        let daysDiff = calendar.dateComponents([.day], from: startDateOnly, to: nowDate).day ?? Int.max
        return abs(daysDiff)
    }
    
    private func calculateTimeRemaining(_ endsAt: String) -> String? {
        let formatter = ISO8601DateFormatter()
        guard let expirationDate = formatter.date(from: endsAt) else {
            return nil
        }
        
        let now = Date()
        let timeDiff = expirationDate.timeIntervalSince(now)
        
        // If already expired, don't show time remaining
        if timeDiff <= 0 { return nil }
        
        let days = Int(timeDiff / (24 * 60 * 60))
        
        // Hide if ends in less than 1 week (7 days)
        if days < 7 { return nil }
        
        let hours = Int((timeDiff.truncatingRemainder(dividingBy: 24 * 60 * 60)) / (60 * 60))
        
        switch (days, hours) {
        case (let d, _) where d > 1:
            return "ends in \(d)d"
        case (1, _):
            return "ends tomorrow"
        case (0, let h) where h > 1:
            return "ends in \(h)h"
        case (0, 1):
            return "ends in 1h"
        default:
            return "ends today"
        }
    }
}
