import UIKit

class PromotionUIFactory {
    
    static func createTagIcon(fontScale: CGFloat = 1.0) -> UIView {
        let iconView = UIView()
        iconView.backgroundColor = UIColor.clear
        
        let iconLayer = CAShapeLayer()
        let path = UIBezierPath()
        
        let baseScale: CGFloat = 12.0 / 24.0
        let scale: CGFloat = baseScale * fontScale
        
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
        
        let dotPath = UIBezierPath(ovalIn: CGRect(x: 7 * scale - 0.5, y: 7 * scale - 0.5, width: 1, height: 1))
        path.append(dotPath)
        
        iconLayer.path = path.cgPath
        iconLayer.fillColor = UIColor.clear.cgColor
        iconLayer.strokeColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0).cgColor
        iconLayer.lineWidth = 2.0 * baseScale * fontScale
        iconLayer.lineCap = .round
        iconLayer.lineJoin = .round
        
        iconView.layer.addSublayer(iconLayer)
        
        return iconView
    }
    
    static func createPromotionBadge(count: Int, fontSize: CGFloat, label: String) -> UIView {
        let containerView = UIView()
        containerView.backgroundColor = UIColor(red: 0.976, green: 0.976, blue: 0.984, alpha: 1.0)
        containerView.layer.cornerRadius = 13
        containerView.translatesAutoresizingMaskIntoConstraints = false
        
        let scaleFactor = fontSize / 11.0
        let scaledIconSize = 12.0 * scaleFactor
        
        let iconView = createTagIcon(fontScale: scaleFactor)
        iconView.translatesAutoresizingMaskIntoConstraints = false
        
        let titleLabel = UILabel()
        titleLabel.text = label
        titleLabel.font = UIFont.systemFont(ofSize: fontSize)
        titleLabel.textColor = UIColor(red: 0.043, green: 0.447, blue: 0.675, alpha: 1.0)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        containerView.addSubview(iconView)
        containerView.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            iconView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 6),
            iconView.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            iconView.widthAnchor.constraint(equalToConstant: scaledIconSize),
            iconView.heightAnchor.constraint(equalToConstant: scaledIconSize),
            
            titleLabel.leadingAnchor.constraint(equalTo: iconView.trailingAnchor, constant: 3),
            titleLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -6),
            titleLabel.centerYAnchor.constraint(equalTo: containerView.centerYAnchor),
            
            containerView.heightAnchor.constraint(equalToConstant: 26),
            titleLabel.topAnchor.constraint(greaterThanOrEqualTo: containerView.topAnchor, constant: 4),
            titleLabel.bottomAnchor.constraint(lessThanOrEqualTo: containerView.bottomAnchor, constant: -4)
        ])
        
        return containerView
    }
}
