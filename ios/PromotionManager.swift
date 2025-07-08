import ExpoModulesCore
import Button
import UIKit

class PromotionManager {
    
    private var promotionData: NSDictionary?
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?
    private weak var currentBrowser: BrowserInterface?
    
    init(promotionData: NSDictionary?, onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?) {
        self.promotionData = promotionData
        self.onPromotionClickCallback = onPromotionClickCallback
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
            browser.header.customActionView = badgeView
            
            // Add tap gesture to badge
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(promotionsBadgeTapped))
            badgeView.addGestureRecognizer(tapGesture)
            badgeView.isUserInteractionEnabled = true
        }
    }
    
    private func createPromotionBadge(count: Int) -> UIView {
        let containerView = UIView()
        
        let badgeView = UIView()
        badgeView.backgroundColor = UIColor.systemRed
        badgeView.layer.cornerRadius = 12
        badgeView.translatesAutoresizingMaskIntoConstraints = false
        
        let badgeLabel = UILabel()
        badgeLabel.text = "\(count)"
        badgeLabel.font = UIFont.boldSystemFont(ofSize: 12)
        badgeLabel.textColor = UIColor.white
        badgeLabel.textAlignment = .center
        badgeLabel.translatesAutoresizingMaskIntoConstraints = false
        
        // Create icon + text stack
        let iconLabel = UILabel()
        iconLabel.text = "🏷️"
        iconLabel.font = UIFont.systemFont(ofSize: 16)
        iconLabel.translatesAutoresizingMaskIntoConstraints = false
        
        let titleLabel = UILabel()
        titleLabel.text = "Offers"
        titleLabel.font = UIFont.systemFont(ofSize: 14)
        titleLabel.textColor = UIColor.white
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addSubview(iconLabel)
        containerView.addSubview(titleLabel)
        containerView.addSubview(badgeView)
        badgeView.addSubview(badgeLabel)
        
        NSLayoutConstraint.activate([
            iconLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            iconLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            iconLabel.topAnchor.constraint(equalTo: containerView.topAnchor),
            iconLabel.bottomAnchor.constraint(equalTo: containerView.bottomAnchor),
            
            titleLabel.leadingAnchor.constraint(equalTo: iconLabel.trailingAnchor, constant: 4),
            titleLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            
            badgeView.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor, constant: 4),
            badgeView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            badgeView.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
            badgeView.widthAnchor.constraint(equalToConstant: 24),
            badgeView.heightAnchor.constraint(equalToConstant: 24),
            
            badgeLabel.centerXAnchor.constraint(equalTo: badgeView.centerXAnchor),
            badgeLabel.centerYAnchor.constraint(equalTo: badgeView.centerYAnchor)
        ])
        
        return containerView
    }
    
    @objc private func promotionsBadgeTapped() {
        showPromotionsList()
    }
    
    private func showPromotionsList() {
        guard let promotionData = self.promotionData else { return }
        
        let alertController = UIAlertController(title: "Promotions", message: nil, preferredStyle: .actionSheet)
        
        let merchantName = promotionData["merchantName"] as? String ?? "Store"
        let rewardText = promotionData["rewardText"] as? String
        
        // Add featured promotion if available
        if let featuredPromotion = promotionData["featuredPromotion"] as? [String: Any] {
            let actionTitle = createPromotionActionTitle(promotion: featuredPromotion, isFeature: true, rewardText: rewardText)
            let action = UIAlertAction(title: actionTitle, style: .default) { [weak self] _ in
                if let promotionId = featuredPromotion["id"] as? String {
                    // Show global loader over everything (including WebView)
                    GlobalLoaderManager.shared.showLoader(message: "Loading promotion...")
                    print("🔄 Global loader shown for featured promotion")
                    
                    self?.onPromotionClickCallback?(promotionId, self?.currentBrowser)
                }
            }
            alertController.addAction(action)
        }
        
        // Add regular promotions
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        for promotion in promotions {
            let actionTitle = createPromotionActionTitle(promotion: promotion, isFeature: false, rewardText: rewardText)
            let action = UIAlertAction(title: actionTitle, style: .default) { [weak self] _ in
                if let promotionId = promotion["id"] as? String {
                    // Show global loader over everything (including WebView)
                    GlobalLoaderManager.shared.showLoader(message: "Loading promotion...")
                    print("🔄 Global loader shown for promotion")
                    
                    self?.onPromotionClickCallback?(promotionId, self?.currentBrowser)
                }
            }
            alertController.addAction(action)
        }
        
        alertController.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        
        // Present from the top-most view controller to ensure it's above the webview
        if let viewController = getTopMostViewController() {
            // For iPad support
            if let popover = alertController.popoverPresentationController {
                popover.sourceView = viewController.view
                popover.sourceRect = CGRect(x: viewController.view.bounds.midX, y: 100, width: 0, height: 0)
                popover.permittedArrowDirections = .up
            }
            
            // Set maximum window level to appear above everything
            alertController.view.layer.zPosition = CGFloat.greatestFiniteMagnitude
            
            viewController.present(alertController, animated: true)
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