package expo.modules.buttonsdk

import android.app.Activity
import android.app.AlertDialog
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
import android.app.Dialog
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ScrollView

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
        promotionData ?: return
        
        // Store browser reference for dismiss functionality
        currentBrowser = browser
        
        val promotions = promotionData["promotions"] as? List<*> ?: listOf<Any>()
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        val totalCount = promotions.size + if (featuredPromotion != null) 1 else 0
        
        if (totalCount > 0) {
            try {
                // Use the official Button SDK method: setCustomActionView
                val promotionButton = createHeaderPromotionButton(totalCount)
                browser.header.setCustomActionView(promotionButton)
                
                // Set the chrome client to handle clicks (as per documentation)
                browser.setBrowserChromeClient(object : com.usebutton.sdk.purchasepath.BrowserChromeClient() {
                    override fun onCustomActionClick(browser: BrowserInterface, view: View) {
                        android.util.Log.d("PromotionManager", "🎯 Custom action clicked!")
                        showPromotionsList()
                    }
                    
                    override fun onSubtitleClick(browser: BrowserInterface) {
                        // Required override but not used
                    }
                })
                
                android.util.Log.d("PromotionManager", "✅ Successfully added custom action view to header")
                
            } catch (e: Exception) {
                android.util.Log.w("PromotionManager", "❌ Could not add custom action view: ${e.message}")
                // Fallback: modify header title
                addPromotionToTitle(totalCount)
            }
        }
    }
    
    private fun hasMethod(obj: Any, methodName: String): Boolean {
        return try {
            obj.javaClass.getMethod(methodName, View::class.java)
            true
        } catch (e: NoSuchMethodException) {
            false
        }
    }
    
    private fun addPromotionToTitle(totalCount: Int) {
        try {
            val originalTitle = currentBrowser?.header?.title?.text
            val newTitle = if (originalTitle.isNullOrEmpty()) {
                "🏷️ $badgeLabel ($totalCount) - Tap here"
            } else {
                "$originalTitle | 🏷️ $badgeLabel ($totalCount)"
            }
            currentBrowser?.header?.title?.text = newTitle
            
            // Try to make the title clickeable by accessing the underlying view
            try {
                val titleView = currentBrowser?.header?.title
                // Try to get the actual TextView if possible
                val titleTextField = titleView?.javaClass?.getDeclaredField("textView")
                titleTextField?.isAccessible = true
                val actualTextView = titleTextField?.get(titleView) as? TextView
                
                actualTextView?.setOnClickListener {
                    android.util.Log.d("PromotionManager", "Title clicked - showing promotions!")
                    showPromotionsList()
                }
                
                android.util.Log.d("PromotionManager", "Made title clickeable")
            } catch (e: Exception) {
                android.util.Log.w("PromotionManager", "Could not make title clickeable: ${e.message}")
                
                // Alternative: try to find any clickeable parent view
                try {
                    val headerView = currentBrowser?.header
                    val headerClass = headerView?.javaClass
                    val viewField = headerClass?.getDeclaredField("view") 
                        ?: headerClass?.getDeclaredField("headerView")
                        ?: headerClass?.getDeclaredField("container")
                    
                    viewField?.isAccessible = true
                    val actualView = viewField?.get(headerView) as? View
                    
                    actualView?.setOnClickListener {
                        android.util.Log.d("PromotionManager", "Header clicked - showing promotions!")
                        showPromotionsList()
                    }
                    
                    android.util.Log.d("PromotionManager", "Made header clickeable")
                } catch (e2: Exception) {
                    android.util.Log.w("PromotionManager", "Could not make header clickeable: ${e2.message}")
                }
            }
            
            android.util.Log.d("PromotionManager", "Added promotions to header title")
        } catch (e: Exception) {
            android.util.Log.w("PromotionManager", "Could not modify header title: ${e.message}")
        }
    }
    
    private fun createHeaderPromotionButton(count: Int): View {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            
            // Create pill background
            val pillBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#F9F9FB")) // #f9f9fb
                cornerRadius = dpToPx(13).toFloat()
            }
            background = pillBackground
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(26)
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(8)
            }
            layoutParams = params

            // Don't set click listener - Button SDK will handle this via BrowserChromeClient
        }
        
        // Icon - create custom tag icon
        val iconView = createTagIconView()
        
        // Text - completely free, no restrictions
        val textView = TextView(context).apply {
            text = badgeLabel
            textSize = 10f
            setTextColor(Color.parseColor("#0B72AC")) // #0b72ac
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(3), 0, 0, 0)
            
            maxLines = 1
            setSingleLine(true)
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
            
            // No maxWidth restriction - let it be natural
        }
        
        button.addView(iconView)
        button.addView(textView)
        
        return button
    }
    
    private fun createTagIconView(): View {
        val iconView = ImageView(context).apply {
            setImageDrawable(createTagIconDrawable())
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            
            val params = LinearLayout.LayoutParams(
                dpToPx(10), // Reduced from 12dp to 10dp
                dpToPx(10)  // Reduced from 12dp to 10dp
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
                strokeWidth = dpToPx(1).toFloat() // Reduce stroke to match iOS
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
                
                // Draw the exact SVG path from HTML
                val path = Path().apply {
                    // M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z
                    moveTo(7f, 3f)
                    lineTo(12f, 3f)
                    cubicTo(12.512f, 3f, 13.024f, 3.195f, 13.414f, 3.586f)
                    lineTo(20.414f, 10.586f)
                    cubicTo(21.195f, 11.367f, 21.195f, 12.633f, 20.414f, 13.414f)
                    lineTo(13.414f, 20.414f)
                    cubicTo(12.633f, 21.195f, 11.367f, 21.195f, 10.586f, 20.414f)
                    lineTo(3.586f, 13.414f)
                    cubicTo(3.195f, 13.024f, 3f, 12.512f, 3f, 12f)
                    lineTo(3f, 7f)
                    cubicTo(3f, 4.791f, 4.791f, 3f, 7f, 3f)
                    close()
                }
                
                canvas.drawPath(path, paint)
                
                // Draw the small dot: M7 7h.01
                canvas.drawCircle(7f, 7f, 0.5f, paint)
                
                canvas.restore()
            }
            
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {}
            override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    private fun createPromotionOverlay(count: Int): View {
        val overlay = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isClickable = false // Don't block other interactions
        }
        
        val promotionButton = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 16, 12)
            isClickable = true
            isFocusable = true
            setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue background
            
            // Position in top-right corner
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(0, 80, 16, 0) // Top margin to avoid header overlap
            }
            layoutParams = params
        }
        
        // Add icon and text
        val iconView = TextView(context).apply {
            text = "🏷️"
            textSize = 16f
            gravity = Gravity.CENTER
        }
        
        val textView = TextView(context).apply {
            text = "Offers"
            textSize = 14f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        
        val countView = TextView(context).apply {
            text = count.toString()
            textSize = 11f // Slightly larger text
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            
            // Create circular background
            val circularBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.RED)
            }
            background = circularBackground
            
            val params = LinearLayout.LayoutParams(
                48, // Slightly larger circle
                48  // Slightly larger circle
            )
            layoutParams = params
            
            // Add slight bottom padding for better vertical centering
            setPadding(0, 0, 0, 2)
        }
        
        promotionButton.addView(iconView)
        promotionButton.addView(textView)
        promotionButton.addView(countView)
        
        // Add click listener
        promotionButton.setOnClickListener {
            android.util.Log.d("PromotionManager", "Promotion button clicked!")
            showPromotionsList()
        }
        
        overlay.addView(promotionButton)
        
        return overlay
    }
    
    private fun createPromotionButton(count: Int): View {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(12, 8, 12, 8)
            isClickable = true
            isFocusable = true
            
            // Add a subtle background
            setBackgroundResource(android.R.drawable.btn_default)
        }
        
        // Icon
        val iconView = TextView(context).apply {
            text = "🏷️"
            textSize = 14f
            gravity = Gravity.CENTER
        }
        
        // Count badge with perfect circular background
        val countView = TextView(context).apply {
            text = count.toString()
            textSize = 11f // Slightly larger text
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            
            // Create circular background
            val circularBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.RED)
            }
            background = circularBackground
            
            val params = LinearLayout.LayoutParams(
                48, // Slightly larger circle
                48  // Slightly larger circle
            ).apply {
                setMargins(4, 0, 0, 0)
            }
            layoutParams = params
            
            // Add slight bottom padding for better vertical centering
            setPadding(0, 0, 0, 2)
        }
        
        button.addView(iconView)
        button.addView(countView)
        
        // Add click listener
        button.setOnClickListener {
            showPromotionsList()
        }
        
        return button
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
            val title = promo["title"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val code = promo["code"] as? String
            val createdAt = promo["createdAt"] as? String
            
            var actionTitle = title
            
            // Add NEW badge if promotion is new
            if (createdAt != null && isPromotionNew(createdAt)) {
                actionTitle = "🆕 $actionTitle"
            }
            
            // Add feature indicator
            actionTitle = "⭐ $actionTitle"
            
            // Add reward text if available
            if (!rewardText.isNullOrEmpty()) {
                actionTitle = "$actionTitle\n$rewardText"
            }
            
            // Add coupon code if available
            if (!code.isNullOrEmpty()) {
                actionTitle = "$actionTitle\nCode: $code"
            }
            
            promotionItems.add(Pair(actionTitle, id))
        }
        
        // Add regular promotions
        val promotions = promotionData["promotions"] as? List<*> ?: listOf<Any>()
        for (promotion in promotions) {
            if (promotion is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val promotionMap = promotion as Map<String, Any>
                val title = promotionMap["title"] as? String ?: "Promotion"
                val id = promotionMap["id"] as? String ?: ""
                val code = promotionMap["code"] as? String
                val createdAt = promotionMap["createdAt"] as? String
                
                var actionTitle = title
                
                // Add NEW badge if promotion is new
                if (createdAt != null && isPromotionNew(createdAt)) {
                    actionTitle = "🆕 $actionTitle"
                }
                
                // Add reward text if available
                if (!rewardText.isNullOrEmpty()) {
                    actionTitle = "$actionTitle\n$rewardText"
                }
                
                // Add coupon code if available
                if (!code.isNullOrEmpty()) {
                    actionTitle = "$actionTitle\nCode: $code"
                }
                
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
            val bottomSheetView = createBottomSheetContent(promotions, container)
            
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
    
    private fun createBottomSheetContent(promotions: List<Pair<String, String>>, container: ViewGroup): View {
        val rootView = RelativeLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent background
        }
        
        // Bottom sheet container
        val sheetContainer = LinearLayout(context).apply {
            id = View.generateViewId()
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(24, 16, 24, 24)
            
            // Rounded corners
            val cornerRadius = 16f * context.resources.displayMetrics.density
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0f, 0f, 0f, 0f)
            }
            background = drawable
            
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                topMargin = (context.resources.displayMetrics.heightPixels * 0.3).toInt()
            }
        }
        
        // Handle bar
        val handleBar = View(context).apply {
            setBackgroundColor(Color.parseColor("#C0C0C0"))
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#C0C0C0"))
                cornerRadius = 2f * context.resources.displayMetrics.density
            }
            background = drawable
            layoutParams = LinearLayout.LayoutParams(
                (40 * context.resources.displayMetrics.density).toInt(),
                (4 * context.resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 16
            }
        }
        sheetContainer.addView(handleBar)
        
        // Title
        val titleView = TextView(context).apply {
            text = listTitle
            textSize = 20f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 24)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        sheetContainer.addView(titleView)
        
        // Scrollable promotions list
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
        }
        
        val promotionListContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        // Add promotion items as cards
        promotions.forEachIndexed { index, (title, id) ->
            val cardContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    // Card margins
                    setMargins(16, 8, 16, 8)
                }
            }
            
            val promotionCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    (16 * context.resources.displayMetrics.density).toInt(), // left
                    (16 * context.resources.displayMetrics.density).toInt(), // top
                    (16 * context.resources.displayMetrics.density).toInt(), // right
                    (16 * context.resources.displayMetrics.density).toInt()  // bottom
                )
                
                // Clean card styling with subtle border and rounded corners
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 12f * context.resources.displayMetrics.density
                    setStroke(1, Color.parseColor("#E5E7EB")) // Very subtle gray border
                }
                background = drawable
                
                isClickable = true
                isFocusable = true
                
                // Add subtle ripple effect for feedback
                val rippleDrawable = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#10000000")),
                    drawable,
                    null
                )
                background = rippleDrawable
                
                setOnClickListener {
                    android.util.Log.d("PromotionManager", "🎯 Promotion selected: $id")
                    
                    // Animate closing
                    animateBottomSheetClose(sheetContainer) {
                        isBottomSheetOpen = false
                        container.removeView(rootView)
                        
                        // Show global loader
                        val activity = context as? Activity
                        if (activity != null) {
                            GlobalLoaderManager.getInstance().showLoader(activity, "Loading promotion...")
                            android.util.Log.d("PromotionManager", "🔄 Global loader shown")
                        }
                        
                        // Call the promotion click callback
                        android.util.Log.d("PromotionManager", "📤 Invoking promotion callback for: $id")
                        onPromotionClickCallback?.invoke(id, currentBrowser)
                        android.util.Log.d("PromotionManager", "✅ Promotion callback completed")
                    }
                }
            }
            
            // Extract cashback info from title
            val cashbackRegex = "(\\d+% Cashback)".toRegex()
            val cashbackMatch = cashbackRegex.find(title)
            val cashbackText = cashbackMatch?.value ?: ""
            val cleanTitle = title.replace(cashbackRegex, "").trim()
            
            // Title text
            val titleView = TextView(context).apply {
                text = cleanTitle
                textSize = 16f
                setTextColor(Color.BLACK)
                gravity = Gravity.START
                maxLines = 2
                minHeight = (50 * context.resources.displayMetrics.density).toInt()
            }
            promotionCard.addView(titleView)
            
            // Add spacing between title and cashback
            if (cashbackText.isNotEmpty()) {
                val spacer = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        (12 * context.resources.displayMetrics.density).toInt()
                    )
                }
                promotionCard.addView(spacer)
                
                // Cashback text
                val cashbackView = TextView(context).apply {
                    text = cashbackText
                    textSize = 18f
                    setTextColor(Color.parseColor("#0B72AC"))
                    gravity = Gravity.START
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                promotionCard.addView(cashbackView)
            }
            cardContainer.addView(promotionCard)
            promotionListContainer.addView(cardContainer)
        }
        
        scrollView.addView(promotionListContainer)
        sheetContainer.addView(scrollView)
        
        // Close on background tap
        rootView.setOnClickListener {
            animateBottomSheetClose(sheetContainer) {
                isBottomSheetOpen = false
                container.removeView(rootView)
            }
        }
        
        rootView.addView(sheetContainer)
        return rootView
    }
    
    private fun animateBottomSheetClose(sheetContainer: LinearLayout, onComplete: () -> Unit) {
        // Get the parent container height for animation
        val parent = sheetContainer.parent as? ViewGroup
        val targetTranslation = parent?.height?.toFloat() ?: 1000f
        
        // Animate sliding down
        sheetContainer.animate()
            .translationY(targetTranslation)
            .setDuration(250)
            .setInterpolator(android.view.animation.AccelerateInterpolator())
            .withEndAction {
                onComplete()
            }
            .start()
    }
    
    private fun isPromotionNew(createdAt: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val createdDate = formatter.parse(createdAt) ?: return false
            
            val twoDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -2)
            }.time
            
            createdDate >= twoDaysAgo
        } catch (e: Exception) {
            false
        }
    }
    
}