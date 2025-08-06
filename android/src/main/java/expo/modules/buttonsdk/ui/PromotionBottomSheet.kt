package expo.modules.buttonsdk.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import expo.modules.buttonsdk.GlobalLoaderManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class PromotionBottomSheet(
    private val context: Context,
    private val promotionData: Map<String, Any>?,
    private val listTitle: String,
    private val onPromotionClick: (String) -> Unit,
    private val onClose: () -> Unit
) {
    
    fun createBottomSheetContent(promotions: List<Pair<String, String>>, container: ViewGroup): View {
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
            setPadding(
                (24 * context.resources.displayMetrics.density).toInt(),
                (16 * context.resources.displayMetrics.density).toInt(),
                (24 * context.resources.displayMetrics.density).toInt(),
                (24 * context.resources.displayMetrics.density).toInt()
            )
            
            // Clean card styling with subtle border and rounded corners
            val drawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 16f * context.resources.displayMetrics.density
                setStroke(1, Color.parseColor("#E5E7EB")) // Very subtle gray border
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
        promotions.forEachIndexed { _, (title, id) ->
            val cardContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 32, 16, 8)
                }
            }
            
            val promotionCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    (16 * context.resources.displayMetrics.density).toInt(),
                    (16 * context.resources.displayMetrics.density).toInt(),
                    (16 * context.resources.displayMetrics.density).toInt(),
                    (16 * context.resources.displayMetrics.density).toInt()
                )
                
                val drawable = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 12f * context.resources.displayMetrics.density
                    setStroke(4, Color.parseColor("#E5E7EB"))
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
                    android.util.Log.d("PromotionBottomSheet", "🎯 Promotion selected: $id")
                    dismissWithAnimation(rootView, container) {
                        onPromotionClick(id)
                    }
                }
            }
            
            // Extract cashback info from promotion data (like iOS)
            var cashbackText = ""
            
            // Get cashback from reward text or description, like iOS
            val promoData = getPromotionData(id)
            if (promoData != null) {
                val rewardText = promoData["rewardText"] as? String ?: promoData["description"] as? String ?: ""
                if (rewardText.isNotEmpty()) {
                    val cashbackRegex = "(\\d+% Cashback)".toRegex()
                    val cashbackMatch = cashbackRegex.find(rewardText)
                    cashbackText = cashbackMatch?.value ?: ""
                }
            }
            
            val cleanTitle = title
            
            // Create title container with potential time label
            val titleContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            // Calculate time label based on startsAt date
            var displayTitle = cleanTitle
            var timeLabel: String? = null
            
            // Get the startsAt date for this promotion
            val startsAt = getStartsAtForPromotion(id)
            if (startsAt != null) {
                val startDiff = calculateDaysDifference(startsAt)
                android.util.Log.d("PromotionBottomSheet", "🔍 Android Debug - Promotion: $id, startsAt: $startsAt, daysDiff: $startDiff")
                timeLabel = when {
                    startDiff < 3 -> {
                        android.util.Log.d("PromotionBottomSheet", "🔍 Android Debug - Setting label to NEW!")
                        "NEW!"
                    }
                    startDiff <= 7 -> { // Changed from < 7 to <= 7
                        android.util.Log.d("PromotionBottomSheet", "🔍 Android Debug - Setting label to THIS WEEK!")
                        "THIS WEEK!"
                    }
                    else -> null
                }
            }
            
            // Remove time labels from title if they exist (like iOS)
            if (timeLabel != null) {
                val timeLabels = listOf("THIS WEEK!", "NEW!", "TODAY!")
                for (label in timeLabels) {
                    displayTitle = displayTitle.replace(label, "").trim()
                }
                android.util.Log.d("PromotionBottomSheet", "🔍 Android Debug - Cleaned title: $displayTitle")
            }
            
            // Add time label if found
            timeLabel?.let { label ->
                android.util.Log.d("PromotionBottomSheet", "🔍 Android Debug - Creating label view for: $label")
                val labelView = TextView(context).apply {
                    text = label
                    textSize = 12f
                    setTextColor(Color.parseColor("#047857")) // emerald-700
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setPadding(
                        (8 * context.resources.displayMetrics.density).toInt(),
                        (2 * context.resources.displayMetrics.density).toInt(),
                        (8 * context.resources.displayMetrics.density).toInt(),
                        (2 * context.resources.displayMetrics.density).toInt()
                    )
                    
                    // Green background
                    val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#ECFDF5")) // emerald-50
                        cornerRadius = 4f * context.resources.displayMetrics.density
                    }
                    background = bgDrawable
                    android.util.Log.d("PromotionBottomSheet", "🔍 Android Debug - Green background applied to label")
                    
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = (8 * context.resources.displayMetrics.density).toInt()
                    }
                }
                titleContainer.addView(labelView)
            }
            
            // Title text
            val titleView = TextView(context).apply {
                text = displayTitle
                textSize = 16f
                setTextColor(Color.parseColor("#374151")) // gray-900
                gravity = Gravity.START
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            titleContainer.addView(titleView)
            promotionCard.addView(titleContainer)
            
            // Add spacing between title and cashback
            val spacer = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (12 * context.resources.displayMetrics.density).toInt()
                )
            }
            promotionCard.addView(spacer)
            
            // Bottom section with cashback, promo code, and time
            val bottomContainer = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            
            // Cashback text
            if (cashbackText.isNotEmpty()) {
                val cashbackView = TextView(context).apply {
                    text = cashbackText
                    textSize = 14f // Larger than ends in (12f)
                    setTextColor(Color.parseColor("#0B72AC"))
                    gravity = Gravity.START
                    typeface = android.graphics.Typeface.DEFAULT // Remove bold
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = (8 * context.resources.displayMetrics.density).toInt()
                    }
                }
                bottomContainer.addView(cashbackView)
            }
            
            // Extract promo code from promotion data
            val promoCode = getPromoCodeForPromotion(id)
            if (promoCode != null && promoCode.isNotEmpty()) {
                // Add bullet separator only if there's cashback text
                if (cashbackText.isNotEmpty()) {
                    val bulletView = TextView(context).apply {
                        text = "•"
                        textSize = 14f
                        setTextColor(Color.parseColor("#6B7280")) // gray-500
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            rightMargin = (8 * context.resources.displayMetrics.density).toInt()
                        }
                    }
                    bottomContainer.addView(bulletView)
                }
                
                // Promo code button with tag icon
                val promoCodeButton = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(
                        (12 * context.resources.displayMetrics.density).toInt(), // Increased padding
                        (6 * context.resources.displayMetrics.density).toInt(),  // Increased padding
                        (12 * context.resources.displayMetrics.density).toInt(), // Increased padding
                        (6 * context.resources.displayMetrics.density).toInt()   // Increased padding
                    )
                    
                    // Blue background with increased corner radius
                    val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#EFF6FF")) // blue-50
                        cornerRadius = 8f * context.resources.displayMetrics.density // Increased from 4f
                    }
                    
                    // Add ripple effect for clickable feedback
                    val rippleDrawable = android.graphics.drawable.RippleDrawable(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#10000000")),
                        bgDrawable,
                        null
                    )
                    background = rippleDrawable
                    
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = (8 * context.resources.displayMetrics.density).toInt()
                    }
                    
                    isClickable = true
                    isFocusable = true
                    
                    // Handle click with animation - consume the event to prevent parent handling
                    setOnTouchListener { view, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN -> {
                                // Press down animation
                                view.animate()
                                    .scaleX(0.95f)
                                    .scaleY(0.95f)
                                    .alpha(0.8f)
                                    .setDuration(100)
                                    .start()
                                true // Consume the event
                            }
                            android.view.MotionEvent.ACTION_UP -> {
                                // Press up animation
                                view.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .alpha(1.0f)
                                    .setDuration(100)
                                    .start()
                                
                                // Trigger the same action as tapping the deal container
                                android.util.Log.d("PromotionBottomSheet", "🎯 Promo code clicked for promotion: $id")
                                dismissWithAnimation(rootView, container) {
                                    onPromotionClick(id)
                                }
                                true // Consume the event
                            }
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                // Press up animation for cancel
                                view.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .alpha(1.0f)
                                    .setDuration(100)
                                    .start()
                                true // Consume the event
                            }
                            else -> false
                        }
                    }
                }
                
                // Add tag icon (same as header pill)
                val tagIcon = createHeaderTagIcon()
                promoCodeButton.addView(tagIcon)
                
                // Add promo code text
                val promoCodeView = TextView(context).apply {
                    text = promoCode
                    textSize = 10f // Smaller font size, no bold
                    setTextColor(Color.parseColor("#0B72AC"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        leftMargin = (4 * context.resources.displayMetrics.density).toInt()
                    }
                }
                promoCodeButton.addView(promoCodeView)
                bottomContainer.addView(promoCodeButton)
            }
            
            // Extract time remaining from promotion data
            val timeRemaining = getTimeRemainingForPromotion(id)
            if (timeRemaining != null) {
                // Add bullet separator before time only if there's cashback or promo code
                if (cashbackText.isNotEmpty() || (promoCode != null && promoCode.isNotEmpty())) {
                    val bulletView2 = TextView(context).apply {
                        text = "•"
                        textSize = 14f
                        setTextColor(Color.parseColor("#6B7280")) // gray-500
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            rightMargin = (8 * context.resources.displayMetrics.density).toInt()
                        }
                    }
                    bottomContainer.addView(bulletView2)
                }
                
                // Time remaining
                val timeView = TextView(context).apply {
                    text = timeRemaining
                    textSize = 12f
                    setTextColor(Color.parseColor("#6B7280")) // gray-500
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                bottomContainer.addView(timeView)
            }
            
            promotionCard.addView(bottomContainer)
            cardContainer.addView(promotionCard)
            promotionListContainer.addView(cardContainer)
        }
        
        scrollView.addView(promotionListContainer)
        sheetContainer.addView(scrollView)
        
        // Close on background tap
        rootView.setOnClickListener {
            dismissWithAnimation(rootView, container) {
                onClose()
            }
        }
        
        rootView.addView(sheetContainer)
        return rootView
    }
    
    private fun dismissWithAnimation(rootView: View, container: ViewGroup, onComplete: () -> Unit) {
        val sheetChild = (rootView as RelativeLayout).getChildAt(0)
        
        // Animate sliding down
        sheetChild?.animate()
            ?.translationY(container.height.toFloat())
            ?.setDuration(250)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction {
                // Remove view after animation
                container.removeView(rootView)
                onComplete()
            }
            ?.start()
    }
    
    private fun createHeaderTagIcon(): View {
        return android.widget.ImageView(context).apply {
            setImageDrawable(createTagIconDrawable())
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(
                (12 * context.resources.displayMetrics.density).toInt(),
                (12 * context.resources.displayMetrics.density).toInt()
            )
        }
    }
    
    private fun createTagIconDrawable(): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = android.graphics.Paint().apply {
                color = Color.parseColor("#0B72AC")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = (1 * context.resources.displayMetrics.density) // Thinner stroke
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
                isAntiAlias = true
            }
            
            override fun draw(canvas: android.graphics.Canvas) {
                val bounds = getBounds()
                val scale = minOf(bounds.width(), bounds.height()) / 24f
                
                canvas.save()
                canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
                canvas.scale(scale, scale)
                
                // iOS SVG path: "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
                val path = android.graphics.Path().apply {
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
                val holePaint = android.graphics.Paint().apply {
                    color = Color.parseColor("#0B72AC")
                    style = android.graphics.Paint.Style.FILL
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
    
    private fun getPromotionData(promotionId: String): Map<String, Any>? {
        promotionData ?: return null
        
        // Check featured promotion
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        if (featuredPromotion != null && featuredPromotion["id"] == promotionId) {
            return featuredPromotion
        }
        
        // Check regular promotions
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promotion ->
            if (promotion["id"] == promotionId) {
                return promotion
            }
        }
        
        return null
    }
    
    private fun getPromoCodeForPromotion(promotionId: String): String? {
        val promotion = getPromotionData(promotionId) ?: return null
        return promotion["couponCode"] as? String ?: promotion["code"] as? String
    }
    
    private fun getTimeRemainingForPromotion(promotionId: String): String? {
        promotionData ?: return null
        
        // Check featured promotion
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        if (featuredPromotion != null && featuredPromotion["id"] == promotionId) {
            return calculateTimeRemaining(featuredPromotion["endsAt"] as? String)
        }
        
        // Check regular promotions
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promotion ->
            if (promotion["id"] == promotionId) {
                return calculateTimeRemaining(promotion["endsAt"] as? String)
            }
        }
        
        return null
    }
    
    private fun calculateTimeRemaining(endsAt: String?): String? {
        if (endsAt == null) return null
        
        return try {
            // Handle the format from your data: "2025-07-21T03:59:00Z"
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val expirationDate = formatter.parse(endsAt) ?: return null
            
            val now = Calendar.getInstance().time
            val timeDiff = expirationDate.time - now.time
            
            // If already expired, don't show time remaining
            if (timeDiff <= 0) return null
            
            val days = TimeUnit.MILLISECONDS.toDays(timeDiff)
            
            // Hide if ends in more than 1 week (7 days) - only show if ≤ 7 days
            if (days > 7) return null
            
            val hours = TimeUnit.MILLISECONDS.toHours(timeDiff) % 24
            
            when {
                days > 1 -> "ends in ${days}d"
                days == 1L -> "ends tomorrow"
                hours > 1 -> "ends in ${hours}h"
                hours == 1L -> "ends in 1h"
                else -> "ends today"
            }
        } catch (e: Exception) {
            android.util.Log.e("PromotionBottomSheet", "Error calculating time remaining", e)
            null
        }
    }
    
    private fun getStartsAtForPromotion(promotionId: String): String? {
        promotionData ?: return null
        
        // Check featured promotion
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        if (featuredPromotion != null && featuredPromotion["id"] == promotionId) {
            return featuredPromotion["startsAt"] as? String
        }
        
        // Check regular promotions
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promotion ->
            if (promotion["id"] == promotionId) {
                return promotion["startsAt"] as? String
            }
        }
        
        return null
    }
    
    private fun calculateDaysDifference(dateStr: String): Int {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = formatter.parse(dateStr) ?: return Int.MAX_VALUE
            
            val now = Calendar.getInstance()
            val startCalendar = Calendar.getInstance().apply {
                time = startDate
            }
            
            // Calculate difference in calendar days
            val nowDayOfYear = now.get(Calendar.DAY_OF_YEAR)
            val nowYear = now.get(Calendar.YEAR)
            val startDayOfYear = startCalendar.get(Calendar.DAY_OF_YEAR)
            val startYear = startCalendar.get(Calendar.YEAR)
            
            val daysDiff = if (nowYear == startYear) {
                Math.abs(nowDayOfYear - startDayOfYear)
            } else {
                // For different years, calculate the actual day difference
                val diffInMillis = Math.abs(now.timeInMillis - startCalendar.timeInMillis)
                (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
            }
            
            return daysDiff
        } catch (e: Exception) {
            android.util.Log.e("PromotionBottomSheet", "Error calculating days difference", e)
            return Int.MAX_VALUE
        }
    }
    
    private fun handlePromoCodeCopy(promotionId: String) {
        // Get promo code for this promotion
        val promoCode = getPromoCodeForPromotion(promotionId)
        if (promoCode != null) {
            // Copy to clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Promo Code", promoCode)
            clipboard.setPrimaryClip(clip)
            
            // Show simple toast message
            showCopiedToast(promoCode)
        }
    }
    
    private fun showCopiedToast(promoCode: String) {
        // Create a custom layout programmatically to ensure styling is applied reliably
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * context.resources.displayMetrics.density).toInt(),
                (12 * context.resources.displayMetrics.density).toInt(),
                (20 * context.resources.displayMetrics.density).toInt(),
                (12 * context.resources.displayMetrics.density).toInt()
            )

            // Create the background drawable programmatically
            val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000")) // Black with 80% opacity
                cornerRadius = 12f * context.resources.displayMetrics.density // 12dp corner radius
            }
            background = backgroundDrawable
        }

        // Create the text view for the message
        val textView = TextView(context).apply {
            text = "✓ $promoCode copied"
            setTextColor(Color.WHITE)
            textSize = 16f
        }

        layout.addView(textView)

        // Create and show the toast with the custom layout
        val toast = android.widget.Toast(context).apply {
            duration = android.widget.Toast.LENGTH_LONG
            // Position the toast at the bottom-right, with offsets to mimic the iOS version
            val xOffset = (20 * context.resources.displayMetrics.density).toInt()
            val yOffset = (40 * context.resources.displayMetrics.density).toInt() // Extra offset from bottom
            setGravity(android.view.Gravity.BOTTOM or android.view.Gravity.END, xOffset, yOffset)
            view = layout
        }
        toast.show()
    }
}