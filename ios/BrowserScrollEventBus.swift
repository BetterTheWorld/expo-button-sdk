import UIKit
import WebKit

// MARK: - ScrollVisibilityEvent

/// Event fired when scroll threshold is crossed
struct ScrollVisibilityEvent {
    let shouldShow: Bool
    let reason: String
}

// MARK: - ScrollVisibilitySubscriber Protocol

protocol ScrollVisibilitySubscriber: AnyObject {
    /// Called when scroll visibility changes
    func onScrollVisibilityChanged(_ event: ScrollVisibilityEvent)
    
    /// Unique identifier for this subscriber
    var subscriberId: String { get }
}

// MARK: - BrowserScrollEventBus

/// Centralized event bus for browser scroll detection
/// Monitors scroll position and emits visibility events to subscribers
class BrowserScrollEventBus: NSObject {
    
    // MARK: - Singleton
    
    static let shared = BrowserScrollEventBus()
    
    private override init() {
        super.init()
    }
    
    // MARK: - Properties
    
    private var subscribers: [String: ScrollVisibilitySubscriber] = [:]
    private weak var currentBrowser: AnyObject?
    private weak var webView: WKWebView?
    private var displayLink: CADisplayLink?
    private var isMonitoring = false
    
    // Scroll detection state
    private var currentVisibilityState = true // Start visible
    
    // MARK: - Public API
    
