import UIKit
import ExpoModulesCore // Assuming this might be needed for the module context, though not directly used in the VC itself.

class PromotionBottomSheetViewController: UIViewController {
    
    var promotionData: NSDictionary?
    var onPromotionSelected: ((String) -> Void)?
    var listTitle: String = "Promotions"
    
    private let scrollView = UIScrollView()
    private let contentView = UIView()
    private let stackView = UIStackView()
    private var promotionIdMap: [Int: String] = [:]
    private var promotionDataMap: [Int: [String: Any]] = [:]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupUI()
        setupPromotions()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor.white
        
        let titleLabel = UILabel()
        titleLabel.text = listTitle
        titleLabel.font = UIFont.boldSystemFont(ofSize: 18)
        titleLabel.textColor = UIColor.black
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        contentView.translatesAutoresizingMaskIntoConstraints = false
        
        stackView.axis = .vertical
        stackView.spacing = 16
        stackView.translatesAutoresizingMaskIntoConstraints = false
        
        view.addSubview(titleLabel)
        view.addSubview(scrollView)
        scrollView.addSubview(contentView)
        contentView.addSubview(stackView)
        
        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 20),
            titleLabel.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            titleLabel.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            
            scrollView.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 20),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            
            contentView.topAnchor.constraint(equalTo: scrollView.topAnchor),
            contentView.leadingAnchor.constraint(equalTo: scrollView.leadingAnchor),
            contentView.trailingAnchor.constraint(equalTo: scrollView.trailingAnchor),
            contentView.bottomAnchor.constraint(equalTo: scrollView.bottomAnchor),
            contentView.widthAnchor.constraint(equalTo: scrollView.widthAnchor),
            
            stackView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 16),
            stackView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            stackView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            stackView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -30)
        ])
    }
    
    private func setupPromotions() {
        guard let promotionData = self.promotionData else { return }
        
        if let featuredPromotion = promotionData["featuredPromotion"] as? [String: Any] {
            let promotionView = createPromotionView(promotion: featuredPromotion, isFeature: true)
            stackView.addArrangedSubview(promotionView)
        }
        
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        for promotion in promotions {
            let promotionView = createPromotionView(promotion: promotion, isFeature: false)
            stackView.addArrangedSubview(promotionView)
        }
    }
    
    private func createPromotionView(promotion: [String: Any], isFeature: Bool) -> UIView {
        let cardContainer = UIView()
        cardContainer.backgroundColor = UIColor.white
        cardContainer.layer.cornerRadius = 12
        cardContainer.layer.borderWidth = 2
        cardContainer.layer.borderColor = UIColor(red: 0.898, green: 0.906, blue: 0.922, alpha: 1.0).cgColor
        cardContainer.translatesAutoresizingMaskIntoConstraints = false
        
        let containerView = UIView()
        containerView.translatesAutoresizingMaskIntoConstraints = false
        cardContainer.addSubview(containerView)
        
        let mainStack = UIStackView()
        mainStack.axis = .vertical
        mainStack.spacing = 12
        mainStack.translatesAutoresizingMaskIntoConstraints = false
        
        let titleContainer = UIStackView()
        titleContainer.axis = .horizontal
        titleContainer.alignment = .center
        titleContainer.spacing = 8
        titleContainer.translatesAutoresizingMaskIntoConstraints = false
        
        var title = promotion["description"] as? String ?? promotion["title"] as? String ?? "Promotion"
        
        var timeLabel: String? = nil
        if let startsAt = promotion["startsAt"] as? String {
            let startDiff = calculateDaysDifference(startsAt)
            if startDiff < 3 {
                timeLabel = "NEW!"
            } else if startDiff <= 7 {
                timeLabel = "THIS WEEK!"
            }
        }
        
        let timeLabels = ["THIS WEEK!", "NEW!", "TODAY!"]
        for label in timeLabels {
            title = title.replacingOccurrences(of: label, with: "").trimmingCharacters(in: .whitespaces)
        }
        
        if let label = timeLabel {
            let labelView = UILabel()
            labelView.text = label
            labelView.font = UIFont.systemFont(ofSize: 12, weight: .bold)
            labelView.textColor = UIColor(red: 0.018, green: 0.471, blue: 0.341, alpha: 1.0)
            labelView.backgroundColor = UIColor(red: 0.925, green: 0.992, blue: 0.961, alpha: 1.0)
            labelView.layer.cornerRadius = 4
            labelView.layer.masksToBounds = true
            labelView.textAlignment = .center
            
            labelView.translatesAutoresizingMaskIntoConstraints = false
            let paddingView = UIView()
            paddingView.addSubview(labelView)
            paddingView.translatesAutoresizingMaskIntoConstraints = false
            
            NSLayoutConstraint.activate([
                labelView.leadingAnchor.constraint(equalTo: paddingView.leadingAnchor, constant: 8),
                labelView.trailingAnchor.constraint(equalTo: paddingView.trailingAnchor, constant: -8),
                labelView.topAnchor.constraint(equalTo: paddingView.topAnchor, constant: 2),
                labelView.bottomAnchor.constraint(equalTo: paddingView.bottomAnchor, constant: -2)
            ])
            
            titleContainer.addArrangedSubview(paddingView)
        }
        
        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.font = UIFont.systemFont(ofSize: 16)
        titleLabel.textColor = UIColor(red: 0.220, green: 0.255, blue: 0.318, alpha: 1.0)
        titleLabel.numberOfLines = 2
        titleLabel.lineBreakMode = .byWordWrapping
        titleLabel.setContentHuggingPriority(.defaultLow, for: .horizontal)
        titleContainer.addArrangedSubview(titleLabel)
        
        mainStack.addArrangedSubview(titleContainer)
        
        let bottomContainer = UIStackView()
        bottomContainer.axis = .horizontal
        bottomContainer.alignment = .center
        bottomContainer.spacing = 8
        bottomContainer.translatesAutoresizingMaskIntoConstraints = false
        
        var cashbackText = ""
        let promotionRewardText = promotion["rewardText"] as? String ?? promotion["description"] as? String ?? ""
        if !promotionRewardText.isEmpty {
            let cashbackRegex = try! NSRegularExpression(pattern: "(\\\\d+% Cashback)")
            let range = NSRange(location: 0, length: promotionRewardText.count)
            if let match = cashbackRegex.firstMatch(in: promotionRewardText, range: range) {
                cashbackText = String(promotionRewardText[Range(match.range, in: promotionRewardText)!])
            }
        }
        
        if !cashbackText.isEmpty {
            let cashbackLabel = UILabel()
            cashbackLabel.text = cashbackText
            cashbackLabel.font = UIFont.systemFont(ofSize: 14)
            cashbackLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0)
            bottomContainer.addArrangedSubview(cashbackLabel)
        }
        
        if let promoCode = promotion["couponCode"] as? String ?? promotion["code"] as? String, !promoCode.isEmpty {
            if !cashbackText.isEmpty {
                let bulletLabel = UILabel()
                bulletLabel.text = "•"
                bulletLabel.font = UIFont.systemFont(ofSize: 14)
                bulletLabel.textColor = UIColor(red: 0.419, green: 0.447, blue: 0.502, alpha: 1.0)
                bottomContainer.addArrangedSubview(bulletLabel)
            }
            
            let promoButton = UIButton(type: .custom)
            promoButton.backgroundColor = UIColor(red: 0.937, green: 0.965, blue: 1.0, alpha: 1.0)
            promoButton.layer.cornerRadius = 8
            promoButton.layer.masksToBounds = true
            promoButton.contentEdgeInsets = UIEdgeInsets(top: 6, left: 12, bottom: 6, right: 12)
            promoButton.translatesAutoresizingMaskIntoConstraints = false
            
            let buttonStack = UIStackView()
            buttonStack.axis = .horizontal
            buttonStack.alignment = .center
            buttonStack.spacing = 4
            buttonStack.isUserInteractionEnabled = false
            buttonStack.translatesAutoresizingMaskIntoConstraints = false
            
            // Using the centralized factory for tag icon
            let tagIcon = PromotionUIFactory.createTagIcon(fontScale: 1.0) // Assuming default scale for this context
            tagIcon.translatesAutoresizingMaskIntoConstraints = false
            buttonStack.addArrangedSubview(tagIcon)
            
            let promoLabel = UILabel()
            promoLabel.text = promoCode
            promoLabel.font = UIFont.systemFont(ofSize: 10)
            promoLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0)
            buttonStack.addArrangedSubview(promoLabel)
            
            promoButton.addSubview(buttonStack)
            
            NSLayoutConstraint.activate([
                tagIcon.widthAnchor.constraint(equalToConstant: 12),
                tagIcon.heightAnchor.constraint(equalToConstant: 12),
                
                buttonStack.centerXAnchor.constraint(equalTo: promoButton.centerXAnchor),
                buttonStack.centerYAnchor.constraint(equalTo: promoButton.centerYAnchor),
                buttonStack.leadingAnchor.constraint(greaterThanOrEqualTo: promoButton.leadingAnchor, constant: 12),
                buttonStack.trailingAnchor.constraint(lessThanOrEqualTo: promoButton.trailingAnchor, constant: -12)
            ])
            
            if let promotionId = promotion["id"] as? String {
                promoButton.addTarget(self, action: #selector(promoCodeTouchDown(_:)), for: .touchDown)
                promoButton.addTarget(self, action: #selector(promoCodeTouchUp(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel])
                promoButton.addTarget(self, action: #selector(promoCodeTapped(_:)), for: .touchUpInside)
                promoButton.tag = promotionId.hashValue
                promotionIdMap[promotionId.hashValue] = promotionId
                promotionDataMap[promotionId.hashValue] = promotion
            }
            
            bottomContainer.addArrangedSubview(promoButton)
        }
        
        if let endsAt = promotion["endsAt"] as? String, let timeRemaining = calculateTimeRemaining(endsAt) {
            if !cashbackText.isEmpty || (promotion["couponCode"] as? String ?? promotion["code"] as? String) != nil {
                let bulletLabel = UILabel()
                bulletLabel.text = "•"
                bulletLabel.font = UIFont.systemFont(ofSize: 14)
                bulletLabel.textColor = UIColor(red: 0.419, green: 0.447, blue: 0.502, alpha: 1.0)
                bottomContainer.addArrangedSubview(bulletLabel)
            }
            
            let timeLabel = UILabel()
            timeLabel.text = timeRemaining
            timeLabel.font = UIFont.systemFont(ofSize: 12)
            timeLabel.textColor = UIColor(red: 0.419, green: 0.447, blue: 0.502, alpha: 1.0)
            bottomContainer.addArrangedSubview(timeLabel)
        }
        
        let spacer = UIView()
        spacer.setContentHuggingPriority(.defaultLow, for: .horizontal)
        bottomContainer.addArrangedSubview(spacer)
        
        mainStack.addArrangedSubview(bottomContainer)
        
        if let promotionId = promotion["id"] as? String {
            let tag = promotionId.hashValue
            let tapGesture = UITapGestureRecognizer(target: self, action: #selector(cardTapped(_:)))
            cardContainer.tag = tag
            promotionIdMap[tag] = promotionId
            promotionDataMap[tag] = promotion
            cardContainer.addGestureRecognizer(tapGesture)
            cardContainer.isUserInteractionEnabled = true
        }
        
        containerView.addSubview(mainStack)
        
        NSLayoutConstraint.activate([
            containerView.leadingAnchor.constraint(equalTo: cardContainer.leadingAnchor, constant: 16),
            containerView.trailingAnchor.constraint(equalTo: cardContainer.trailingAnchor, constant: -16),
            containerView.topAnchor.constraint(equalTo: cardContainer.topAnchor, constant: 16),
            containerView.bottomAnchor.constraint(equalTo: cardContainer.bottomAnchor, constant: -16),
            
            mainStack.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            mainStack.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            mainStack.topAnchor.constraint(equalTo: containerView.topAnchor),
            mainStack.bottomAnchor.constraint(equalTo: containerView.bottomAnchor)
        ])
        
        return cardContainer
    }
    
    @objc private func cardTapped(_ gesture: UITapGestureRecognizer) {
        guard let view = gesture.view,
              let promotionId = promotionIdMap[view.tag] else { return }
        
        dismiss(animated: true) {
            self.onPromotionSelected?(promotionId)
        }
    }
    
    @objc private func promoCodeTouchDown(_ sender: UIButton) {
        UIView.animate(withDuration: 0.1, animations: {
            sender.transform = CGAffineTransform(scaleX: 0.95, y: 0.95)
            sender.alpha = 0.8
        })
    }
    
    @objc private func promoCodeTouchUp(_ sender: UIButton) {
        UIView.animate(withDuration: 0.1, animations: {
            sender.transform = .identity
            sender.alpha = 1.0
        })
    }

    @objc private func promoCodeTapped(_ sender: UIButton) {
        guard let promotionId = promotionIdMap[sender.tag] else { return }
        
        dismiss(animated: true) {
            self.onPromotionSelected?(promotionId)
        }
    }
    
    private func getPromoCodeForPromotionId(_ promotionId: String) -> String? {
        guard let promotionData = self.promotionData else { return nil }
        
        if let featuredPromotion = promotionData["featuredPromotion"] as? [String: Any],
           let id = featuredPromotion["id"] as? String, id == promotionId {
            return featuredPromotion["couponCode"] as? String ?? featuredPromotion["code"] as? String
        }
        
        let promotions = promotionData["promotions"] as? [[String: Any]] ?? []
        for promotion in promotions {
            if let id = promotion["id"] as? String, id == promotionId {
                return promotion["couponCode"] as? String ?? promotion["code"] as? String
            }
        }
        
        return nil
    }
    
    private func calculateDaysDifference(_ dateStr: String) -> Int {
        let formatter = ISO8601DateFormatter()
        guard let startDate = formatter.date(from: dateStr) else {
            return Int.max
        }
        
        let now = Date()
        let calendar = Calendar.current
        
        let nowComponents = calendar.dateComponents([.year, .month, .day], from: now)
        let startComponents = calendar.dateComponents([.year, .month, .day], from: startDate)
        
        guard let nowDate = calendar.date(from: nowComponents),
              let startDateOnly = calendar.date(from: startComponents) else {
            return Int.max
        }
        
        let daysDiff = calendar.dateComponents([.day], from: startDateOnly, to: nowDate).day ?? Int.max
        return abs(daysDiff)
    }
    
    private func calculateTimeRemaining(_ endsAt: String) -> String? {
        let formatter = ISO8601DateFormatter()
        guard let expirationDate = formatter.date(from: endsAt) else {
            return nil
        }
        
        let now = Date()
        let timeDiff = expirationDate.timeIntervalSince(now)
        
        if timeDiff <= 0 { return nil }
        
        let days = Int(timeDiff / (24 * 60 * 60))
        
        if days > 7 { return nil }
        
        let hours = Int((timeDiff.truncatingRemainder(dividingBy: 24 * 60 * 60)) / (60 * 60))
        
        switch (days, hours) {
        case (let d, _) where d > 1:
            return "ends in \(d)d"
        case (1, _):
            return "ends tomorrow"
        case (0, let h) where h > 1:
            return "ends in \(h)h"
        case (0, 1):
            return "ends in 1h"
        default:
            return "ends today"
        }
    }
}
