import ExpoModulesCore
import Button
import UIKit

class PromotionManager {
    
    private var promotionData: NSDictionary?
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?
    private weak var currentBrowser: BrowserInterface?
    private var badgeLabel: String
    private var listTitle: String
    
    init(promotionData: NSDictionary?, onPromotionClickCallback: ((String, BrowserInterface?) -> Void)?, badgeLabel: String? = nil, listTitle: String? = nil) {
        self.promotionData = promotionData
        self.onPromotionClickCallback = onPromotionClickCallback
        self.badgeLabel = badgeLabel ?? "Offers"
        self.listTitle = listTitle ?? "Promotions"
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
        titleLabel.text = self.badgeLabel
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
        
        let promotionsVC = PromotionListViewController()
        promotionsVC.promotionData = promotionData
        promotionsVC.listTitle = self.listTitle
        promotionsVC.onPromotionSelected = { [weak self] promotionId in
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
            promotionsVC.modalPresentationStyle = .pageSheet
            if #available(iOS 15.0, *) {
                if let sheet = promotionsVC.sheetPresentationController {
                    sheet.detents = [.medium(), .large()]
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

// Simple custom view controller for promotions list
class PromotionListViewController: UIViewController {
    
    var promotionData: NSDictionary?
    var onPromotionSelected: ((String) -> Void)?
    var listTitle: String = "Promotions"
    
    private let scrollView = UIScrollView()
    private let contentView = UIView()
    private let stackView = UIStackView()
    
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
        stackView.spacing = 12
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
        let containerView = UIView()
        containerView.backgroundColor = UIColor.systemBackground
        
        // Add bottom border using a separator view
        let separatorView = UIView()
        separatorView.backgroundColor = UIColor.systemGray4
        separatorView.translatesAutoresizingMaskIntoConstraints = false
        containerView.addSubview(separatorView)
        
        let button = UIButton(type: .system)
        button.contentHorizontalAlignment = .left
        button.contentVerticalAlignment = .top
        button.translatesAutoresizingMaskIntoConstraints = false
        
        // Create title
        var title = promotion["title"] as? String ?? "Promotion"
        
        // Add NEW badge if promotion is new
        if let createdAt = promotion["createdAt"] as? String, isPromotionNew(createdAt) {
            title = "🆕 \(title)"
        }
        
        // Add feature indicator
        if isFeature {
            title = "⭐ \(title)"
        }
        
        // Create attributed string for multiple lines with better spacing
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineSpacing = 6 // Add spacing between lines
        paragraphStyle.paragraphSpacing = 12 // Add spacing between paragraphs
        
        let attributedTitle = NSMutableAttributedString(string: title, attributes: [
            .font: UIFont.systemFont(ofSize: 16, weight: .medium),
            .foregroundColor: UIColor.label,
            .paragraphStyle: paragraphStyle
        ])
        
        // Add reward text if available
        if let reward = rewardText, !reward.isEmpty {
            attributedTitle.append(NSAttributedString(string: "\n\(reward)", attributes: [
                .font: UIFont.systemFont(ofSize: 14),
                .foregroundColor: UIColor.systemBlue,
                .paragraphStyle: paragraphStyle
            ]))
        }
        
        // Add coupon code if available
        if let code = promotion["code"] as? String, !code.isEmpty {
            attributedTitle.append(NSAttributedString(string: "\nCode: \(code)", attributes: [
                .font: UIFont.systemFont(ofSize: 14),
                .foregroundColor: UIColor.systemGray,
                .paragraphStyle: paragraphStyle
            ]))
        }
        
        button.setAttributedTitle(attributedTitle, for: .normal)
        button.titleLabel?.numberOfLines = 0
        button.titleLabel?.lineBreakMode = .byWordWrapping
        
        // Ensure button can grow unlimited based on content
        button.setContentCompressionResistancePriority(.required, for: .vertical)
        button.setContentHuggingPriority(.defaultLow, for: .vertical)
        
        // Add tap action
        if let promotionId = promotion["id"] as? String {
            button.addAction(UIAction { [weak self] _ in
                self?.dismiss(animated: true) {
                    self?.onPromotionSelected?(promotionId)
                }
            }, for: .touchUpInside)
        }
        
        containerView.addSubview(button)
        
        NSLayoutConstraint.activate([
            // Button constraints
            button.topAnchor.constraint(equalTo: containerView.topAnchor, constant: 20),
            button.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 20),
            button.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -20),
            button.bottomAnchor.constraint(equalTo: containerView.bottomAnchor, constant: -28),
            
            // Separator constraints
            separatorView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            separatorView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            separatorView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor),
            separatorView.heightAnchor.constraint(equalToConstant: 1)
        ])
        
        return containerView
    }
    
    private func isPromotionNew(_ createdAt: String) -> Bool {
        let formatter = ISO8601DateFormatter()
        guard let createdDate = formatter.date(from: createdAt) else {
            return false
        }
        
        let twoDaysAgo = Calendar.current.date(byAdding: .day, value: -2, to: Date()) ?? Date()
        return createdDate >= twoDaysAgo
    }
}