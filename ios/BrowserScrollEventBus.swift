import UIKit
import WebKit
import ExpoModulesCore

// MARK: - Scroll Event Types

protocol ScrollEvent {
    var timestamp: TimeInterval { get }
    var source: ScrollEventSource { get }
}

enum ScrollEventSource {
    case webView(WKWebView)
    case displayLink
    case keyValueObserver
    case userTriggered
    case orientationChange
    case keyboardShow
    case keyboardHide
}

struct WebViewScrollEvent: ScrollEvent {
    let timestamp: TimeInterval
    let source: ScrollEventSource
    let contentOffset: CGPoint
    let previousOffset: CGPoint
    let contentSize: CGSize
    let visibleBounds: CGRect
    let velocity: CGPoint
    let deceleration: CGFloat
    
    var scrollDirection: ScrollDirection {
        let deltaY = contentOffset.y - previousOffset.y
        let deltaX = contentOffset.x - previousOffset.x
        
        if abs(deltaY) > abs(deltaX) {
            return deltaY > 0 ? .down : .up
        } else {
            return deltaX > 0 ? .right : .left
        }
    }
    
    var isScrolling: Bool {
        return contentOffset != previousOffset
    }
    
    var scrollDistance: CGFloat {
        let deltaY = contentOffset.y - previousOffset.y
        let deltaX = contentOffset.x - previousOffset.x
        return sqrt(deltaY * deltaY + deltaX * deltaX)
    }
}

enum ScrollDirection {
    case up, down, left, right, none
}

struct ScrollVisibilityEvent: ScrollEvent {
    let timestamp: TimeInterval
    let source: ScrollEventSource
    let shouldShow: Bool
    let reason: String
    let triggerFrame: CGRect?
    let safeAreaInsets: UIEdgeInsets
    let hasNotch: Bool
}

// MARK: - Observer Protocols

protocol ScrollEventObserver: AnyObject {
    var observerId: String { get }
    func onScrollEvent(_ event: WebViewScrollEvent)
    func onScrollStarted(_ event: WebViewScrollEvent)
    func onScrollEnded(_ event: WebViewScrollEvent)
    func onScrollDirectionChanged(_ event: WebViewScrollEvent, newDirection: ScrollDirection)
}

protocol ScrollVisibilityObserver: AnyObject {
    var observerId: String { get }
    func onScrollVisibilityChanged(_ event: ScrollVisibilityEvent)
}

extension ScrollEventObserver {
    var observerId: String {
        return String(describing: type(of: self)) + "_" + String(ObjectIdentifier(self).hashValue)
    }
    
    // Default implementations for optional methods
    func onScrollDirectionChanged(_ event: WebViewScrollEvent, newDirection: ScrollDirection) {}
}

extension ScrollVisibilityObserver {
    var observerId: String {
        return String(describing: type(of: self)) + "_" + String(ObjectIdentifier(self).hashValue)
    }
}

// MARK: - Event Bus Implementation

final class BrowserScrollEventBus: NSObject {
    
    // MARK: - Singleton
    static let shared = BrowserScrollEventBus()
    
    // MARK: - Properties
    private var scrollObservers: [String: WeakScrollObserverWrapper] = [:]
    private var visibilityObservers: [String: WeakVisibilityObserverWrapper] = [:]
    private var monitoredWebViews: [WeakWebViewWrapper] = []
    private var displayLink: CADisplayLink?
    private var isDisplayLinkActive: Bool = false
    
    // Scroll state tracking
    private var lastScrollEvents: [ObjectIdentifier: WebViewScrollEvent] = [:]
    private var scrollEndTimers: [ObjectIdentifier: Timer] = [:]
    private var lastScrollDirections: [ObjectIdentifier: ScrollDirection] = [:]
    private let scrollEndDelay: TimeInterval = 0.15
    
    // Visibility state tracking
    private var currentVisibilityState = true
    private var lastVisibilityCheckTime: TimeInterval = 0
    private let visibilityCheckThrottle: TimeInterval = 1.0 / 60.0 // 60fps max
    
    // Performance optimization
    private let maxObserverCount: Int = 50
    private let cleanupInterval: TimeInterval = 30.0
    private var lastCleanupTime: TimeInterval = 0
    
    // Browser tracking
    private weak var currentBrowser: AnyObject?
    
