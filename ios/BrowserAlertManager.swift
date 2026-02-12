import UIKit
import Button

class BrowserAlertManager {
    
    static func showExitConfirmationAlert(
        browser: BrowserInterface,
        title: String?,
        message: String?,
        stayButtonLabel: String?,
        leaveButtonLabel: String?,
        titleColor: UIColor?,
        stayButtonTextColor: UIColor?,
        stayButtonBackgroundColor: UIColor?,
        leaveButtonTextColor: UIColor?,
        leaveButtonBackgroundColor: UIColor?,
        buttonBorderColor: UIColor?,
        fontFamily: String?,
        messageColor: UIColor?,
        titleFontSize: CGFloat?,
        messageFontSize: CGFloat?,
        buttonFontSize: CGFloat?,
        completion: @escaping (Bool) -> Void
    ) {
#if DEBUG
        print("expo-button-sdk showExitConfirmationAlert called")
#endif

        DispatchQueue.main.async {
            let modalViewController = ExitConfirmationModalViewController(
                title: title ?? "Are you sure you want to leave?",
                message: message ?? "You may lose your progress and any available deals.",
                stayButtonLabel: stayButtonLabel ?? "Stay",
                leaveButtonLabel: leaveButtonLabel ?? "Leave",
                titleColor: titleColor,
                stayButtonTextColor: stayButtonTextColor,
                stayButtonBackgroundColor: stayButtonBackgroundColor,
                leaveButtonTextColor: leaveButtonTextColor,
                leaveButtonBackgroundColor: leaveButtonBackgroundColor,
                buttonBorderColor: buttonBorderColor,
                fontFamily: fontFamily,
                messageColor: messageColor,
                titleFontSize: titleFontSize,
                messageFontSize: messageFontSize,
                buttonFontSize: buttonFontSize,
                completion: completion
            )
            
            if let topViewController = BrowserAlertManager.getTopViewController() {
#if DEBUG
                print("expo-button-sdk presenting custom modal on: \(topViewController)")
#endif
                modalViewController.modalPresentationStyle = .overFullScreen
                modalViewController.modalTransitionStyle = .crossDissolve
                topViewController.present(modalViewController, animated: true, completion: nil)
            } else {
#if DEBUG
                print("expo-button-sdk ERROR: No top view controller found")
#endif
                completion(false) // Default to staying if we can't present
            }
        }
    }
    
    private static func getTopViewController() -> UIViewController? {
        // Try multiple approaches to find the top view controller
        
        // Approach 1: Using key window
        if let keyWindow = UIApplication.shared.windows.first(where: { $0.isKeyWindow }),
           let rootViewController = keyWindow.rootViewController {
            var topViewController = rootViewController
            while let presentedViewController = topViewController.presentedViewController {
                topViewController = presentedViewController
            }
            return topViewController
        }
        
        // Approach 2: Using window scene
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = windowScene.windows.first {
            var topViewController = window.rootViewController
            while let presentedViewController = topViewController?.presentedViewController {
                topViewController = presentedViewController
            }
            return topViewController
        }
        
        // Approach 3: Using shared application windows
        for window in UIApplication.shared.windows {
            if window.isKeyWindow {
                var topViewController = window.rootViewController
                while let presentedViewController = topViewController?.presentedViewController {
                    topViewController = presentedViewController
                }
                return topViewController
            }
        }
        
        return nil
    }
}

// MARK: - ExitConfirmationModalViewController
class ExitConfirmationModalViewController: UIViewController {

    private let titleText: String
    private let messageText: String
    private let stayButtonLabel: String
    private let leaveButtonLabel: String
    private let titleColor: UIColor
    private let stayButtonTextColor: UIColor
    private let stayButtonBgColor: UIColor
    private let leaveButtonTextColor: UIColor
    private let leaveButtonBgColor: UIColor
    private let buttonBorderColor: UIColor
    private let fontFamily: String?
    private let messageColor: UIColor
    private let titleFontSize: CGFloat
    private let messageFontSize: CGFloat
    private let buttonFontSize: CGFloat
    private let completion: (Bool) -> Void

