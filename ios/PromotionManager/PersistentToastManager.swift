import UIKit

class PersistentToastManager {
    static let shared = PersistentToastManager()
    
    private var currentToastWindow: UIWindow?
    
    private init() {}
    
    func showToast(message: String, duration: TimeInterval) {
        hideToast()
        
        let toastWindow = UIWindow(frame: UIScreen.main.bounds)
        toastWindow.windowLevel = UIWindow.Level.alert + 1000
        toastWindow.backgroundColor = UIColor.clear
        toastWindow.isHidden = false
        
        let toastContainer = UIView()
        toastContainer.backgroundColor = UIColor.black.withAlphaComponent(0.8)
        toastContainer.layer.cornerRadius = 12
        toastContainer.translatesAutoresizingMaskIntoConstraints = false
        
        let label = UILabel()
        label.text = message
        label.textColor = .white
        label.font = UIFont.systemFont(ofSize: 16, weight: .medium)
        label.textAlignment = .center
        label.numberOfLines = 0
        label.translatesAutoresizingMaskIntoConstraints = false
        
        toastContainer.addSubview(label)
        toastWindow.addSubview(toastContainer)
        
        NSLayoutConstraint.activate([
            toastContainer.trailingAnchor.constraint(equalTo: toastWindow.trailingAnchor, constant: -20),
            toastContainer.bottomAnchor.constraint(equalTo: toastWindow.safeAreaLayoutGuide.bottomAnchor, constant: -10),
            toastContainer.leadingAnchor.constraint(greaterThanOrEqualTo: toastWindow.leadingAnchor, constant: 40),
            
            label.topAnchor.constraint(equalTo: toastContainer.topAnchor, constant: 16),
            label.bottomAnchor.constraint(equalTo: toastContainer.bottomAnchor, constant: -16),
            label.leadingAnchor.constraint(equalTo: toastContainer.leadingAnchor, constant: 20),
            label.trailingAnchor.constraint(equalTo: toastContainer.trailingAnchor, constant: -20)
        ])
        
        currentToastWindow = toastWindow
        
        toastContainer.alpha = 0
        toastContainer.transform = CGAffineTransform(translationX: 100, y: 0)
        
        UIView.animate(withDuration: 0.4, delay: 0, usingSpringWithDamping: 0.8, initialSpringVelocity: 0, options: [], animations: {
            toastContainer.alpha = 1
            toastContainer.transform = .identity
        }, completion: nil)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + duration) {
            self.hideToast()
        }
    }
    
    func hideToast() {
        guard let toastWindow = currentToastWindow,
              let toastContainer = toastWindow.subviews.first else { return }
        
        UIView.animate(withDuration: 0.3, animations: {
            toastContainer.alpha = 0
            toastContainer.transform = CGAffineTransform(translationX: 100, y: 0)
        }) { _ in
            toastWindow.isHidden = true
            self.currentToastWindow = nil
        }
    }
}