    private override init() {
        super.init()
        setupNotificationObservers()
    }
    
    deinit {
        stopAllMonitoring()
        removeNotificationObservers()
    }
    
    // MARK: - Public API
    
    func addScrollObserver(_ observer: ScrollEventObserver) {
        cleanupStaleObserversIfNeeded()
        
        guard scrollObservers.count < maxObserverCount else {
            print("⚠️ ScrollEventBus: Maximum scroll observer count reached. Skipping: \(observer.observerId)")
            return
        }
        
        scrollObservers[observer.observerId] = WeakScrollObserverWrapper(observer)
        print("📡 ScrollEventBus: Added scroll observer: \(observer.observerId)")
        
        startDisplayLinkIfNeeded()
    }
    
    func removeScrollObserver(_ observer: ScrollEventObserver) {
        scrollObservers.removeValue(forKey: observer.observerId)
        print("📡 ScrollEventBus: Removed scroll observer: \(observer.observerId)")
        
        stopDisplayLinkIfNotNeeded()
    }
    
    func addVisibilityObserver(_ observer: ScrollVisibilityObserver) {
        cleanupStaleObserversIfNeeded()
        
        guard visibilityObservers.count < maxObserverCount else {
            print("⚠️ ScrollEventBus: Maximum visibility observer count reached. Skipping: \(observer.observerId)")
            return
        }
        
        visibilityObservers[observer.observerId] = WeakVisibilityObserverWrapper(observer)
        print("📡 ScrollEventBus: Added visibility observer: \(observer.observerId)")
        
        // Send current state to new observer after a small delay to let views settle
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            self?.sendCurrentVisibilityState(to: observer)
        }
        
