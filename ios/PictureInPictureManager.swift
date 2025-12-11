import Foundation
import UIKit
import WebKit
import Button

class PictureInPictureManager: NSObject, ScrollVisibilityObserver {
    private var isMinimized: Bool = false
    private var originalBrowserViewController: UIViewController?
    private var options: [String: Any]
    private var coverImageView: UIImageView?
    private var pipWindow: UIWindow?
    private var originalWebView: UIView?
    private var containerView: UIView?
    private var isButtonHidden: Bool = false
    
    // Drag & Drop properties
    private var isDragging: Bool = false
    private var dragStartLocation: CGPoint = .zero
    private var pipWindowStartFrame: CGRect = .zero
    private var lastPipPosition: CGPoint?
    
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
                // Default square size like YouTube (120x120 points)
                let defaultSize: CGFloat = 120
                pipSize = CGSize(width: defaultSize, height: defaultSize)
            }
            
            // Use last saved position, or custom position if provided, or default
            if let savedPosition = lastPipPosition {
                // Use the last position from previous drag
                pipPosition = savedPosition
            } else if let positionConfig = pipConfig["position"] as? [String: NSNumber] {
                // Use custom position from config
                pipPosition = CGPoint(
                    x: positionConfig["x"]?.doubleValue ?? 20,
                    y: positionConfig["y"]?.doubleValue ?? 120
                )
            } else {
                // Default position (bottom-right like YouTube)
                pipPosition = CGPoint(
                    x: screenBounds.width - pipSize.width - 16,
                    y: screenBounds.height - pipSize.height - 100
                )
            }
        } else {
            // Legacy fallback - square like YouTube
            let defaultSize: CGFloat = 120
            pipSize = CGSize(width: defaultSize, height: defaultSize)
            
            // Use last saved position or default
            if let savedPosition = lastPipPosition {
                pipPosition = savedPosition
            } else {
                pipPosition = CGPoint(
                    x: screenBounds.width - pipSize.width - 16,
                    y: screenBounds.height - pipSize.height - 100
                )
            }
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
        snapshot.contentMode = .scaleAspectFill
        snapshot.clipsToBounds = true
        pipVC.view.addSubview(snapshot)
        
        // Add cover image if provided
        if let coverImageConfig = options["coverImage"] as? [String: Any] {
            setupCoverImage(in: pipVC.view, size: pipSize, config: coverImageConfig)
        }
        
        // Add shadow and styling to PiP window
        pipWindow?.layer.shadowColor = UIColor.black.cgColor
        pipWindow?.layer.shadowOffset = CGSize(width: 0, height: 4)
        pipWindow?.layer.shadowOpacity = 0.3
        pipWindow?.layer.shadowRadius = 8
        
        // Add gestures for interaction
        setupPiPGestures(for: pipVC.view)
        
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
            
            // Fade out cover image if present
            if let coverImageView = self.coverImageView {
                coverImageView.alpha = 0.0
            }
            
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
    
    // MARK: - PiP Gesture Handling
    
    private func setupPiPGestures(for view: UIView) {
        // Pan gesture for dragging (primary gesture)
        let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        
        // Tap gesture to restore (single tap)
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(minimizedBrowserTapped))
        tapGesture.numberOfTapsRequired = 1
        
        // Configure gesture interactions
        // Tap should fail if there's any significant pan movement
        tapGesture.require(toFail: panGesture)
        
        view.addGestureRecognizer(panGesture)
        view.addGestureRecognizer(tapGesture)
    }
    
    @objc private func minimizedBrowserTapped() {
        restoreFromPiP()
    }
    
    @objc private func handlePan(_ gesture: UIPanGestureRecognizer) {
        guard let pipWindow = self.pipWindow else { return }
        
        switch gesture.state {
        case .began:
            isDragging = true
            pipWindowStartFrame = pipWindow.frame
            
            // Immediate visual feedback
            UIView.animate(withDuration: 0.15, animations: {
                pipWindow.transform = CGAffineTransform(scaleX: 1.03, y: 1.03)
                pipWindow.layer.shadowOpacity = 0.5
                pipWindow.layer.shadowRadius = 10
            })
            
        case .changed:
            let translation = gesture.translation(in: nil)
            let newOrigin = CGPoint(
                x: pipWindowStartFrame.origin.x + translation.x,
                y: pipWindowStartFrame.origin.y + translation.y
            )
            
            // Apply bounds checking
            let constrainedFrame = constrainPiPFrame(
                origin: newOrigin,
                size: pipWindow.frame.size
            )
            
            pipWindow.frame = constrainedFrame
            
        case .ended:
            // Check if there was significant movement to determine if it was a drag or tap
            let translation = gesture.translation(in: nil)
            let distance = sqrt(translation.x * translation.x + translation.y * translation.y)
            
            if distance < 10 {
                // Minimal movement - treat as tap, restore PiP
                
                UIView.animate(withDuration: 0.15, animations: {
                    pipWindow.transform = .identity
                    pipWindow.layer.shadowOpacity = 0.3
                    pipWindow.layer.shadowRadius = 8
                }) { _ in
                    self.isDragging = false
                    self.restoreFromPiP()
                }
            } else {
                // Significant movement - complete the drag
                let finalFrame = snapToEdges(currentFrame: pipWindow.frame)
                
                UIView.animate(withDuration: 0.3, delay: 0, usingSpringWithDamping: 0.7, initialSpringVelocity: 0.5, animations: {
                    pipWindow.frame = finalFrame
                    pipWindow.transform = .identity
                    pipWindow.layer.shadowOpacity = 0.3
                    pipWindow.layer.shadowRadius = 8
                }) { _ in
                    self.isDragging = false
                    // Save the final position for future minimizations
                    self.lastPipPosition = finalFrame.origin
                }
            }
            
        case .cancelled:
            
            UIView.animate(withDuration: 0.3, animations: {
                pipWindow.frame = self.pipWindowStartFrame
                pipWindow.transform = .identity
                pipWindow.layer.shadowOpacity = 0.3
                pipWindow.layer.shadowRadius = 8
            }) { _ in
                self.isDragging = false
            }
            
        default:
            break
        }
    }
    
    private func constrainPiPFrame(origin: CGPoint, size: CGSize) -> CGRect {
        let screenBounds = UIScreen.main.bounds
        let margin: CGFloat = 10
        
        let minX = margin
        let maxX = screenBounds.width - size.width - margin
        let minY = margin
        let maxY = screenBounds.height - size.height - margin
        
        let constrainedX = max(minX, min(maxX, origin.x))
        let constrainedY = max(minY, min(maxY, origin.y))
        
        return CGRect(
            x: constrainedX,
            y: constrainedY,
            width: size.width,
            height: size.height
        )
    }
    
    private func snapToEdges(currentFrame: CGRect) -> CGRect {
        let screenBounds = UIScreen.main.bounds
        let snapMargin: CGFloat = 20
        
        var newFrame = currentFrame
        
        // Snap to left or right edge (whichever is closer)
        let distanceToLeft = currentFrame.minX
        let distanceToRight = screenBounds.width - currentFrame.maxX
        
        if distanceToLeft < distanceToRight {
            // Snap to left
            newFrame.origin.x = snapMargin
        } else {
            // Snap to right
            newFrame.origin.x = screenBounds.width - currentFrame.width - snapMargin
        }
        
        // Keep Y position but ensure it's within safe bounds
        let safeAreaInsets = getSafeAreaInsets()
        let topMargin = safeAreaInsets.top + 20
        let bottomMargin = safeAreaInsets.bottom + 20
        
        let minY = topMargin
        let maxY = screenBounds.height - currentFrame.height - bottomMargin
        
        newFrame.origin.y = max(minY, min(maxY, currentFrame.origin.y))
        
        return newFrame
    }
    
    private func getSafeAreaInsets() -> UIEdgeInsets {
        if #available(iOS 11.0, *) {
            let window = UIApplication.shared.windows.first { $0.isKeyWindow }
            return window?.safeAreaInsets ?? UIEdgeInsets.zero
        }
        return UIEdgeInsets.zero
    }
    
    private func setupCoverImage(in containerView: UIView, size: CGSize, config: [String: Any]) {
        let imageView = UIImageView()
        imageView.frame = CGRect(origin: .zero, size: size)
        imageView.contentMode = .scaleAspectFill
        imageView.clipsToBounds = true
        imageView.backgroundColor = UIColor.black.withAlphaComponent(0.1)
        
        // Try to load image from different sources
        if let imageUrlString = config["uri"] as? String,
           let imageUrl = URL(string: imageUrlString) {
            // Load from URL
            loadImageFromURL(imageUrl, into: imageView)
        } else if let imageName = config["source"] as? String {
            // Load from bundle
            if let bundleImage = UIImage(named: imageName) {
                imageView.image = bundleImage
            }
        } else if let base64String = config["base64"] as? String {
            // Load from base64
            if let imageData = Data(base64Encoded: base64String),
               let image = UIImage(data: imageData) {
                imageView.image = image
            }
        }
        
        // Add subtle overlay to distinguish from background
        let overlayView = UIView()
        overlayView.frame = imageView.bounds
        overlayView.backgroundColor = UIColor.black.withAlphaComponent(0.05)
        imageView.addSubview(overlayView)
        
        containerView.addSubview(imageView)
        self.coverImageView = imageView
        
        // Ensure cover image is on top
        containerView.bringSubviewToFront(imageView)
    }
    
    private func loadImageFromURL(_ url: URL, into imageView: UIImageView) {
        URLSession.shared.dataTask(with: url) { data, response, error in
            guard let data = data, error == nil else {
                print("PiP: Failed to load cover image from URL: \(error?.localizedDescription ?? "Unknown error")")
                return
            }
            
            DispatchQueue.main.async {
                imageView.image = UIImage(data: data)
            }
        }.resume()
    }
    
    func cleanup() {
        // UI updates must be on main thread
        if Thread.isMainThread {
            self.performCleanup()
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.performCleanup()
            }
        }
    }
    
    private func performCleanup() {
        // Remove from event bus
        BrowserScrollEventBus.shared.removeVisibilityObserver(self)
        
        // Just destroy PiP window without restoring browser state (avoiding unnecessary restore)
        if pipWindow != nil {
            pipWindow?.isHidden = true
            pipWindow?.resignKey()
            pipWindow = nil
        }
        
        // Reset browser view alpha if we still have reference, just in case
        if let browserVC = originalBrowserViewController {
            browserVC.view.alpha = 1.0
        }
        
        originalBrowserViewController = nil
        originalWebView = nil
        containerView = nil
        coverImageView = nil
        isMinimized = false
        
        // Reset drag state
        isDragging = false
        dragStartLocation = .zero
        pipWindowStartFrame = .zero
        lastPipPosition = nil
    }
}

protocol PictureInPictureManagerDelegate: AnyObject {
    func didMinimize()
    func didRestore()
}