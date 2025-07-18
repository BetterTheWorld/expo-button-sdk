package expo.modules.buttonsdk

import android.app.Activity
import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import expo.modules.buttonsdk.ui.PromotionBottomSheet

class PromotionManager(
    private val context: Context,
    private val promotionData: Map<String, Any>?,
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Unit)?,
    private val badgeLabel: String = "Offers",
    private val listTitle: String = "Available Promotions"
) {
    private var isBottomSheetOpen = false
    
    private var currentBrowser: BrowserInterface? = null
    
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
                strokeWidth = dpToPx(1).toFloat()
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
                
                // Create tag path (simplified version of the SVG)
                val path = Path().apply {
                    // Tag shape: rectangle with triangular cut on left
                    moveTo(8f, 2f)
                    lineTo(20f, 2f)
                    lineTo(20f, 22f)
                    lineTo(8f, 22f)
                    lineTo(2f, 12f)
                    close()
                }
                
                canvas.drawPath(path, paint)
                
                // Draw small circle for tag hole
                val circlePaint = Paint().apply {
                    color = Color.parseColor("#0B72AC")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(14f, 8f, 1.5f, circlePaint)
                
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
                onPromotionClick = { promotionId ->
                    isBottomSheetOpen = false
                    
                    // Show global loader
                    val activity = context as? Activity
                    if (activity != null) {
                        GlobalLoaderManager.getInstance().showLoader(activity, "Loading promotion...")
                        android.util.Log.d("PromotionManager", "🔄 Global loader shown")
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
    
}