    init(
        title: String,
        message: String,
        stayButtonLabel: String,
        leaveButtonLabel: String,
        titleColor: UIColor?,
        stayButtonTextColor: UIColor?,
        stayButtonBackgroundColor: UIColor?,
        leaveButtonTextColor: UIColor?,
        leaveButtonBackgroundColor: UIColor?,
        buttonBorderColor: UIColor?,
        fontFamily: String?,
        messageColor: UIColor?,
        titleFontSize: CGFloat?,
        messageFontSize: CGFloat?,
        buttonFontSize: CGFloat?,
        completion: @escaping (Bool) -> Void
    ) {
        self.titleText = title
        self.messageText = message
        self.stayButtonLabel = stayButtonLabel
        self.leaveButtonLabel = leaveButtonLabel
        self.titleColor = titleColor ?? UIColor(red: 0.111, green: 0.111, blue: 0.111, alpha: 1.0)
        self.stayButtonTextColor = stayButtonTextColor ?? .white
        self.stayButtonBgColor = stayButtonBackgroundColor ?? UIColor(red: 0.027, green: 0.290, blue: 0.482, alpha: 1.0)
        self.leaveButtonTextColor = leaveButtonTextColor ?? UIColor(red: 0.404, green: 0.427, blue: 0.455, alpha: 1.0)
        self.leaveButtonBgColor = leaveButtonBackgroundColor ?? .white
        self.buttonBorderColor = buttonBorderColor ?? UIColor(red: 0.827, green: 0.843, blue: 0.859, alpha: 1.0)
        self.fontFamily = fontFamily
        self.messageColor = messageColor ?? UIColor(red: 0.156, green: 0.163, blue: 0.176, alpha: 1.0)
        self.titleFontSize = titleFontSize ?? 20
        self.messageFontSize = messageFontSize ?? 14
        self.buttonFontSize = buttonFontSize ?? 14
        self.completion = completion
        super.init(nibName: nil, bundle: nil)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }
    
    private func setupUI() {
        view.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        
        // Modal container
        let modalContainer = UIView()
        modalContainer.backgroundColor = .white
        modalContainer.layer.cornerRadius = 12
        modalContainer.layer.shadowColor = UIColor.black.cgColor
        modalContainer.layer.shadowOffset = CGSize(width: 0, height: 4)
        modalContainer.layer.shadowOpacity = 0.1
        modalContainer.layer.shadowRadius = 16
        modalContainer.translatesAutoresizingMaskIntoConstraints = false
        
        let contentContainer = UIView()
        contentContainer.translatesAutoresizingMaskIntoConstraints = false
        
        let titleSection = UIView()
        titleSection.translatesAutoresizingMaskIntoConstraints = false
        
        let titleLabel = UILabel()
        titleLabel.text = titleText
        titleLabel.font = fontFamily.flatMap({ UIFont(name: $0, size: titleFontSize) }) ?? UIFont.boldSystemFont(ofSize: titleFontSize)
        titleLabel.textColor = titleColor
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 0
        titleLabel.translatesAutoresizingMaskIntoConstraints = false

        // Subtitle
        let subtitleLabel = UILabel()
        subtitleLabel.text = messageText
        subtitleLabel.font = fontFamily.flatMap({ UIFont(name: $0, size: messageFontSize) }) ?? UIFont.systemFont(ofSize: messageFontSize)
        subtitleLabel.textColor = messageColor
        subtitleLabel.textAlignment = .center
        subtitleLabel.numberOfLines = 0
        subtitleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        let buttonContainer = UIStackView()
        buttonContainer.axis = .horizontal
        buttonContainer.distribution = .fillEqually
        buttonContainer.spacing = 8
        buttonContainer.translatesAutoresizingMaskIntoConstraints = false
        
        let stayButton = createButton(
            title: stayButtonLabel,
            backgroundColor: stayButtonBgColor,
            textColor: stayButtonTextColor,
            action: #selector(stayButtonTapped)
        )
        stayButton.layer.borderWidth = 2.5
        stayButton.layer.borderColor = buttonBorderColor.cgColor

        let leaveButton = createButton(
            title: leaveButtonLabel,
            backgroundColor: leaveButtonBgColor,
            textColor: leaveButtonTextColor,
            action: #selector(leaveButtonTapped)
        )
        leaveButton.layer.borderWidth = 2.5
        leaveButton.layer.borderColor = buttonBorderColor.cgColor
        
        // Add views to hierarchy
        view.addSubview(modalContainer)
        modalContainer.addSubview(contentContainer)
        contentContainer.addSubview(titleSection)
        titleSection.addSubview(titleLabel)
        titleSection.addSubview(subtitleLabel)
        contentContainer.addSubview(buttonContainer)
        buttonContainer.addArrangedSubview(leaveButton)
        buttonContainer.addArrangedSubview(stayButton)
        
        // Setup constraints
        NSLayoutConstraint.activate([
            // Modal container - centered in screen with min 85% width
            modalContainer.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            modalContainer.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            modalContainer.leadingAnchor.constraint(greaterThanOrEqualTo: view.leadingAnchor, constant: 30),
            modalContainer.trailingAnchor.constraint(lessThanOrEqualTo: view.trailingAnchor, constant: -30),
            modalContainer.widthAnchor.constraint(greaterThanOrEqualTo: view.widthAnchor, multiplier: 0.75),
            modalContainer.widthAnchor.constraint(lessThanOrEqualToConstant: 500),
            
            // Content container padding
            contentContainer.topAnchor.constraint(equalTo: modalContainer.topAnchor, constant: 32),
            contentContainer.bottomAnchor.constraint(equalTo: modalContainer.bottomAnchor, constant: -32),
            contentContainer.leadingAnchor.constraint(equalTo: modalContainer.leadingAnchor, constant: 32),
            contentContainer.trailingAnchor.constraint(equalTo: modalContainer.trailingAnchor, constant: -32),
            
            // Title section
            titleSection.topAnchor.constraint(equalTo: contentContainer.topAnchor),
            titleSection.leadingAnchor.constraint(equalTo: contentContainer.leadingAnchor),
            titleSection.trailingAnchor.constraint(equalTo: contentContainer.trailingAnchor),
            
            // Title label
            titleLabel.topAnchor.constraint(equalTo: titleSection.topAnchor),
            titleLabel.leadingAnchor.constraint(equalTo: titleSection.leadingAnchor),
            titleLabel.trailingAnchor.constraint(equalTo: titleSection.trailingAnchor),
            
            // Subtitle label
            subtitleLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 16),
            subtitleLabel.leadingAnchor.constraint(equalTo: titleSection.leadingAnchor),
            subtitleLabel.trailingAnchor.constraint(equalTo: titleSection.trailingAnchor),
            subtitleLabel.bottomAnchor.constraint(equalTo: titleSection.bottomAnchor),
            
            // Button container
            buttonContainer.topAnchor.constraint(equalTo: titleSection.bottomAnchor, constant: 24),
            buttonContainer.leadingAnchor.constraint(equalTo: contentContainer.leadingAnchor),
            buttonContainer.trailingAnchor.constraint(equalTo: contentContainer.trailingAnchor),
            buttonContainer.bottomAnchor.constraint(equalTo: contentContainer.bottomAnchor),
            buttonContainer.heightAnchor.constraint(equalToConstant: 40)
        ])
        
