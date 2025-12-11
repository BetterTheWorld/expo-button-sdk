import UIKit
import WebKit

// MARK: - Example Usage of BrowserScrollEventBus

class ScrollEventBusExample: ScrollEventObserver, ScrollVisibilityObserver {
    
    // MARK: - Setup
    
    func setupScrollMonitoring(for browser: AnyObject) {
        // Add this class as both scroll and visibility observer
        BrowserScrollEventBus.shared.addScrollObserver(self)
        BrowserScrollEventBus.shared.addVisibilityObserver(self)
        
        // Start monitoring the browser
        BrowserScrollEventBus.shared.startMonitoring(browser: browser)
        
        print("📱 Example: Started monitoring scroll events")
    }
    
    // MARK: - ScrollEventObserver Implementation
    
    func onScrollEvent(_ event: WebViewScrollEvent) {
        print("📱 Example: Scroll event - Direction: \(event.scrollDirection), Distance: \(event.scrollDistance)")
    }
    
    func onScrollStarted(_ event: WebViewScrollEvent) {
        print("📱 Example: Scroll started - Velocity: \(event.velocity)")
    }
    
    func onScrollEnded(_ event: WebViewScrollEvent) {
        print("📱 Example: Scroll ended - Final offset: \(event.contentOffset)")
    }
    
    func onScrollDirectionChanged(_ event: WebViewScrollEvent, newDirection: ScrollDirection) {
        print("📱 Example: Direction changed to: \(newDirection)")
    }
    
    // MARK: - ScrollVisibilityObserver Implementation
    
    func onScrollVisibilityChanged(_ event: ScrollVisibilityEvent) {
        print("📱 Example: Visibility changed - Show: \(event.shouldShow), Reason: \(event.reason)")
        
        // Example: Update UI based on visibility
        if event.shouldShow {
            showMyButton()
        } else {
            hideMyButton()
        }
    }
    
    // MARK: - Example UI Methods
    
    private func showMyButton() {
        // Your button show logic here
        print("📱 Example: Showing button")
    }
    
    private func hideMyButton() {
        // Your button hide logic here
        print("📱 Example: Hiding button")
    }
    
    // MARK: - Cleanup
    
    deinit {
        BrowserScrollEventBus.shared.removeScrollObserver(self)
        BrowserScrollEventBus.shared.removeVisibilityObserver(self)
        print("📱 Example: Cleaned up scroll monitoring")
    }
}

// MARK: - Advanced Usage Example

class AdvancedScrollEventExample: ScrollEventObserver {
    private var lastScrollTime: TimeInterval = 0
    private var scrollVelocityHistory: [CGPoint] = []
    
    func onScrollEvent(_ event: WebViewScrollEvent) {
        // Track scroll velocity history for momentum detection
        scrollVelocityHistory.append(event.velocity)
        if scrollVelocityHistory.count > 10 {
            scrollVelocityHistory.removeFirst()
        }
        
        // Throttle processing to avoid excessive calls
        let currentTime = Date().timeIntervalSince1970
        guard currentTime - lastScrollTime > 0.016 else { return } // 60fps max
        lastScrollTime = currentTime
        
        // Analyze scroll behavior
        analyzeScrollBehavior(event)
    }
    
    private func analyzeScrollBehavior(_ event: WebViewScrollEvent) {
        let speed = sqrt(event.velocity.x * event.velocity.x + event.velocity.y * event.velocity.y)
        
        if speed > 1000 {
            print("🚀 Fast scroll detected: \(speed)")
        } else if speed < 50 && event.isScrolling {
            print("🐌 Slow scroll detected: \(speed)")
        }
        
        // Check if user is scrolling near edges
        let scrollView = event.visibleBounds
        let contentOffset = event.contentOffset
        let contentSize = event.contentSize
        
        if contentOffset.y < 100 {
            print("📍 Scrolling near top")
        } else if contentOffset.y > contentSize.height - scrollView.height - 100 {
            print("📍 Scrolling near bottom")
        }
    }
}