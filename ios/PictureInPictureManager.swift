import Foundation
import UIKit
import WebKit
import Button

class PictureInPictureManager: NSObject, ScrollVisibilityObserver {
    private var isMinimized: Bool = false
    private var originalBrowserViewController: UIViewController?
    private var options: [String: Any]
    private var coverImageView: UIView?
    private var pipWindow: UIWindow?
    private var originalWebView: UIView?
    private var containerView: UIView?
    private var isButtonHidden: Bool = false
    private var chevronColor: UIColor = .white
    private var headerTintColor: UIColor = .white
    private var earnText: String?
    private var earnTextColor: UIColor = .white
    private var earnTextBackgroundColor: UIColor? = nil
    private var earnTextFontFamily: String? = nil
    private var earnTextFontSize: CGFloat = 12
    private var earnTextFontWeight: UIFont.Weight = .semibold
    private var earnTextLineHeight: CGFloat? = nil
    private var earnLabel: UILabel?
    private var earnTextMargin: CGFloat = 28
    private var chevronUpView: UIView?
    private var closeButton: UIView?
    private var pipOverlayInset: CGFloat = 10
    private var pipCloseButtonSize: CGFloat = 20
    private var pipChevronSize: CGFloat = 15
    private var pipChevronHeight: CGFloat? = nil
    private var pipChevronStrokeWidth: CGFloat = 0
    private var pipCloseStrokeWidth: CGFloat = 0
    private var pipTapToRestore: Bool = true
    private var coverImageScaleType: UIView.ContentMode = .scaleAspectFill
    private var coverImageBackgroundColor: UIColor = .clear
    private var coverImagePadding: CGFloat = 0
    private var coverImageWidth: CGFloat? = nil
    private var coverImageHeight: CGFloat? = nil
    
    private var isDragging: Bool = false
    private var dragStartLocation: CGPoint = .zero
    private var pipWindowStartFrame: CGRect = .zero
    private var lastPipPosition: CGPoint?
    
    weak var delegate: PictureInPictureManagerDelegate?
    
