package expo.modules.buttonsdk

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.ImageView
import android.graphics.Color
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import com.usebutton.sdk.purchasepath.BrowserInterface
import com.usebutton.sdk.purchasepath.BrowserChromeClient
import expo.modules.buttonsdk.ui.PromotionBottomSheet
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PromotionManager(
    private val context: Context,
    private val promotionData: Map<String, Any>?,
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Unit)?,
    private val badgeLabel: String = "Offers",
    private val listTitle: String = "Available Promotions"
) {
    private var isBottomSheetOpen = false
    
    private var currentBrowser: BrowserInterface? = null
    
    companion object {
        // Shared promo code state across all instances
        private var sharedPendingPromoCode: String? = null
    }
    
    fun setOnPromotionClickCallback(callback: (String, BrowserInterface?) -> Unit) {
        this.onPromotionClickCallback = callback
    }
    
    fun setupPromotionsBadge(browser: BrowserInterface) {
        currentBrowser = browser
        
        val headerActions = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val promotionCount = getPromotionCount()
        if (promotionCount > 0) {
            val promotionButton = createHeaderPromotionButton(promotionCount)
            headerActions.addView(promotionButton)
        }
        
        try {
            browser.header.setCustomActionView(headerActions)
        } catch (e: Exception) {
            android.util.Log.e("PromotionManager", "Failed to set custom action view", e)
        }
    }
    
    private fun getPromotionCount(): Int {
        promotionData ?: return 0
        
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        val featuredCount = if (featuredPromotion != null) 1 else 0
        
        val promotions = promotionData["promotions"] as? List<*>
        val regularCount = promotions?.size ?: 0
        
        return featuredCount + regularCount
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
    
    private fun createHeaderPromotionButton(count: Int): View {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            
            // Create pill-shaped background with ripple effect
            val pillBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F9F9FB"))
                cornerRadius = dpToPx(13).toFloat()
            }
            
            // Add subtle ripple effect
            val rippleDrawable = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#20000000")), // Very subtle gray ripple
                pillBackground,
                null
            )
            background = rippleDrawable
            
            // Make it clickable
            isClickable = true
            isFocusable = true
            
            // Add click listener to show promotions
            setOnClickListener {
                android.util.Log.d("PromotionManager", "Header promotion button clicked")
                showPromotionsList()
            }
        }
        
        // Add tag icon
        val iconView = createTagIconView()
        button.addView(iconView)
        
        // Add label
        val labelView = TextView(context).apply {
            text = badgeLabel
            textSize = 11f
            setTextColor(Color.parseColor("#0B72AC"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(4)
                rightMargin = if (count > 0) dpToPx(4) else 0
            }
            layoutParams = params
        }
        button.addView(labelView)
        
        // Add count if there are promotions
        if (count > 0) {
            val countView = TextView(context).apply {
                text = count.toString()
                textSize = 11f
                setTextColor(Color.parseColor("#0B72AC"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                
                // Create circular background
                val circleBackground = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor("#E8F4F8"))
                }
                background = circleBackground
                
                // Set size and padding for circle
                val circleSize = dpToPx(16)
                setPadding(dpToPx(4), dpToPx(1), dpToPx(4), dpToPx(1))
                minWidth = circleSize
                minHeight = circleSize
                gravity = Gravity.CENTER
                
                val params = LinearLayout.LayoutParams(circleSize, circleSize)
                layoutParams = params
            }
            button.addView(countView)
        }
        
        return button
    }
    
    private fun createTagIconView(): View {
        val iconView = ImageView(context).apply {
            setImageDrawable(createTagIconDrawable())
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            
            val params = LinearLayout.LayoutParams(
                dpToPx(14),
                dpToPx(14)
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            layoutParams = params
        }
        return iconView
    }
    
    private fun createTagIconDrawable(): Drawable {
        return object : Drawable() {
            private val paint = Paint().apply {
                color = Color.parseColor("#0B72AC")
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(1).toFloat() // Thinner stroke
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            
            override fun draw(canvas: Canvas) {
                val bounds = getBounds()
                val scale = minOf(bounds.width(), bounds.height()) / 24f
                
                canvas.save()
                canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
                canvas.scale(scale, scale)
                
                // iOS SVG path: "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
                val path = Path().apply {
                    // Start at M7 3 (moveTo 7,3)
                    moveTo(7f, 3f)
                    // h5 (horizontal line to 12,3)
                    lineTo(12f, 3f)
                    // c.512 0 1.024.195 1.414.586 (curve representing rounded corner)
                    // Simplified as small curve
                    lineTo(12.586f, 3.586f)
                    // l7 7 (line 7 units right and 7 down)
                    lineTo(19.586f, 10.586f)
                    // a2 2 0 010 2.828 (arc representing rounded corner at tip)
                    lineTo(19.586f, 13.414f)
                    // l-7 7 (line 7 units left and 7 down)
                    lineTo(12.586f, 20.414f)
                    // a2 2 0 01-2.828 0 (arc)
                    lineTo(9.758f, 20.414f)
                    // l-7-7 (line 7 left, 7 up)
                    lineTo(2.758f, 13.414f)
                    // A1.994 1.994 0 013 12 (arc to point 3,12)
                    lineTo(3f, 12f)
                    // V7 (vertical line to 7)
                    lineTo(3f, 7f)
                    // a4 4 0 014-4 (arc back to start, completing rounded rectangle)
                    lineTo(7f, 3f)
                    close()
                }
                
                canvas.drawPath(path, paint)
                
                // Draw the small hole/dot inside the tag (like iOS)
                val holePaint = Paint().apply {
                    color = Color.parseColor("#0B72AC")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                // Draw small circle representing the tag hole at (7,7)
                canvas.drawCircle(7f, 7f, 1f, holePaint)
                
                canvas.restore()
            }
            
            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }
            
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }
            
            override fun getOpacity(): Int {
                return android.graphics.PixelFormat.TRANSLUCENT
            }
        }
    }
    
    private fun showPromotionsList() {
        promotionData ?: return
        currentBrowser ?: return
        
        val merchantName = promotionData["merchantName"] as? String ?: "Store"
        val rewardText = promotionData["rewardText"] as? String
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        
        val promotionItems = mutableListOf<Pair<String, String>>() // title, id
        
        // Add featured promotion if available
        featuredPromotion?.let { promo ->
            val title = promo["description"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val code = promo["couponCode"] as? String
            val startsAt = promo["startsAt"] as? String
            
            var actionTitle = title
            
            // Add NEW badge if promotion is new
            if (startsAt != null && isPromotionNew(startsAt)) {
                actionTitle = "⭐ $actionTitle"
            }
            
            // Add code if available
            if (!code.isNullOrEmpty()) {
                actionTitle = "$actionTitle"
            }
            
            promotionItems.add(Pair(actionTitle, id))
        }
        
        // Add regular promotions
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promo ->
            val title = promo["description"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val code = promo["couponCode"] as? String
            val startsAt = promo["startsAt"] as? String
            
            var actionTitle = title
            
            // Add NEW badge if promotion is new
            if (startsAt != null && isPromotionNew(startsAt)) {
                actionTitle = "NEW! $actionTitle"
            }
            
            // Add code if available
            if (!code.isNullOrEmpty()) {
                actionTitle = "$actionTitle"
            }
            
            if (id.isNotEmpty()) {
                promotionItems.add(Pair(actionTitle, id))
            }
        }
        
        // Use Bottom Sheet instead of overlay for better UX
        showPromotionsBottomSheet(promotionItems)
    }
    
    private fun showPromotionsBottomSheet(promotions: List<Pair<String, String>>) {
        // Prevent multiple instances
        if (isBottomSheetOpen) {
            android.util.Log.d("PromotionManager", "Bottom sheet already open, ignoring request")
            return
        }

        val browser = currentBrowser ?: return
        val container = browser.viewContainer
        
        if (container == null) {
            android.util.Log.e("PromotionManager", "Could not get view container from browser")
            return
        }
        
        try {
            isBottomSheetOpen = true
            
            val bottomSheet = PromotionBottomSheet(
                context = context,
                promotionData = promotionData,
                listTitle = listTitle,
                onPromotionClick = { promotionId: String ->
                    isBottomSheetOpen = false
                    
                    savePromoCodeForPromotion(promotionId)
                    
                    // Show appropriate loader based on whether promotion has promo code
                    val activity = context as? Activity
                    if (activity != null) {
                        if (sharedPendingPromoCode != null && sharedPendingPromoCode!!.isNotEmpty()) {
                            // For promotions with promo code, just show generic loader (copy logic handled in showPendingPromoCodeToast)
                            GlobalLoaderManager.getInstance().showLoader(activity, "Loading promotion...")
                            android.util.Log.d("PromotionManager", "🔄 Generic loader shown for promotion with code")
                        } else {
                            // For promotions without promo code, show loader and hide after 3 seconds
                            GlobalLoaderManager.getInstance().showLoader(activity, "Loading promotion...")
                            android.util.Log.d("PromotionManager", "🔄 Generic loader shown for promotion without code")
                            
                            // Hide generic loader after exactly 3 seconds too
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                GlobalLoaderManager.getInstance().hideLoader()
                                android.util.Log.d("PromotionManager", "🔄 Generic loader hidden after 3 seconds (forced)")
                            }, 3000) // 3 seconds
                        }
                    }
                    
                    // Call the promotion click callback
                    android.util.Log.d("PromotionManager", "📤 Invoking promotion callback for: $promotionId")
                    onPromotionClickCallback?.invoke(promotionId, currentBrowser)
                    android.util.Log.d("PromotionManager", "✅ Promotion callback completed")
                },
                onClose = {
                    isBottomSheetOpen = false
                }
            )
            
            val bottomSheetView = bottomSheet.createBottomSheetContent(promotions, container)
            
            // Add to browser container (same as ConfirmationDialog)
            container.addView(bottomSheetView)
            
            // Animate the bottom sheet sliding up from bottom
            val sheetChild = (bottomSheetView as RelativeLayout).getChildAt(0)
            
            // Set initial position off-screen at the bottom
            sheetChild?.translationY = container.height.toFloat()
            
            // Animate sliding up
            sheetChild?.animate()
                ?.translationY(0f)
                ?.setDuration(300)
                ?.setInterpolator(android.view.animation.DecelerateInterpolator())
                ?.start()
            
            android.util.Log.d("PromotionManager", "Bottom sheet overlay added successfully with animation")
            
        } catch (e: Exception) {
            android.util.Log.e("PromotionManager", "Error creating bottom sheet overlay", e)
            // Reset state if something went wrong
            isBottomSheetOpen = false
        }
    }
    
    private fun isPromotionNew(startsAt: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = formatter.parse(startsAt) ?: return false
            
            val twoDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -2)
            }.time
            
            startDate >= twoDaysAgo
        } catch (e: Exception) {
            false
        }
    }
    
    private fun savePromoCodeForPromotion(promotionId: String) {
        promotionData ?: run {
            android.util.Log.d("PromotionManager", "🔄 No promotion data available")
            return
        }
        
        sharedPendingPromoCode = null
        
        // Check featured promotion first
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        if (featuredPromotion != null) {
            val featuredId = featuredPromotion["id"] as? String
            if (featuredId == promotionId) {
                val promoCode = featuredPromotion["couponCode"] as? String ?: featuredPromotion["code"] as? String
                if (!promoCode.isNullOrEmpty()) {
                    sharedPendingPromoCode = promoCode
                    android.util.Log.d("PromotionManager", "🔄 Saved promo code for featured promotion: $promoCode")
                    return
                }
            }
        }
        
        // Check regular promotions
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promo ->
            val id = promo["id"] as? String
            if (id == promotionId) {
                val promoCode = promo["couponCode"] as? String ?: promo["code"] as? String
                if (!promoCode.isNullOrEmpty()) {
                    sharedPendingPromoCode = promoCode
                    android.util.Log.d("PromotionManager", "🔄 Saved promo code for promotion: $promoCode")
                    return
                }
            }
        }
        
        android.util.Log.d("PromotionManager", "🔄 No promo code found for promotion ID: $promotionId")
    }
    
    fun showPendingPromoCodeToast() {
        android.util.Log.d("PromotionManager", "🔄 showPendingPromoCodeToast called, sharedPendingPromoCode: ${sharedPendingPromoCode ?: "nil"}")
        val promoCode = sharedPendingPromoCode
        if (promoCode.isNullOrEmpty()) {
            android.util.Log.d("PromotionManager", "🔄 No pending promo code to show")
            return
        }
        
        // Copy to clipboard immediately
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = android.content.ClipData.newPlainText("Promo Code", promoCode)
        clipboardManager.setPrimaryClip(clipData)
        android.util.Log.d("PromotionManager", "🔄 Promo code copied: $promoCode")
        
        // Show toast immediately when Button SDK opens
        val activity = context as? Activity
        if (activity != null) {
            activity.runOnUiThread {
                showStyledToast(promoCode)
                android.util.Log.d("PromotionManager", "🔄 Toast shown immediately when Button SDK opened")
            }
        }
        
        // Hide loader after exactly 3 seconds, no matter what
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            GlobalLoaderManager.getInstance().hideLoader()
            android.util.Log.d("PromotionManager", "🔄 Loader hidden after 3 seconds (forced)")
        }, 3000) // 3 seconds
        
        // Clear the pending code after showing
        sharedPendingPromoCode = null
    }
    
    private fun copyToClipboardAndShowToast(promoCode: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = android.content.ClipData.newPlainText("Promo Code", promoCode)
        clipboardManager.setPrimaryClip(clipData)
        
        val activity = context as? Activity
        if (activity != null) {
            activity.runOnUiThread {
                showStyledToast(promoCode)
            }
        }
    }
    
    private fun showStyledToast(promoCode: String) {
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(
                (20 * context.resources.displayMetrics.density).toInt(),
                (16 * context.resources.displayMetrics.density).toInt(),
                (20 * context.resources.displayMetrics.density).toInt(),
                (16 * context.resources.displayMetrics.density).toInt()
            )
            
            val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(android.graphics.Color.parseColor("#CC000000"))
                cornerRadius = 12f * context.resources.displayMetrics.density
            }
            background = backgroundDrawable
        }
        
        val textView = android.widget.TextView(context).apply {
            text = "✓ $promoCode copied"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        
        layout.addView(textView)

        val toast = android.widget.Toast(context).apply {
            duration = android.widget.Toast.LENGTH_LONG
            view = layout
            
            setGravity(
                android.view.Gravity.BOTTOM or android.view.Gravity.RIGHT,
                (20 * context.resources.displayMetrics.density).toInt(),
                (60 * context.resources.displayMetrics.density).toInt()
            )
        }
        toast.show()
    }
    
}