        // Background tap gesture
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(backgroundTapped))
        view.addGestureRecognizer(tapGesture)
    }
    
    private func createButton(title: String, backgroundColor: UIColor, textColor: UIColor, action: Selector) -> UIButton {
        let button = UIButton(type: .custom)
        button.setTitle(title, color: textColor)
        button.backgroundColor = backgroundColor
        button.titleLabel?.font = fontFamily.flatMap({ UIFont(name: $0, size: buttonFontSize) }) ?? UIFont.systemFont(ofSize: buttonFontSize, weight: .medium)
        button.contentEdgeInsets = UIEdgeInsets(top: 8, left: 12, bottom: 8, right: 12)
        button.titleLabel?.adjustsFontSizeToFitWidth = true
        button.titleLabel?.minimumScaleFactor = 0.75
        button.layer.cornerRadius = 20
        button.layer.shadowColor = UIColor.black.cgColor
        button.layer.shadowOffset = CGSize(width: 0, height: 1)
        button.layer.shadowOpacity = 0.05
        button.layer.shadowRadius = 2
        button.addTarget(self, action: action, for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        
        // Press effect
        button.addTarget(self, action: #selector(buttonTouchDown(_:)), for: .touchDown)
        button.addTarget(self, action: #selector(buttonTouchUp(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel])
        
        return button
    }
    
    @objc private func stayButtonTapped() {
        dismiss(animated: true) {
            self.completion(false)
        }
    }
    
    @objc private func leaveButtonTapped() {
        dismiss(animated: true) {
            self.completion(true)
        }
    }
    
    @objc private func backgroundTapped() {
        // Dismiss and stay
        stayButtonTapped()
    }
    
    @objc private func buttonTouchDown(_ sender: UIButton) {
        UIView.animate(withDuration: 0.1) {
            sender.transform = CGAffineTransform(scaleX: 0.95, y: 0.95)
            sender.alpha = 0.8
        }
    }
    
    @objc private func buttonTouchUp(_ sender: UIButton) {
        UIView.animate(withDuration: 0.1) {
            sender.transform = .identity
            sender.alpha = 1.0
        }
    }
}

extension UIButton {
    func setTitle(_ title: String, color: UIColor) {
        setTitle(title, for: .normal)
        setTitleColor(color, for: .normal)
    }
}