    init(options: [String: Any]) {
        self.options = options
        super.init()

        if let tintColor = options["headerTintColor"] as? String {
            self.headerTintColor = UIColor(hex: tintColor) ?? .white
        }

        if let animationConfig = options["animationConfig"] as? [String: Any],
           let pipConfig = animationConfig["pictureInPicture"] as? [String: Any] {
            if let colorString = pipConfig["chevronColor"] as? String {
                self.chevronColor = UIColor(hex: colorString) ?? .white
            }
            if let text = pipConfig["earnText"] as? String {
                self.earnText = text
            }
            if let textColorString = pipConfig["earnTextColor"] as? String {
                self.earnTextColor = UIColor(hex: textColorString) ?? .white
            }
            if let bgColorString = pipConfig["earnTextBackgroundColor"] as? String {
                self.earnTextBackgroundColor = UIColor(hex: bgColorString)
            }
            if let fontFamily = pipConfig["earnTextFontFamily"] as? String {
                self.earnTextFontFamily = fontFamily
            }
            if let fontSize = pipConfig["earnTextFontSize"] as? NSNumber {
                self.earnTextFontSize = CGFloat(fontSize.doubleValue)
            }
            if let weight = pipConfig["earnTextFontWeight"] as? String {
                self.earnTextFontWeight = Self.parseFontWeight(weight)
            }
            if let lineHeight = pipConfig["earnTextLineHeight"] as? NSNumber {
                self.earnTextLineHeight = CGFloat(lineHeight.doubleValue)
            }
            if let inset = pipConfig["pipOverlayInset"] as? NSNumber {
                self.pipOverlayInset = CGFloat(inset.doubleValue)
            }
            if let closeSize = pipConfig["pipCloseButtonSize"] as? NSNumber {
                self.pipCloseButtonSize = CGFloat(closeSize.doubleValue)
            }
            if let chevSize = pipConfig["pipChevronSize"] as? NSNumber {
                self.pipChevronSize = CGFloat(chevSize.doubleValue)
            }
            if let chevH = pipConfig["pipChevronHeight"] as? NSNumber {
                self.pipChevronHeight = CGFloat(chevH.doubleValue)
            }
            if let sw = pipConfig["pipChevronStrokeWidth"] as? NSNumber {
                self.pipChevronStrokeWidth = CGFloat(sw.doubleValue)
            }
            if let sw = pipConfig["pipCloseStrokeWidth"] as? NSNumber {
                self.pipCloseStrokeWidth = CGFloat(sw.doubleValue)
            }
            if let tapRestore = pipConfig["pipTapToRestore"] as? Bool {
                self.pipTapToRestore = tapRestore
            }
            if let margin = pipConfig["earnTextMargin"] as? NSNumber {
                self.earnTextMargin = CGFloat(margin.doubleValue)
            }
            NSLog("[PiPManager] Config parsed — chevronSize=%.1f chevronHeight=%@ closeSize=%.1f chevronStroke=%.2f closeStroke=%.2f fontSize=%.1f inset=%.1f earnMargin=%.1f",
                  self.pipChevronSize,
                  self.pipChevronHeight.map { String(format: "%.1f", $0) } ?? "auto",
                  self.pipCloseButtonSize,
                  self.pipChevronStrokeWidth,
                  self.pipCloseStrokeWidth,
                  self.earnTextFontSize,
                  self.pipOverlayInset,
                  self.earnTextMargin)
        }
        
        if let coverImage = options["coverImage"] as? [String: Any] {
            if let scaleTypeString = coverImage["scaleType"] as? String {
                switch scaleTypeString.lowercased() {
                case "contain":
                    self.coverImageScaleType = .scaleAspectFit
                case "cover":
                    self.coverImageScaleType = .scaleAspectFill
                case "center":
                    self.coverImageScaleType = .center
                case "stretch":
                    self.coverImageScaleType = .scaleToFill
                default:
                    self.coverImageScaleType = .scaleAspectFill
                }
            }
            if let bgColorString = coverImage["backgroundColor"] as? String {
                self.coverImageBackgroundColor = UIColor(hex: bgColorString) ?? .clear
            }
            if let padding = coverImage["padding"] as? NSNumber {
                self.coverImagePadding = CGFloat(padding.doubleValue)
            }
            if let w = coverImage["width"] as? NSNumber {
                self.coverImageWidth = CGFloat(w.doubleValue)
            }
            if let h = coverImage["height"] as? NSNumber {
                self.coverImageHeight = CGFloat(h.doubleValue)
            }
        }
    }
    
