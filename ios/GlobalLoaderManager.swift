import UIKit

class GlobalLoaderManager {
    
    static let shared = GlobalLoaderManager()
    
    private var currentLoaderView: UIView?
    private var currentWindow: UIWindow?
    
    private init() {}
    
    func showLoader(message: String = "Loading promotion...") {
        hideLoader()
        
        DispatchQueue.main.async { [weak self] in
            self?.createAndShowLoader(message: message)
        }
    }
    
    func showCopyLoader(promoCode: String) {
        hideLoader()
        
        DispatchQueue.main.async { [weak self] in
            self?.createAndShowCopyLoader(promoCode: promoCode)
        }
    }
    
    func hideLoader() {
        DispatchQueue.main.async { [weak self] in
            self?.removeLoader()
        }
    }
    
    private func createAndShowLoader(message: String) {
        // Get key window
        guard let keyWindow = getKeyWindow() else {
            print("❌ GlobalLoaderManager: Could not find key window")
            return
        }
        
        currentWindow = keyWindow
        
        // Screen overlay
        let overlay = UIView()
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        overlay.translatesAutoresizingMaskIntoConstraints = false
        
        // Loader container
        let loaderContainer = UIView()
        loaderContainer.backgroundColor = UIColor.white
        loaderContainer.layer.cornerRadius = 12
        loaderContainer.layer.shadowColor = UIColor.black.cgColor
        loaderContainer.layer.shadowOffset = CGSize(width: 0, height: 2)
        loaderContainer.layer.shadowOpacity = 0.3
        loaderContainer.layer.shadowRadius = 8
        loaderContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Activity indicator
        let activityIndicator = UIActivityIndicatorView(style: .large)
        activityIndicator.color = UIColor.systemBlue
        activityIndicator.startAnimating()
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        
        // Create loading label
        let loadingLabel = UILabel()
        loadingLabel.text = message
        loadingLabel.font = UIFont.systemFont(ofSize: 16)
        loadingLabel.textColor = UIColor.darkGray
        loadingLabel.textAlignment = .center
        loadingLabel.numberOfLines = 0
        loadingLabel.translatesAutoresizingMaskIntoConstraints = false
        
        // Add components to container
        loaderContainer.addSubview(activityIndicator)
        loaderContainer.addSubview(loadingLabel)
        
        // Add container to overlay
        overlay.addSubview(loaderContainer)
        
        // Add overlay to window (this ensures it appears above everything, including WebView)
        keyWindow.addSubview(overlay)
        
        // Set up constraints
        NSLayoutConstraint.activate([
            // Overlay fills entire window
            overlay.topAnchor.constraint(equalTo: keyWindow.topAnchor),
            overlay.leadingAnchor.constraint(equalTo: keyWindow.leadingAnchor),
            overlay.trailingAnchor.constraint(equalTo: keyWindow.trailingAnchor),
            overlay.bottomAnchor.constraint(equalTo: keyWindow.bottomAnchor),
            
            // Center loader container
            loaderContainer.centerXAnchor.constraint(equalTo: overlay.centerXAnchor),
            loaderContainer.centerYAnchor.constraint(equalTo: overlay.centerYAnchor),
            loaderContainer.widthAnchor.constraint(greaterThanOrEqualToConstant: 200),
            loaderContainer.heightAnchor.constraint(greaterThanOrEqualToConstant: 120),
            
            // Activity indicator at top of container
            activityIndicator.centerXAnchor.constraint(equalTo: loaderContainer.centerXAnchor),
            activityIndicator.topAnchor.constraint(equalTo: loaderContainer.topAnchor, constant: 24),
            
            // Loading label below activity indicator
            loadingLabel.centerXAnchor.constraint(equalTo: loaderContainer.centerXAnchor),
            loadingLabel.topAnchor.constraint(equalTo: activityIndicator.bottomAnchor, constant: 16),
            loadingLabel.leadingAnchor.constraint(greaterThanOrEqualTo: loaderContainer.leadingAnchor, constant: 24),
            loadingLabel.trailingAnchor.constraint(lessThanOrEqualTo: loaderContainer.trailingAnchor, constant: -24),
            loadingLabel.bottomAnchor.constraint(equalTo: loaderContainer.bottomAnchor, constant: -24)
        ])
        
        currentLoaderView = overlay
        
        print("✅ GlobalLoaderManager: Loader shown above all views")
    }
    