        startDisplayLinkIfNeeded()
    }
    
    func removeVisibilityObserver(_ observer: ScrollVisibilityObserver) {
        visibilityObservers.removeValue(forKey: observer.observerId)
        print("📡 ScrollEventBus: Removed visibility observer: \(observer.observerId)")
        
        stopDisplayLinkIfNotNeeded()
    }
    
    func startMonitoring(webView: WKWebView) {
        let webViewId = ObjectIdentifier(webView)
        
        if monitoredWebViews.contains(where: { $0.webView === webView }) {
            print("📡 ScrollEventBus: Already monitoring webView: \(webViewId)")
            return
        }
        
        monitoredWebViews.append(WeakWebViewWrapper(webView))
        
        webView.scrollView.addObserver(
            self,
            forKeyPath: "contentOffset",
            options: [.new, .old],
            context: UnsafeMutableRawPointer(bitPattern: webViewId.hashValue)
        )
        
        print("📡 ScrollEventBus: Started monitoring webView: \(webViewId)")
        
        let initialEvent = createScrollEvent(
            for: webView,
            newOffset: webView.scrollView.contentOffset,
            oldOffset: webView.scrollView.contentOffset,
            source: .keyValueObserver
        )
        lastScrollEvents[webViewId] = initialEvent
        lastScrollDirections[webViewId] = .none
        
        startDisplayLinkIfNeeded()
    }
    
    func startMonitoring(browser: AnyObject) {
        // Don't stop previous browser - allow multiple browsers to be monitored
        // Only update currentBrowser for legacy compatibility
        currentBrowser = browser
        
        if let browserView = browser as? UIView {
            if let webView = findWebView(in: browserView) {
                startMonitoring(webView: webView)
            }
        }
        
        checkVisibilityThreshold(source: .userTriggered)
    }
    
    func stopMonitoring(webView: WKWebView) {
        let webViewId = ObjectIdentifier(webView)
        
        monitoredWebViews.removeAll { $0.webView === webView }
        
        webView.scrollView.removeObserver(self, forKeyPath: "contentOffset")
        
        lastScrollEvents.removeValue(forKey: webViewId)
        lastScrollDirections.removeValue(forKey: webViewId)
        scrollEndTimers[webViewId]?.invalidate()
        scrollEndTimers.removeValue(forKey: webViewId)
        
        print("📡 ScrollEventBus: Stopped monitoring webView: \(webViewId)")
        
        stopDisplayLinkIfNotNeeded()
    }
    
    func stopMonitoring(browser: AnyObject?) {
        if let browser = browser as? UIView {
            if let webView = findWebView(in: browser) {
                stopMonitoring(webView: webView)
            }
        }
        
        if browser === currentBrowser {
            currentBrowser = nil
        }
    }
    
    func stopAllMonitoring() {
        let webViewsCopy = monitoredWebViews.compactMap { $0.webView }
        for webView in webViewsCopy {
            stopMonitoring(webView: webView)
        }
        
        stopDisplayLink()
        currentBrowser = nil
        scrollObservers.removeAll()
        visibilityObservers.removeAll()
        lastScrollEvents.removeAll()
        lastScrollDirections.removeAll()
        scrollEndTimers.values.forEach { $0.invalidate() }
        scrollEndTimers.removeAll()
        
        print("📡 ScrollEventBus: Stopped all monitoring")
    }
    
    func checkVisibilityNow() {
        checkVisibilityThreshold(source: .userTriggered)
    }
    
    func getActiveObserverCount() -> (scroll: Int, visibility: Int) {
        cleanupStaleObserversIfNeeded()
        return (scrollObservers.count, visibilityObservers.count)
    }
    
    func getMonitoredWebViewCount() -> Int {
        cleanupStaleWebViewsIfNeeded()
        return monitoredWebViews.count
    }
    
    // MARK: - Display Link Management
    
    private func startDisplayLinkIfNeeded() {
        guard !isDisplayLinkActive && (!scrollObservers.isEmpty || !visibilityObservers.isEmpty) else { return }
        
        displayLink = CADisplayLink(target: self, selector: #selector(displayLinkFired))
        displayLink?.add(to: .main, forMode: .common)
        isDisplayLinkActive = true
        
        print("📡 ScrollEventBus: Started DisplayLink monitoring")
    }
    
    private func stopDisplayLinkIfNotNeeded() {
        guard isDisplayLinkActive && scrollObservers.isEmpty && visibilityObservers.isEmpty else { return }
        stopDisplayLink()
    }
    
    private func stopDisplayLink() {
        displayLink?.invalidate()
        displayLink = nil
        isDisplayLinkActive = false
        print("📡 ScrollEventBus: Stopped DisplayLink monitoring")
    }
    
    @objc private func displayLinkFired() {
        let currentTime = Date().timeIntervalSince1970
        
        // Check scroll events
        let activeWebViews = monitoredWebViews.compactMap { $0.webView }
        for webView in activeWebViews {
            checkScrollStateWithDisplayLink(webView: webView)
        }
        
        // Throttled visibility check
        if !visibilityObservers.isEmpty && currentTime - lastVisibilityCheckTime >= visibilityCheckThrottle {
            checkVisibilityThreshold(source: .displayLink)
            lastVisibilityCheckTime = currentTime
        }
    }
    
    private func checkScrollStateWithDisplayLink(webView: WKWebView) {
        let webViewId = ObjectIdentifier(webView)
        let currentOffset = webView.scrollView.contentOffset
        
        guard let lastEvent = lastScrollEvents[webViewId] else { return }
        
        let offsetDelta = abs(currentOffset.x - lastEvent.contentOffset.x) + abs(currentOffset.y - lastEvent.contentOffset.y)
        
        if offsetDelta > 0.5 {
            let event = createScrollEvent(
                for: webView,
                newOffset: currentOffset,
                oldOffset: lastEvent.contentOffset,
                source: .displayLink
            )
            
            lastScrollEvents[webViewId] = event
            notifyScrollObservers(with: event)
            
            // Check for direction changes
            let currentDirection = event.scrollDirection
            let lastDirection = lastScrollDirections[webViewId] ?? .none
            
            if currentDirection != lastDirection && currentDirection != .none {
                lastScrollDirections[webViewId] = currentDirection
                notifyScrollDirectionChange(with: event, newDirection: currentDirection)
            }
        }
    }
    
    // MARK: - KVO Implementation
    
    override func observeValue(
        forKeyPath keyPath: String?,
        of object: Any?,
        change: [NSKeyValueChangeKey : Any]?,
        context: UnsafeMutableRawPointer?
    ) {
        guard keyPath == "contentOffset",
              let scrollView = object as? UIScrollView,
              let webView = findWebView(for: scrollView),
              let change = change else { return }
        
        let newOffset = (change[.newKey] as? NSValue)?.cgPointValue ?? .zero
        let oldOffset = (change[.oldKey] as? NSValue)?.cgPointValue ?? .zero
        
        let event = createScrollEvent(
            for: webView,
            newOffset: newOffset,
            oldOffset: oldOffset,
            source: .keyValueObserver
        )
        
        let webViewId = ObjectIdentifier(webView)
        let wasScrolling = lastScrollEvents[webViewId]?.isScrolling ?? false
        
        lastScrollEvents[webViewId] = event
        
        notifyScrollObservers(with: event)
        
        if event.isScrolling && !wasScrolling {
            notifyScrollStarted(with: event)
        }
        
        scrollEndTimers[webViewId]?.invalidate()
        if event.isScrolling {
            scrollEndTimers[webViewId] = Timer.scheduledTimer(withTimeInterval: scrollEndDelay, repeats: false) { [weak self] _ in
                self?.handleScrollEnd(for: webView, lastEvent: event)
            }
        }
        
        // Check visibility on scroll
        checkVisibilityThreshold(source: .keyValueObserver)
    }
    
    // MARK: - Helper Methods
    
    private func createScrollEvent(
        for webView: WKWebView,
        newOffset: CGPoint,
        oldOffset: CGPoint,
        source: ScrollEventSource
    ) -> WebViewScrollEvent {
        let scrollView = webView.scrollView
        
        return WebViewScrollEvent(
            timestamp: Date().timeIntervalSince1970,
            source: source,
            contentOffset: newOffset,
            previousOffset: oldOffset,
            contentSize: scrollView.contentSize,
            visibleBounds: scrollView.bounds,
            velocity: scrollView.panGestureRecognizer.velocity(in: scrollView),
            deceleration: scrollView.decelerationRate.rawValue
        )
    }
    
    private func findWebView(for scrollView: UIScrollView) -> WKWebView? {
        return monitoredWebViews.compactMap { $0.webView }.first { $0.scrollView === scrollView }
    }
    
    private func findWebView(in view: UIView) -> WKWebView? {
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
    
    // MARK: - Scroll Observer Notifications
    
    private func notifyScrollObservers(with event: WebViewScrollEvent) {
        let activeObservers = scrollObservers.values.compactMap { $0.observer }
        for observer in activeObservers {
            observer.onScrollEvent(event)
        }
    }
    
    private func notifyScrollStarted(with event: WebViewScrollEvent) {
        let activeObservers = scrollObservers.values.compactMap { $0.observer }
        for observer in activeObservers {
            observer.onScrollStarted(event)
        }
    }
    
    private func notifyScrollDirectionChange(with event: WebViewScrollEvent, newDirection: ScrollDirection) {
        let activeObservers = scrollObservers.values.compactMap { $0.observer }
        for observer in activeObservers {
            observer.onScrollDirectionChanged(event, newDirection: newDirection)
        }
    }
    
    private func handleScrollEnd(for webView: WKWebView, lastEvent: WebViewScrollEvent) {
        let webViewId = ObjectIdentifier(webView)
        scrollEndTimers.removeValue(forKey: webViewId)
        lastScrollDirections[webViewId] = .none
        
        let activeObservers = scrollObservers.values.compactMap { $0.observer }
        for observer in activeObservers {
            observer.onScrollEnded(lastEvent)
        }
    }
    
    // MARK: - Visibility Detection
    
    private func checkVisibilityThreshold(source: ScrollEventSource) {
        let safeAreaInsets = getSafeAreaInsets()
        let shouldHide = isInScrollHideZone(safeAreaInsets: safeAreaInsets)
        let shouldShow = !shouldHide
        
        if shouldShow != currentVisibilityState {
            currentVisibilityState = shouldShow
            
            let reason = shouldShow ? "Scroll position allows UI visibility" : "Scroll position conflicts with system UI"
            let triggerFrame = getAnyHeaderView()?.convert(getAnyHeaderView()?.bounds ?? .zero, to: nil)
            
            let event = ScrollVisibilityEvent(
                timestamp: Date().timeIntervalSince1970,
                source: source,
                shouldShow: shouldShow,
                reason: reason,
                triggerFrame: triggerFrame,
                safeAreaInsets: safeAreaInsets,
                hasNotch: safeAreaInsets.top > 24
            )
            
            notifyVisibilityObservers(with: event)
        }
    }
    
    private func isInScrollHideZone(safeAreaInsets: UIEdgeInsets) -> Bool {
        guard let window = UIApplication.shared.windows.first else { return false }
        
        let statusBarHeight = safeAreaInsets.top
        let hasNotch = statusBarHeight > 24
        
        if let headerView = getAnyHeaderView() {
            return isViewInNotchArea(
                viewFrame: headerView.convert(headerView.bounds, to: nil),
                safeAreaInsets: safeAreaInsets,
                window: window,
                hasNotch: hasNotch
            )
        }
        
        if let webView = monitoredWebViews.first?.webView {
            let scrollOffset = webView.scrollView.contentOffset.y
            let hideThreshold: CGFloat = hasNotch ? 50 : 20
            return scrollOffset > hideThreshold
        }
        
        return false
    }
    
    private func isViewInNotchArea(viewFrame: CGRect, safeAreaInsets: UIEdgeInsets, window: UIWindow, hasNotch: Bool) -> Bool {
        let isViewOffScreen = viewFrame.minY < -10
        
        if isViewOffScreen {
            return true
        }
        
        if hasNotch {
            let notchHeight = safeAreaInsets.top
            
            // Only hide if the view is significantly ABOVE the safe area, not just touching it
            // Allow views that start near the bottom of the notch area (legitimate header position)
            let conflictThreshold = notchHeight - 10 // Allow 10pt buffer below notch
            let viewTooHighInNotch = viewFrame.minY < conflictThreshold
            
            return viewTooHighInNotch
        }
        
        return false
    }
    
    private func getAnyHeaderView() -> UIView? {
        if let browser = currentBrowser as? NSObject,
           browser.responds(to: Selector("header")),
           let header = browser.value(forKey: "header") as? NSObject,
           header.responds(to: Selector("customActionView")),
           let customActionView = header.value(forKey: "customActionView") as? UIView {
            return customActionView
        }
        return nil
    }
    
    private func getSafeAreaInsets() -> UIEdgeInsets {
        if #available(iOS 11.0, *) {
            let window = UIApplication.shared.windows.first { $0.isKeyWindow }
            return window?.safeAreaInsets ?? UIEdgeInsets.zero
        }
        return UIEdgeInsets.zero
    }
    
    // MARK: - Visibility Observer Notifications
    
    private func notifyVisibilityObservers(with event: ScrollVisibilityEvent) {
        let activeObservers = visibilityObservers.values.compactMap { $0.observer }
        for observer in activeObservers {
            observer.onScrollVisibilityChanged(event)
        }
    }
    
    private func sendCurrentVisibilityState(to observer: ScrollVisibilityObserver) {
        // Re-evaluate visibility state when sending to new observer to ensure accuracy
        let safeAreaInsets = getSafeAreaInsets()
        let shouldHide = isInScrollHideZone(safeAreaInsets: safeAreaInsets)
        let shouldShow = !shouldHide
        
        
        let event = ScrollVisibilityEvent(
            timestamp: Date().timeIntervalSince1970,
            source: .userTriggered,
            shouldShow: shouldShow,
            reason: "Initial state for new observer (re-evaluated)",
            triggerFrame: getAnyHeaderView()?.convert(getAnyHeaderView()?.bounds ?? .zero, to: nil),
            safeAreaInsets: safeAreaInsets,
            hasNotch: safeAreaInsets.top > 24
        )
        
        // Update current state to match what we're sending
        currentVisibilityState = shouldShow
        
        observer.onScrollVisibilityChanged(event)
    }
    
    // MARK: - Cleanup and Memory Management
    
    private func cleanupStaleObserversIfNeeded() {
        let currentTime = Date().timeIntervalSince1970
        
        if currentTime - lastCleanupTime > cleanupInterval {
            cleanupStaleObservers()
            cleanupStaleWebViewsIfNeeded()
            lastCleanupTime = currentTime
        }
    }
    
    private func cleanupStaleObservers() {
        let scrollBefore = scrollObservers.count
        let visibilityBefore = visibilityObservers.count
        
        scrollObservers = scrollObservers.filter { $0.value.observer != nil }
        visibilityObservers = visibilityObservers.filter { $0.value.observer != nil }
        
        let scrollAfter = scrollObservers.count
        let visibilityAfter = visibilityObservers.count
        
        if scrollBefore != scrollAfter || visibilityBefore != visibilityAfter {
            print("📡 ScrollEventBus: Cleaned up \(scrollBefore - scrollAfter) scroll observers, \(visibilityBefore - visibilityAfter) visibility observers")
        }
        
        stopDisplayLinkIfNotNeeded()
    }
    
    private func cleanupStaleWebViewsIfNeeded() {
        let staleBefore = monitoredWebViews.count
        
        monitoredWebViews = monitoredWebViews.filter { wrapper in
            if wrapper.webView == nil {
                if let webViewId = wrapper.webViewId {
                    lastScrollEvents.removeValue(forKey: webViewId)
                    lastScrollDirections.removeValue(forKey: webViewId)
                    scrollEndTimers[webViewId]?.invalidate()
                    scrollEndTimers.removeValue(forKey: webViewId)
                }
                return false
            }
            return true
        }
        
        let staleAfter = monitoredWebViews.count
        
        if staleBefore != staleAfter {
            print("📡 ScrollEventBus: Cleaned up \(staleBefore - staleAfter) stale webViews")
        }
    }
    
    // MARK: - Notification Observers
    
    private func setupNotificationObservers() {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appWillResignActive),
            name: UIApplication.willResignActiveNotification,
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
            selector: #selector(orientationChanged),
            name: UIDevice.orientationDidChangeNotification,
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
    
    private func removeNotificationObservers() {
        NotificationCenter.default.removeObserver(self)
    }
    
    @objc private func appWillResignActive() {
        stopDisplayLink()
    }
    
    @objc private func appDidBecomeActive() {
        startDisplayLinkIfNeeded()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.checkVisibilityThreshold(source: .userTriggered)
        }
    }
    
    @objc private func orientationChanged() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.checkVisibilityThreshold(source: .orientationChange)
        }
    }
    
    @objc private func keyboardWillShow() {
        checkVisibilityThreshold(source: .keyboardShow)
    }
    
    @objc private func keyboardWillHide() {
        checkVisibilityThreshold(source: .keyboardHide)
    }
}

