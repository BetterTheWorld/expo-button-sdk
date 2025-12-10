import Foundation
import UIKit
import WebKit
import Button

class PictureInPictureManager: NSObject, ScrollVisibilityObserver {
    private var isMinimized: Bool = false
    private var originalBrowserViewController: UIViewController?
    private var options: [String: Any]
    private var pipWindow: UIWindow?
    private var originalWebView: UIView?
    private var containerView: UIView?
    private var isButtonHidden: Bool = false
    
    weak var delegate: PictureInPictureManagerDelegate?
    
    init(options: [String: Any]) {
        self.options = options
    }
    
    func addMinimizeButton(to browser: BrowserInterface) {
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            self.createMinimizeButtonInHeader(browser: browser)
        }
    }
    
    private func createMinimizeButtonInHeader(browser: BrowserInterface) {
        // Create container view for existing customActionView and minimize button
        let containerView = UIView()
        containerView.translatesAutoresizingMaskIntoConstraints = false
        self.containerView = containerView
        
        // Create minimize button with simple chevron
        let minimizeButton = UIButton(type: .system)
        minimizeButton.setTitle("⌄", for: .normal)
        minimizeButton.titleLabel?.font = UIFont.systemFont(ofSize: 16, weight: .semibold)
        minimizeButton.setTitleColor(.white, for: .normal)
        minimizeButton.translatesAutoresizingMaskIntoConstraints = false
        minimizeButton.addTarget(self, action: #selector(minimizeButtonTapped), for: .touchUpInside)
        
        // Get existing custom action view (deals button) if any
        let existingView = browser.header.customActionView
        
        // Add views to container
        if let existingView = existingView {
            containerView.addSubview(existingView)
            existingView.translatesAutoresizingMaskIntoConstraints = false
        }
        containerView.addSubview(minimizeButton)
        
        // Set up constraints for horizontal layout
        NSLayoutConstraint.activate([
            containerView.heightAnchor.constraint(equalToConstant: 30)
        ])
        
        if let existingView = existingView {
            // Layout: [deals button] [minimize button] with spacing
            NSLayoutConstraint.activate([
                existingView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
                existingView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
                
                minimizeButton.leadingAnchor.constraint(equalTo: existingView.trailingAnchor, constant: 12),
                minimizeButton.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
                minimizeButton.topAnchor.constraint(equalTo: containerView.topAnchor, constant: -6), // Subir el chevron
                minimizeButton.widthAnchor.constraint(equalToConstant: 30),
                minimizeButton.heightAnchor.constraint(equalToConstant: 30)
            ])
            
            // Update container width based on content
            containerView.widthAnchor.constraint(equalTo: existingView.widthAnchor, constant: 42).isActive = true
        } else {
            // Only minimize button
            NSLayoutConstraint.activate([
                minimizeButton.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
                minimizeButton.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
                minimizeButton.topAnchor.constraint(equalTo: containerView.topAnchor, constant: -6), // Subir el chevron
                minimizeButton.widthAnchor.constraint(equalToConstant: 30),
                minimizeButton.heightAnchor.constraint(equalToConstant: 30),
                
                containerView.widthAnchor.constraint(equalToConstant: 30)
            ])
        }
        
        // Set the container as the new custom action view
        browser.header.customActionView = containerView
        
        // Setup scroll monitoring using the event bus after a small delay
        // to let the UI settle after wrapping the promotion badge
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            BrowserScrollEventBus.shared.addVisibilityObserver(self)
            BrowserScrollEventBus.shared.startMonitoring(browser: browser)
        }
    }
    
    // MARK: - ScrollVisibilityObserver Implementation
    
    func onScrollVisibilityChanged(_ event: ScrollVisibilityEvent) {
        print("📡 PictureInPictureManager: Visibility event -> \(event.shouldShow ? "SHOW" : "HIDE") (\(event.reason))")
        setContainerVisibility(event.shouldShow)
    }
    
    
    
    
    
    
    private func setContainerVisibility(_ visible: Bool) {
        guard let containerView = self.containerView else { return }
        
        isButtonHidden = !visible
        
        UIView.animate(withDuration: 0.3) {
            containerView.alpha = visible ? 1.0 : 0.0
        }
    }
    
    @objc private func minimizeButtonTapped() {
        if !isMinimized {
            minimizeToPiP()
        } else {
            restoreFromPiP()
        }
    }
    
    private func minimizeToPiP() {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
        
        var topViewController = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let presentedVC = topViewController?.presentedViewController {
            topViewController = presentedVC
        }
        
        guard let browserVC = topViewController else { return }
        originalBrowserViewController = browserVC
        
        let screenBounds = UIScreen.main.bounds
        
        // Get PiP configuration from options
        var pipSize: CGSize
        var pipPosition: CGPoint
        
        if let animationConfig = options["animationConfig"] as? [String: Any],
           let pipConfig = animationConfig["pictureInPicture"] as? [String: Any] {
            
            // Custom size if provided
            if let sizeConfig = pipConfig["size"] as? [String: NSNumber] {
                pipSize = CGSize(
                    width: sizeConfig["width"]?.doubleValue ?? 200,
                    height: sizeConfig["height"]?.doubleValue ?? 300
                )
            } else {
                // Default scale approach
                let scale: CGFloat = 0.25
                pipSize = CGSize(width: screenBounds.width * scale, height: screenBounds.height * scale)
            }
            
            // Custom position if provided
            if let positionConfig = pipConfig["position"] as? [String: NSNumber] {
                pipPosition = CGPoint(
                    x: positionConfig["x"]?.doubleValue ?? 20,
                    y: positionConfig["y"]?.doubleValue ?? 120
                )
            } else {
                // Default position (bottom-right)
                pipPosition = CGPoint(
                    x: screenBounds.width - pipSize.width - 20,
                    y: screenBounds.height - pipSize.height - 120
                )
            }
        } else {
            // Legacy fallback
            let scale: CGFloat = 0.25
            pipSize = CGSize(width: screenBounds.width * scale, height: screenBounds.height * scale)
            pipPosition = CGPoint(
                x: screenBounds.width - pipSize.width - 20,
                y: screenBounds.height - pipSize.height - 120
            )
        }
        
        let pipFrame = CGRect(origin: pipPosition, size: pipSize)
        
        // Create snapshot of entire browser view with proper content
        let browserSnapshot = browserVC.view.snapshotView(afterScreenUpdates: false)
        guard let snapshot = browserSnapshot else {
            return
        }
        
        // Create new floating window
        pipWindow = UIWindow(frame: pipFrame)
        pipWindow?.windowScene = windowScene
        pipWindow?.windowLevel = UIWindow.Level.statusBar + 1
        pipWindow?.backgroundColor = UIColor.clear
        pipWindow?.layer.cornerRadius = 12
        pipWindow?.clipsToBounds = true
        
        // Create container view controller
        let pipVC = UIViewController()
        pipWindow?.rootViewController = pipVC
        
        // Set snapshot to fill the PiP window completely
        snapshot.frame = CGRect(origin: .zero, size: pipSize)
        snapshot.contentMode = .scaleAspectFit
        pipVC.view.addSubview(snapshot)
        
        // Add shadow and styling to PiP window
        pipWindow?.layer.shadowColor = UIColor.black.cgColor
        pipWindow?.layer.shadowOffset = CGSize(width: 0, height: 4)
        pipWindow?.layer.shadowOpacity = 0.3
        pipWindow?.layer.shadowRadius = 8
        
        // Add tap gesture to restore
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(minimizedBrowserTapped))
        pipVC.view.addGestureRecognizer(tapGesture)
        
        // Start with PiP window at full screen size for animation
        let fullScreenFrame = CGRect(origin: .zero, size: screenBounds.size)
        pipWindow?.frame = fullScreenFrame
        pipWindow?.makeKeyAndVisible()
        
        // Position snapshot to fill the full screen window initially
        snapshot.frame = CGRect(origin: .zero, size: screenBounds.size)
        
        // Animate PiP window to minimized size
        UIView.animate(withDuration: 0.4, delay: 0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0.3, animations: {
            // Animate window frame to target position/size
            self.pipWindow?.frame = pipFrame
            
            // Animate snapshot to fit the new window size
            snapshot.frame = CGRect(origin: .zero, size: pipSize)
            
            // Fade out browser simultaneously
            browserVC.view.alpha = 0
        }) { _ in
            // After animation completes, set up window management
            if let browserWindow = windowScene.windows.first(where: { $0.rootViewController == browserVC || $0.rootViewController?.presentedViewController == browserVC }) {
                browserWindow.windowLevel = UIWindow.Level.normal - 1
            }
            
            // Make main window key to enable React Native interaction
            if let mainWindow = windowScene.windows.first(where: { !$0.isEqual(self.pipWindow) && $0.windowLevel == UIWindow.Level.normal }) {
                mainWindow.makeKeyAndVisible()
            }
            
            self.isMinimized = true
        }
    }
    
    
    private func restoreFromPiP() {
        guard let pipWindow = pipWindow,
              let browserVC = originalBrowserViewController,
              let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
        
        let screenBounds = UIScreen.main.bounds
        
        // Get the snapshot for animation
        guard let snapshot = pipWindow.rootViewController?.view.subviews.first else {
            // Fallback to instant restore if no snapshot found
            self.instantRestore(browserVC: browserVC, windowScene: windowScene)
            return
        }
        
        // Animate PiP window and snapshot back to full screen
        UIView.animate(withDuration: 0.4, delay: 0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0.3, animations: {
            // Animate window back to full screen
            pipWindow.frame = screenBounds
            
            // Animate snapshot back to full size
            snapshot.frame = CGRect(origin: .zero, size: screenBounds.size)
            
            // Fade in browser simultaneously
            browserVC.view.alpha = 1.0
        }) { _ in
            // Restore browser window level and make it key FIRST
            if let browserWindow = windowScene.windows.first(where: { $0.rootViewController == browserVC || $0.rootViewController?.presentedViewController == browserVC }) {
                browserWindow.windowLevel = UIWindow.Level.normal
                browserWindow.makeKeyAndVisible()
            }
            
            browserVC.view.isUserInteractionEnabled = true
            self.isMinimized = false
            
            // Clean up PiP window AFTER browser is restored
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                pipWindow.isHidden = true
                pipWindow.resignKey()
                self.pipWindow = nil
            }
        }
    }
    
    private func instantRestore(browserVC: UIViewController, windowScene: UIWindowScene) {
        // Fallback instant restore
        pipWindow?.isHidden = true
        pipWindow?.resignKey()
        pipWindow = nil
        
        if let browserWindow = windowScene.windows.first(where: { $0.rootViewController == browserVC || $0.rootViewController?.presentedViewController == browserVC }) {
            browserWindow.windowLevel = UIWindow.Level.normal
            browserWindow.makeKeyAndVisible()
        }
        
        browserVC.view.alpha = 1.0
        browserVC.view.isUserInteractionEnabled = true
        isMinimized = false
    }
    
    @objc private func minimizedBrowserTapped() {
        restoreFromPiP()
    }
    
    func cleanup() {
        // Remove from event bus
        BrowserScrollEventBus.shared.removeVisibilityObserver(self)
        
        // If PiP window exists, restore webview and close window
        if isMinimized && pipWindow != nil {
            restoreFromPiP()
        }
        
        // Clean up PiP window if still exists
        pipWindow?.isHidden = true
        pipWindow?.resignKey()
        pipWindow = nil
        
        // Restore browser if it was minimized
        if let browserVC = originalBrowserViewController {
            browserVC.view.alpha = 1
            browserVC.view.isUserInteractionEnabled = true
        }
        
        originalBrowserViewController = nil
        originalWebView = nil
        containerView = nil
        isMinimized = false
    }
}

protocol PictureInPictureManagerDelegate: AnyObject {
    func didMinimize()
    func didRestore()
}