    private func createAndShowCopyLoader(promoCode: String) {
        // Get key window
        guard let keyWindow = getKeyWindow() else {
            print("❌ GlobalLoaderManager: Could not find key window")
            return
        }
        
        currentWindow = keyWindow
        
        // Screen overlay
        let overlay = UIView()
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        overlay.translatesAutoresizingMaskIntoConstraints = false
        
        // Loader container
        let loaderContainer = UIView()
        loaderContainer.backgroundColor = UIColor.white
        loaderContainer.layer.cornerRadius = 12
        loaderContainer.layer.shadowColor = UIColor.black.cgColor
        loaderContainer.layer.shadowOffset = CGSize(width: 0, height: 2)
        loaderContainer.layer.shadowOpacity = 0.3
        loaderContainer.layer.shadowRadius = 8
        loaderContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Activity indicator (blue)
        let activityIndicator = UIActivityIndicatorView(style: .large)
        activityIndicator.color = UIColor.systemBlue
        activityIndicator.startAnimating()
        activityIndicator.translatesAutoresizingMaskIntoConstraints = false
        
        // Create loading label
        let loadingLabel = UILabel()
        loadingLabel.text = "Copying to clipboard..."
        loadingLabel.font = UIFont.systemFont(ofSize: 16)
        loadingLabel.textColor = UIColor.darkGray
        loadingLabel.textAlignment = .center
        loadingLabel.numberOfLines = 0
        loadingLabel.translatesAutoresizingMaskIntoConstraints = false
        
        // Create promo code pill (same as in promotion cards)
        let promoCodePill = createPromoCodePill(promoCode: promoCode)
        
        // Create bottom label "Loading promotion..."
        let bottomLabel = UILabel()
        bottomLabel.text = "Loading promotion..."
        bottomLabel.font = UIFont.systemFont(ofSize: 14)
        bottomLabel.textColor = UIColor.lightGray
        bottomLabel.textAlignment = .center
        bottomLabel.numberOfLines = 0
        bottomLabel.translatesAutoresizingMaskIntoConstraints = false
        
        // Add components to container
        loaderContainer.addSubview(activityIndicator)
        loaderContainer.addSubview(loadingLabel)
        loaderContainer.addSubview(promoCodePill)
        loaderContainer.addSubview(bottomLabel)
        
        // Add container to overlay
        overlay.addSubview(loaderContainer)
        
        // Add overlay to window
        keyWindow.addSubview(overlay)
        
        // Set up constraints
        NSLayoutConstraint.activate([
            // Overlay fills entire window
            overlay.topAnchor.constraint(equalTo: keyWindow.topAnchor),
            overlay.leadingAnchor.constraint(equalTo: keyWindow.leadingAnchor),
            overlay.trailingAnchor.constraint(equalTo: keyWindow.trailingAnchor),
            overlay.bottomAnchor.constraint(equalTo: keyWindow.bottomAnchor),
            
            // Center loader container
            loaderContainer.centerXAnchor.constraint(equalTo: overlay.centerXAnchor),
            loaderContainer.centerYAnchor.constraint(equalTo: overlay.centerYAnchor),
            loaderContainer.widthAnchor.constraint(greaterThanOrEqualToConstant: 200),
            loaderContainer.heightAnchor.constraint(greaterThanOrEqualToConstant: 200),
            
            // Activity indicator at top of container
            activityIndicator.centerXAnchor.constraint(equalTo: loaderContainer.centerXAnchor),
            activityIndicator.topAnchor.constraint(equalTo: loaderContainer.topAnchor, constant: 24),
            
            // Loading label below activity indicator
            loadingLabel.centerXAnchor.constraint(equalTo: loaderContainer.centerXAnchor),
            loadingLabel.topAnchor.constraint(equalTo: activityIndicator.bottomAnchor, constant: 16),
            loadingLabel.leadingAnchor.constraint(greaterThanOrEqualTo: loaderContainer.leadingAnchor, constant: 24),
            loadingLabel.trailingAnchor.constraint(lessThanOrEqualTo: loaderContainer.trailingAnchor, constant: -24),
            
            // Promo code pill below loading label
            promoCodePill.centerXAnchor.constraint(equalTo: loaderContainer.centerXAnchor),
            promoCodePill.topAnchor.constraint(equalTo: loadingLabel.bottomAnchor, constant: 16),
            
            // Bottom label below promo code pill
            bottomLabel.centerXAnchor.constraint(equalTo: loaderContainer.centerXAnchor),
            bottomLabel.topAnchor.constraint(equalTo: promoCodePill.bottomAnchor, constant: 16),
            bottomLabel.leadingAnchor.constraint(greaterThanOrEqualTo: loaderContainer.leadingAnchor, constant: 24),
            bottomLabel.trailingAnchor.constraint(lessThanOrEqualTo: loaderContainer.trailingAnchor, constant: -24),
            bottomLabel.bottomAnchor.constraint(equalTo: loaderContainer.bottomAnchor, constant: -24)
        ])
        
        currentLoaderView = overlay
        
        print("✅ GlobalLoaderManager: Copy loader shown with promo code: \(promoCode)")
    }
    