// MARK: - Weak Reference Wrappers

private class WeakScrollObserverWrapper {
    weak var observer: ScrollEventObserver?
    
    init(_ observer: ScrollEventObserver) {
        self.observer = observer
    }
}

private class WeakVisibilityObserverWrapper {
    weak var observer: ScrollVisibilityObserver?
    
    init(_ observer: ScrollVisibilityObserver) {
        self.observer = observer
    }
}

private class WeakWebViewWrapper {
    weak var webView: WKWebView?
    let webViewId: ObjectIdentifier?
    
    init(_ webView: WKWebView) {
        self.webView = webView
        self.webViewId = ObjectIdentifier(webView)
    }
}

// MARK: - Legacy Support

extension BrowserScrollEventBus {
    
    // Legacy support for the old interface
    func subscribe(_ subscriber: ScrollVisibilitySubscriber) {
        addVisibilityObserver(LegacyScrollVisibilityAdapter(subscriber))
    }
    
    func unsubscribe(subscriberId: String) {
        removeObserver(withId: subscriberId)
    }
    
    func removeObserver(withId observerId: String) {
        scrollObservers.removeValue(forKey: observerId)
        visibilityObservers.removeValue(forKey: observerId)
        
        if scrollObservers.isEmpty && visibilityObservers.isEmpty {
            stopDisplayLinkIfNotNeeded()
        }
    }
    
    private class LegacyScrollVisibilityAdapter: ScrollVisibilityObserver {
        let observerId: String
        private weak var subscriber: ScrollVisibilitySubscriber?
        
        init(_ subscriber: ScrollVisibilitySubscriber) {
            self.subscriber = subscriber
            self.observerId = subscriber.subscriberId
        }
        
        func onScrollVisibilityChanged(_ event: ScrollVisibilityEvent) {
            let legacyEvent = LegacyScrollVisibilityEvent(
                shouldShow: event.shouldShow,
                reason: event.reason
            )
            subscriber?.onScrollVisibilityChanged(legacyEvent)
        }
    }
}

// Legacy protocol for backward compatibility
protocol ScrollVisibilitySubscriber: AnyObject {
    func onScrollVisibilityChanged(_ event: LegacyScrollVisibilityEvent)
    var subscriberId: String { get }
}

// Legacy event struct for backward compatibility
struct LegacyScrollVisibilityEvent {
    let shouldShow: Bool
    let reason: String
}