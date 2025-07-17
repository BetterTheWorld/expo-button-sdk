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
        containerView.backgroundColor = UIColor(red: 0.976, green: 0.976, blue: 0.984, alpha: 1.0)
        containerView.layer.cornerRadius = 13
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        // Create icon + text stack
        let iconView = createTagIcon()
        iconView.translatesAutoresizingMaskIntoConstraints = false
        
        let titleLabel = UILabel()
        titleLabel.text = self.badgeLabel
        titleLabel.font = UIFont.systemFont(ofSize: 11)
        titleLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0) // #0b72ac
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addSubview(iconView)
        containerView.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            iconView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 6),
            iconView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: 12),
            iconView.heightAnchor.constraint(equalToConstant: 12),
            
            titleLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 3),
            titleLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -6),
            titleLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            
            // Ensure containerView has appropriate height for pill design
            containerView.heightAnchor.constraint(equalToConstant: 26),
            titleLabel.topAnchor.constraint(greaterThanOrEqualTo: containerView.topAnchor, constant: 4),
            titleLabel.bottomAnchor.constraint(lessThanOrEqualTo: containerView.bottomAnchor, constant: -4)
        ])
        
        return containerView
    }
    
    private func createTagIcon() -> UIView {
        let iconView = UIView()
        iconView.backgroundColor = UIColor.clear
        
        let iconLayer = CAShapeLayer()
        let path = UIBezierPath()
        
        // Create exact SVG path from HTML (scaled to 12x12)
        let scale: CGFloat = 12.0 / 24.0 // Scale from 24x24 to 12x12
        
        // Main tag shape path: M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z
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
        
        // Add the small dot: M7 7h.01
        let dotPath = UIBezierPath(ovalIn: CGRect(x: 7 * scale - 0.5, y: 7 * scale - 0.5, width: 1, height: 1))
        path.append(dotPath)
        
        iconLayer.path = path.cgPath
        iconLayer.fillColor = UIColor.clear.cgColor
        iconLayer.strokeColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0).cgColor // #0b72ac
        iconLayer.lineWidth = 2.0 * scale
        iconLayer.lineCap = .round
        iconLayer.lineJoin = .round
        
        iconView.layer.addSublayer(iconLayer)
        
        return iconView
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