    func hidePip() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self, self.isMinimized, let pipWindow = self.pipWindow else { return }
            pipWindow.frame.origin = CGPoint(x: -pipWindow.frame.width, y: pipWindow.frame.origin.y)
        }
    }
    
    func showPip() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self, self.isMinimized, let pipWindow = self.pipWindow else { return }
            let screenBounds = UIScreen.main.bounds
            let targetX = self.lastPipPosition?.x ?? (screenBounds.width - pipWindow.frame.width - 16)
            pipWindow.frame.origin = CGPoint(x: targetX, y: pipWindow.frame.origin.y)
        }
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
        
        let minimizeButton = UIButton(type: .custom)
        minimizeButton.translatesAutoresizingMaskIntoConstraints = false
        minimizeButton.addTarget(self, action: #selector(minimizeButtonTapped), for: .touchUpInside)
        
        let chevronSize: CGFloat = 14.5
        let chevronView = UIView(frame: CGRect(x: 0, y: 0, width: chevronSize, height: chevronSize * 0.5))
        chevronView.backgroundColor = .clear
        chevronView.translatesAutoresizingMaskIntoConstraints = false
        chevronView.isUserInteractionEnabled = false

        let chevronLayer = CAShapeLayer()
        let chevronPath = UIBezierPath()
        chevronPath.move(to: CGPoint(x: 0, y: 0))
        chevronPath.addLine(to: CGPoint(x: chevronSize / 2, y: chevronSize * 0.5))
        chevronPath.addLine(to: CGPoint(x: chevronSize, y: 0))
        chevronLayer.path = chevronPath.cgPath
        chevronLayer.strokeColor = self.headerTintColor.cgColor
        chevronLayer.fillColor = UIColor.clear.cgColor
        chevronLayer.lineWidth = 2.0
        chevronLayer.lineCap = .round
        chevronLayer.lineJoin = .round
        chevronView.layer.addSublayer(chevronLayer)

        minimizeButton.addSubview(chevronView)
        NSLayoutConstraint.activate([
            chevronView.centerXAnchor.constraint(equalTo: minimizeButton.centerXAnchor),
            chevronView.centerYAnchor.constraint(equalTo: minimizeButton.centerYAnchor),
            chevronView.widthAnchor.constraint(equalToConstant: chevronSize),
            chevronView.heightAnchor.constraint(equalToConstant: chevronSize * 0.55)
        ])
        
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
            containerView.heightAnchor.constraint(equalToConstant: 44)
        ])
        
        if let existingView = existingView {
            NSLayoutConstraint.activate([
                existingView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
                existingView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
                
                minimizeButton.leadingAnchor.constraint(equalTo: existingView.trailingAnchor, constant: 12),
                minimizeButton.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
                minimizeButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
                minimizeButton.heightAnchor.constraint(equalToConstant: 44)
            ])
            
            let widthConstraint = minimizeButton.widthAnchor.constraint(equalToConstant: 44)
            widthConstraint.priority = .defaultHigh
            widthConstraint.isActive = true
            
            containerView.widthAnchor.constraint(equalTo: existingView.widthAnchor, constant: 56).isActive = true
        } else {
            NSLayoutConstraint.activate([
                minimizeButton.centerXAnchor.constraint(equalTo: containerView.centerXAnchor),
                minimizeButton.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
                minimizeButton.heightAnchor.constraint(equalToConstant: 44)
            ])
            
            let widthConstraint = minimizeButton.widthAnchor.constraint(equalToConstant: 44)
            widthConstraint.priority = .defaultHigh
            widthConstraint.isActive = true
            
            let containerWidthConstraint = containerView.widthAnchor.constraint(equalToConstant: 30)
            containerWidthConstraint.priority = .defaultHigh
            containerWidthConstraint.isActive = true
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
    
    @objc private func chevronButtonTapped() {
        restoreFromPiP()
    }

    @objc private func closeButtonTapped() {
        // Close PIP and browser completely
        cleanup()
        delegate?.didRestore()
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
        pipWindow?.clipsToBounds = false
        
        // Create container view controller
        let pipVC = UIViewController()
        pipVC.view.layer.cornerRadius = 12
        pipVC.view.clipsToBounds = true
        pipWindow?.rootViewController = pipVC
        
        // Set snapshot to fill the PiP window completely
        snapshot.frame = CGRect(origin: .zero, size: pipSize)
        snapshot.contentMode = .scaleAspectFill
        snapshot.clipsToBounds = true
        pipVC.view.addSubview(snapshot)
        
        if let coverImageConfig = options["coverImage"] as? [String: Any] {
            setupCoverImage(in: pipVC.view, size: pipSize, config: coverImageConfig)
        }
        
        // Add shadow and styling to PiP window
        pipWindow?.layer.shadowColor = UIColor.black.cgColor
        pipWindow?.layer.shadowOffset = CGSize(width: 0, height: 4)
        pipWindow?.layer.shadowOpacity = 0.4
        pipWindow?.layer.shadowRadius = 12
        
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
            if let browserWindow = windowScene.windows.first(where: { $0.rootViewController == browserVC || $0.rootViewController?.presentedViewController == browserVC }) {
                browserWindow.windowLevel = UIWindow.Level.normal - 1
            }
            
            if let mainWindow = windowScene.windows.first(where: { !$0.isEqual(self.pipWindow) && $0.windowLevel == UIWindow.Level.normal }) {
                mainWindow.makeKeyAndVisible()
            }
            
            self.setupPipOverlays(in: pipVC.view, size: pipSize)
            
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
            
            if let coverImageView = self.coverImageView {
                coverImageView.alpha = 0.0
            }
            self.earnLabel?.alpha = 0.0
            self.chevronUpView?.alpha = 0.0
            self.closeButton?.alpha = 0.0
            
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
        view.addGestureRecognizer(panGesture)

        if self.pipTapToRestore {
            // Tap gesture to restore (single tap) — only if pipTapToRestore is true
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(minimizedBrowserTapped))
            tapGesture.numberOfTapsRequired = 1
            tapGesture.require(toFail: panGesture)
            view.addGestureRecognizer(tapGesture)
        }
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
            
            if distance < 10 && self.pipTapToRestore {
                // Minimal movement - treat as tap, restore PiP

                UIView.animate(withDuration: 0.15, animations: {
                    pipWindow.transform = .identity
                    pipWindow.layer.shadowOpacity = 0.3
                    pipWindow.layer.shadowRadius = 8
                }) { _ in
                    self.isDragging = false
                    self.restoreFromPiP()
                }
            } else if distance < 10 {
                // Minimal movement but tap-to-restore disabled — just reset visual
                UIView.animate(withDuration: 0.15) {
                    pipWindow.transform = .identity
                    pipWindow.layer.shadowOpacity = 0.3
                    pipWindow.layer.shadowRadius = 8
                }
                self.isDragging = false
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
        let snapMargin: CGFloat = 10
        
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
        let topMargin = safeAreaInsets.top + 8
        let bottomMargin = safeAreaInsets.bottom + 8
        
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
        let backgroundView = UIView()
        backgroundView.frame = CGRect(origin: .zero, size: size)
        backgroundView.backgroundColor = self.coverImageBackgroundColor
        
        let imageView = UIImageView()
        let padding = self.coverImagePadding
        let imgW = self.coverImageWidth ?? (size.width - padding * 2)
        let imgH = self.coverImageHeight ?? (size.height - padding * 2)
        imageView.frame = CGRect(
            x: (size.width - imgW) / 2,
            y: (size.height - imgH) / 2,
            width: imgW,
            height: imgH
        )
        imageView.contentMode = self.coverImageScaleType
        imageView.clipsToBounds = true
        
        if let imageUrlString = config["uri"] as? String,
           let imageUrl = URL(string: imageUrlString) {
            loadImageFromURL(imageUrl, into: imageView)
        } else if let imageName = config["source"] as? String {
            if let bundleImage = UIImage(named: imageName) {
                imageView.image = bundleImage
            }
        } else if let base64String = config["base64"] as? String {
            if let imageData = Data(base64Encoded: base64String),
               let image = UIImage(data: imageData) {
                imageView.image = image
            }
        }
        
        backgroundView.addSubview(imageView)
        containerView.addSubview(backgroundView)
        self.coverImageView = backgroundView
    }
    
    private func setupPipOverlays(in containerView: UIView, size: CGSize) {
        let inset = self.pipOverlayInset

        // Close button (X) — top-left — SVG viewBox 0 0 13 13
        // Minimum 44x44 hit area per Apple HIG
        let closeSize = self.pipCloseButtonSize
        let closeHitSize = max(closeSize, 44)
        let closeBtn = UIButton(type: .custom)
        closeBtn.frame = CGRect(x: inset - (closeHitSize - closeSize) / 2,
                                y: inset - (closeHitSize - closeSize) / 2,
                                width: closeHitSize, height: closeHitSize)
        closeBtn.backgroundColor = .clear
        closeBtn.addTarget(self, action: #selector(closeButtonTapped), for: .touchUpInside)

        let xLayer = CAShapeLayer()
        xLayer.frame = CGRect(x: (closeHitSize - closeSize) / 2, y: (closeHitSize - closeSize) / 2, width: closeSize, height: closeSize)
        let xPath = UIBezierPath()
        xPath.move(to: CGPoint(x: 0.351563, y: 11.0156))
        xPath.addLine(to: CGPoint(x: 5.03906, y: 6.32812))
        xPath.addLine(to: CGPoint(x: 0.390626, y: 1.67969))
        xPath.addCurve(to: CGPoint(x: 0.390626, y: 0.390624), controlPoint1: CGPoint(x: 0, y: 1.32812), controlPoint2: CGPoint(x: 0, y: 0.742186))
        xPath.addCurve(to: CGPoint(x: 1.67969, y: 0.390624), controlPoint1: CGPoint(x: 0.742189, y: 0), controlPoint2: CGPoint(x: 1.32813, y: 0))
        xPath.addLine(to: CGPoint(x: 6.36719, y: 5.03906))
        xPath.addLine(to: CGPoint(x: 11.0156, y: 0.390625))
        xPath.addCurve(to: CGPoint(x: 12.3047, y: 0.390625), controlPoint1: CGPoint(x: 11.3672, y: 0), controlPoint2: CGPoint(x: 11.9531, y: 0))
        xPath.addCurve(to: CGPoint(x: 12.3047, y: 1.71875), controlPoint1: CGPoint(x: 12.6953, y: 0.742187), controlPoint2: CGPoint(x: 12.6953, y: 1.32812))
        xPath.addLine(to: CGPoint(x: 7.65625, y: 6.36719))
        xPath.addLine(to: CGPoint(x: 12.3047, y: 11.0156))
        xPath.addCurve(to: CGPoint(x: 12.3047, y: 12.3437), controlPoint1: CGPoint(x: 12.6953, y: 11.3672), controlPoint2: CGPoint(x: 12.6953, y: 11.9531))
        xPath.addCurve(to: CGPoint(x: 10.9766, y: 12.3437), controlPoint1: CGPoint(x: 11.9531, y: 12.6953), controlPoint2: CGPoint(x: 11.3672, y: 12.6953))
        xPath.addLine(to: CGPoint(x: 6.32813, y: 7.65625))
        xPath.addLine(to: CGPoint(x: 1.67969, y: 12.3047))
        xPath.addCurve(to: CGPoint(x: 0.351563, y: 12.3047), controlPoint1: CGPoint(x: 1.32813, y: 12.6953), controlPoint2: CGPoint(x: 0.742188, y: 12.6953))
        xPath.addCurve(to: CGPoint(x: 0.351563, y: 11.0156), controlPoint1: CGPoint(x: 0, y: 11.9531), controlPoint2: CGPoint(x: 0, y: 11.3672))
        xPath.close()
        xPath.apply(CGAffineTransform(scaleX: closeSize / 13.0, y: closeSize / 13.0))
        xLayer.path = xPath.cgPath
        xLayer.fillColor = self.chevronColor.cgColor
        if self.pipCloseStrokeWidth > 0 {
            xLayer.strokeColor = self.chevronColor.cgColor
            xLayer.lineWidth = self.pipCloseStrokeWidth
            xLayer.lineJoin = .round
        }
        closeBtn.layer.addSublayer(xLayer)
        containerView.addSubview(closeBtn)
        self.closeButton = closeBtn

        // Chevron-up — top-right — FA6 Pro Regular, viewBox 18x10, filled
        // Minimum 44x44 hit area per Apple HIG
        let chevronScale: CGFloat = self.pipChevronSize / 18.0
        let chevronW: CGFloat = self.pipChevronSize
        let chevronH: CGFloat = self.pipChevronHeight ?? (10.0 * chevronScale)
        let chevronHitW = max(chevronW, 44)
        let chevronHitH = max(chevronH, 44)
        let pathW: CGFloat = 18.0 * chevronScale
        let pathH: CGFloat = 10.0 * chevronScale
        let offsetX: CGFloat = (chevronW - pathW) / 2.0
        let offsetY: CGFloat = (chevronH - pathH) / 2.0
        let chevronUp = UIButton(type: .custom)
        chevronUp.frame = CGRect(x: size.width - chevronHitW - inset + (chevronHitW - chevronW) / 2,
                                 y: inset - (chevronHitH - chevronH) / 2,
                                 width: chevronHitW, height: chevronHitH)
        chevronUp.backgroundColor = .clear
        chevronUp.addTarget(self, action: #selector(chevronButtonTapped), for: .touchUpInside)
        let chevronUpLayer = CAShapeLayer()
        chevronUpLayer.frame = CGRect(x: (chevronHitW - chevronW) / 2, y: (chevronHitH - chevronH) / 2, width: chevronW, height: chevronH)
        let chevronUpPath = UIBezierPath()
        chevronUpPath.move(to: CGPoint(x: 7.89062, y: 0.351562))
        chevronUpPath.addCurve(to: CGPoint(x: 9.17969, y: 0.351562), controlPoint1: CGPoint(x: 8.24219, y: 0), controlPoint2: CGPoint(x: 8.82812, y: 0))
        chevronUpPath.addLine(to: CGPoint(x: 16.7188, y: 7.85156))
        chevronUpPath.addCurve(to: CGPoint(x: 16.7188, y: 9.17969), controlPoint1: CGPoint(x: 17.0703, y: 8.24219), controlPoint2: CGPoint(x: 17.0703, y: 8.82812))
        chevronUpPath.addCurve(to: CGPoint(x: 15.3906, y: 9.17969), controlPoint1: CGPoint(x: 16.3281, y: 9.57031), controlPoint2: CGPoint(x: 15.7422, y: 9.57031))
        chevronUpPath.addLine(to: CGPoint(x: 8.55469, y: 2.34375))
        chevronUpPath.addLine(to: CGPoint(x: 1.71875, y: 9.17969))
        chevronUpPath.addCurve(to: CGPoint(x: 0.390625, y: 9.17969), controlPoint1: CGPoint(x: 1.32812, y: 9.57031), controlPoint2: CGPoint(x: 0.742188, y: 9.57031))
        chevronUpPath.addCurve(to: CGPoint(x: 0.390625, y: 7.89062), controlPoint1: CGPoint(x: 0, y: 8.82812), controlPoint2: CGPoint(x: 0, y: 8.24219))
        chevronUpPath.addLine(to: CGPoint(x: 7.89062, y: 0.351562))
        chevronUpPath.close()
        chevronUpPath.apply(CGAffineTransform(scaleX: chevronScale, y: chevronScale))
        chevronUpPath.apply(CGAffineTransform(translationX: offsetX, y: offsetY))
        chevronUpLayer.path = chevronUpPath.cgPath
        chevronUpLayer.fillColor = self.chevronColor.cgColor
        if self.pipChevronStrokeWidth > 0 {
            chevronUpLayer.strokeColor = self.chevronColor.cgColor
            chevronUpLayer.lineWidth = self.pipChevronStrokeWidth
            chevronUpLayer.lineJoin = .round
        }
        chevronUp.layer.addSublayer(chevronUpLayer)
        containerView.addSubview(chevronUp)
        self.chevronUpView = chevronUp
        
        if let earnText = self.earnText, !earnText.isEmpty {
            let label = UILabel()
            label.textColor = self.earnTextColor
            if let fontFamily = self.earnTextFontFamily,
               let customFont = UIFont(name: fontFamily, size: self.earnTextFontSize) {
                label.font = customFont
            } else {
                label.font = UIFont.systemFont(ofSize: self.earnTextFontSize, weight: self.earnTextFontWeight)
            }
            label.textAlignment = .center
            if let bgColor = self.earnTextBackgroundColor {
                label.backgroundColor = bgColor
                label.layer.cornerRadius = 4
                label.clipsToBounds = true
            }
            // Apply text with optional line height via attributed string
            if let lineHeight = self.earnTextLineHeight {
                let paragraphStyle = NSMutableParagraphStyle()
                paragraphStyle.minimumLineHeight = lineHeight
                paragraphStyle.maximumLineHeight = lineHeight
                paragraphStyle.alignment = .center
                let attributes: [NSAttributedString.Key: Any] = [
                    .font: label.font!,
                    .foregroundColor: self.earnTextColor,
                    .paragraphStyle: paragraphStyle
                ]
                label.attributedText = NSAttributedString(string: earnText, attributes: attributes)
            } else {
                label.text = earnText
            }
            let labelWidth = earnText.size(withAttributes: [.font: label.font!]).width + 16
            let labelHeight: CGFloat = self.earnTextLineHeight ?? (self.earnTextFontSize + 6)
            label.frame = CGRect(
                x: (size.width - labelWidth) / 2,
                y: size.height - self.earnTextMargin - labelHeight,
                width: labelWidth,
                height: labelHeight
            )
            containerView.addSubview(label)
            self.earnLabel = label
        }
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
        closeButton = nil
        isMinimized = false
        
        // Reset drag state
        isDragging = false
        dragStartLocation = .zero
        pipWindowStartFrame = .zero
        lastPipPosition = nil
    }

    private static func parseFontWeight(_ value: String) -> UIFont.Weight {
        switch value.lowercased() {
        case "100": return .ultraLight
        case "200": return .thin
        case "300": return .light
        case "normal", "400": return .regular
        case "500": return .medium
        case "600": return .semibold
        case "bold", "700": return .bold
        case "800": return .heavy
        case "900": return .black
        default: return .semibold
        }
    }
}

protocol PictureInPictureManagerDelegate: AnyObject {
    func didMinimize()
    func didRestore()
}