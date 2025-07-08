import UIKit

class GlobalLoaderManager {
    
    static let shared = GlobalLoaderManager()
    
    private var currentLoaderView: UIView?
    private var currentWindow: UIWindow?
    
    private init() {}
    
    func showLoader(message: String = "Loading promotion...") {
        hideLoader() // Remove any existing loader first
        
        DispatchQueue.main.async { [weak self] in
            self?.createAndShowLoader(message: message)
        }
    }
    
    func hideLoader() {
        DispatchQueue.main.async { [weak self] in
            self?.removeLoader()
        }
    }
    
    private func createAndShowLoader(message: String) {
        // Get the key window to show loader above everything
        guard let keyWindow = getKeyWindow() else {
            print("❌ GlobalLoaderManager: Could not find key window")
            return
        }
        
        currentWindow = keyWindow
        
        // Create overlay that covers the entire screen
        let overlay = UIView()
        overlay.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        overlay.translatesAutoresizingMaskIntoConstraints = false
        
        // Create loader container
        let loaderContainer = UIView()
        loaderContainer.backgroundColor = UIColor.white
        loaderContainer.layer.cornerRadius = 12
        loaderContainer.layer.shadowColor = UIColor.black.cgColor
        loaderContainer.layer.shadowOffset = CGSize(width: 0, height: 2)
        loaderContainer.layer.shadowOpacity = 0.3
        loaderContainer.layer.shadowRadius = 8
        loaderContainer.translatesAutoresizingMaskIntoConstraints = false
        
        // Create activity indicator
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