    /// Start monitoring scroll events for a browser
    func startMonitoring(browser: AnyObject) {
        print("🚌 ScrollEventBus: Starting monitoring for browser: \(type(of: browser))")
        
        stopMonitoring() // Stop previous monitoring
        
        currentBrowser = browser
        setupWebViewScrollMonitoring(browser: browser)
        setupHighFrequencyPositionCheck()
        isMonitoring = true
        
        // Initial check
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.checkScrollThreshold()
        }
    }
    
    /// Stop monitoring scroll events
    func stopMonitoring() {
        print("🚌 ScrollEventBus: Stopping monitoring")
        
        // Remove KVO observers
        if let webView = self.webView {
            webView.scrollView.removeObserver(self, forKeyPath: "contentOffset")
        }
        
        // Remove DisplayLink
        displayLink?.invalidate()
        displayLink = nil
        
        currentBrowser = nil
        webView = nil
        isMonitoring = false
    }
    
    /// Subscribe to scroll visibility events
    func subscribe(_ subscriber: ScrollVisibilitySubscriber) {
        subscribers[subscriber.subscriberId] = subscriber
        print("🚌 ScrollEventBus: Subscriber added: \(subscriber.subscriberId), total: \(subscribers.count)")
        
        // Send current state to new subscriber
        let event = ScrollVisibilityEvent(
            shouldShow: currentVisibilityState,
            reason: "Initial state for new subscriber"
        )
        subscriber.onScrollVisibilityChanged(event)
    }
    
    /// Unsubscribe from scroll visibility events
    func unsubscribe(subscriberId: String) {
        subscribers.removeValue(forKey: subscriberId)
        print("🚌 ScrollEventBus: Subscriber removed: \(subscriberId), remaining: \(subscribers.count)")
        
        // Stop monitoring if no subscribers
        if subscribers.isEmpty {
            stopMonitoring()
        }
    }
    
    /// Manual check (for testing or forced updates)
    func checkNow() {
        checkScrollThreshold()
    }
    
    // MARK: - Internal Implementation
    
    /// Setup webview scroll monitoring
    private func setupWebViewScrollMonitoring(browser: AnyObject) {
        print("🚌 ScrollEventBus: Setting up webview scroll monitoring")
        print("🚌 ScrollEventBus: Browser type: \(type(of: browser))")
        
        // Try to find webview in browser hierarchy
        if let browserView = browser as? UIView {
            print("🚌 ScrollEventBus: Browser is a UIView, searching for webview...")
            
            if let webView = findWebView(in: browserView) {
                print("🚌 ScrollEventBus: ✅ Found webview: \(webView)")
                
                self.webView = webView
                
                // Add scroll observer using KVO
                webView.scrollView.addObserver(self, forKeyPath: "contentOffset", options: [.new, .old], context: nil)
                print("🚌 ScrollEventBus: ✅ KVO observer added to webview scroll")
            } else {
                print("🚌 ScrollEventBus: ❌ Could not find webview in browser hierarchy")
            }
        } else {
            print("🚌 ScrollEventBus: ❌ Browser is not a UIView")
        }
    }
    
    /// Find webview in view hierarchy
    private func findWebView(in view: UIView?) -> WKWebView? {
        guard let view = view else { return nil }
        
        if let webView = view as? WKWebView {
            return webView
        }
        
        for subview in view.subviews {
            if let webView = findWebView(in: subview) {
                return webView
            }
        }
        
        return nil
    }
    
    /// Setup high-frequency position checking using DisplayLink
    private func setupHighFrequencyPositionCheck() {
        print("🚌 ScrollEventBus: Setting up DisplayLink for real-time position checking")
        
        displayLink = CADisplayLink(target: self, selector: #selector(displayLinkFired))
        displayLink?.add(to: .main, forMode: .common)
    }
    
    /// Called every frame (60fps) to check scroll threshold
    @objc private func displayLinkFired() {
        checkScrollThreshold()
    }
    
    /// KVO observer for webview scroll changes
    override func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey : Any]?, context: UnsafeMutableRawPointer?) {
        if keyPath == "contentOffset" {
            print("🚌 ScrollEventBus: Webview scroll detected, checking threshold")
            DispatchQueue.main.async {
                self.checkScrollThreshold()
            }
        }
    }
    
    /// Check scroll position and emit events if threshold is crossed
    private func checkScrollThreshold() {
        guard isMonitoring else { return }
        
        // Get safe area insets
        let safeAreaInsets = getSafeAreaInsets()
        
        // Determine if UI should be hidden based on scroll position
        let shouldHide = isInScrollHideZone(safeAreaInsets: safeAreaInsets)
        let shouldShow = !shouldHide
        
        // Only emit event if state changed
        if shouldShow != currentVisibilityState {
            currentVisibilityState = shouldShow
            
            let reason = shouldShow ? "Scroll position allows UI" : "Scroll position conflicts with system UI"
            let event = ScrollVisibilityEvent(shouldShow: shouldShow, reason: reason)
            
            print("🚌 ScrollEventBus: State change -> \(shouldShow ? "SHOW" : "HIDE") (\(reason))")
            
            // Emit event to all subscribers
            emitEvent(event)
        }
    }
    
    /// Check if current scroll position should hide UI elements
    /// Uses the proven logic from PromotionManager for precise detection
    private func isInScrollHideZone(safeAreaInsets: UIEdgeInsets) -> Bool {
        guard let window = UIApplication.shared.windows.first else {
            print("🚌 ScrollEventBus: No window found")
            return false
        }
        
        let statusBarHeight = safeAreaInsets.top
        let hasNotch = statusBarHeight > 24
        
        // Use header position detection (migrated from PromotionManager)
        // This checks if ANY header element would conflict with system UI
        if let headerView = getAnyHeaderView() {
            return isViewInNotchArea(viewFrame: headerView.convert(headerView.bounds, to: nil), 
                                   safeAreaInsets: safeAreaInsets, 
                                   window: window, 
                                   hasNotch: hasNotch)
        }
        
        // Fallback: use scroll offset if no header view available
        if let webView = self.webView {
            let scrollOffset = webView.scrollView.contentOffset.y
            let hideThreshold: CGFloat = hasNotch ? 50 : 20
            
            if scrollOffset > hideThreshold {
                return true // Hide UI when scrolled down
            }
        }
        
        // Default: show UI
        return false
    }
    
    /// Get any available header view for position detection
    private func getAnyHeaderView() -> UIView? {
        // Try to find a header element to check position
        if let browser = currentBrowser as? NSObject,
           browser.responds(to: Selector(("header"))),
           let header = browser.value(forKey: "header") as? NSObject,
           header.responds(to: Selector(("customActionView"))),
           let customActionView = header.value(forKey: "customActionView") as? UIView {
            return customActionView
        }
        return nil
    }
    
    /// Migrated from PromotionManager: Check if view is in notch area
    private func isViewInNotchArea(viewFrame: CGRect, safeAreaInsets: UIEdgeInsets, window: UIWindow, hasNotch: Bool) -> Bool {
        // CRITICAL: Check if view is actually visible on screen
        // If view's Y position is negative or very small, it means it scrolled out of view
        let isViewOffScreen = viewFrame.minY < -10  // View has scrolled up and out of view
        
        if isViewOffScreen {
            return true  // Hide view when it's scrolled out of view
        }
        
        // For devices WITH notch: also check if view intersects with notch area
        if hasNotch {
            let notchHeight = safeAreaInsets.top
            let notchArea = CGRect(x: 0, y: 0, width: window.bounds.width, height: notchHeight)
            let isInNotchArea = viewFrame.intersects(notchArea)
            
            return isInNotchArea
        }
        
        // Device without notch and view is on screen - show it
        return false
    }
    
    /// Get current safe area insets
    private func getSafeAreaInsets() -> UIEdgeInsets {
        if #available(iOS 11.0, *) {
            let window = UIApplication.shared.windows.first { $0.isKeyWindow }
            return window?.safeAreaInsets ?? UIEdgeInsets.zero
        }
        return UIEdgeInsets.zero
    }
    
    /// Emit event to all subscribers
    private func emitEvent(_ event: ScrollVisibilityEvent) {
        for (subscriberId, subscriber) in subscribers {
            print("🚌 ScrollEventBus: Sending event to \(subscriberId): \(event.shouldShow ? "SHOW" : "HIDE")")
            subscriber.onScrollVisibilityChanged(event)
        }
    }
    
    // MARK: - Cleanup
    
    deinit {
        stopMonitoring()
    }
}