    private func createPromoCodePill(promoCode: String) -> UIView {
        // Create pill container (same styling as promotion cards)
        let pillContainer = UIView()
        pillContainer.backgroundColor = UIColor(red: 0.937, green: 0.965, blue: 1.0, alpha: 1.0) // #EFF6FF blue-50
        pillContainer.layer.cornerRadius = 8
        pillContainer.layer.masksToBounds = true
        pillContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Create content stack
        let contentStack = UIStackView()
        contentStack.axis = .horizontal
        contentStack.alignment = .center
        contentStack.spacing = 4
        contentStack.translatesAutoresizingMaskIntoConstraints = false
        
        // Add tag icon (same as promotion cards)
        let tagIcon = createTagIcon()
        tagIcon.translatesAutoresizingMaskIntoConstraints = false
        contentStack.addArrangedSubview(tagIcon)
        
        // Promo code text
        let promoLabel = UILabel()
        promoLabel.text = promoCode
        promoLabel.font = UIFont.systemFont(ofSize: 10)
        promoLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0) // #0b72ac
        contentStack.addArrangedSubview(promoLabel)
        
        pillContainer.addSubview(contentStack)
        
        // Constraints
        NSLayoutConstraint.activate([
            tagIcon.widthAnchor.constraint(equalToConstant: 12),
            tagIcon.heightAnchor.constraint(equalToConstant: 12),
            
            contentStack.centerXAnchor.constraint(equalTo: pillContainer.centerXAnchor),
            contentStack.centerYAnchor.constraint(equalTo: pillContainer.centerYAnchor),
            contentStack.leadingAnchor.constraint(greaterThanOrEqualTo: pillContainer.leadingAnchor, constant: 12),
            contentStack.trailingAnchor.constraint(lessThanOrEqualTo: pillContainer.trailingAnchor, constant: -12),
            contentStack.topAnchor.constraint(greaterThanOrEqualTo: pillContainer.topAnchor, constant: 6),
            contentStack.bottomAnchor.constraint(lessThanOrEqualTo: pillContainer.bottomAnchor, constant: -6)
        ])
        
        return pillContainer
    }
    
    private func createTagIcon() -> UIView {
        let iconView = UIView()
        iconView.backgroundColor = UIColor.clear
        
        let iconLayer = CAShapeLayer()
        let path = UIBezierPath()
        
        // SVG path scaled to 12x12 (same as promotion cards)
        let scale: CGFloat = 12.0 / 24.0
        
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
        
        // Small dot
        let dotPath = UIBezierPath(ovalIn: CGRect(x: 7 * scale - 0.5, y: 7 * scale - 0.5, width: 1, height: 1))
        path.append(dotPath)
        
        iconLayer.path = path.cgPath
        iconLayer.fillColor = UIColor.clear.cgColor
        iconLayer.strokeColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0).cgColor
        iconLayer.lineWidth = 1.0 * scale
        iconLayer.lineCap = .round
        iconLayer.lineJoin = .round
        
        iconView.layer.addSublayer(iconLayer)
        
        return iconView
    }
    
    private func removeLoader() {
        currentLoaderView?.removeFromSuperview()
        currentLoaderView = nil
        currentWindow = nil
        print("✅ GlobalLoaderManager: Loader hidden")
    }
    
    private func getKeyWindow() -> UIWindow? {
        if #available(iOS 13.0, *) {
            return UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .flatMap { $0.windows }
                .first { $0.isKeyWindow }
        } else {
            return UIApplication.shared.keyWindow
        }
    }
}