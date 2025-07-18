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
                    // Card margins
                    setMargins(16, 8, 16, 8)
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
                    android.util.Log.d("PromotionBottomSheet", "🎯 Promotion selected: $id")
                    container.removeView(rootView)
                    onPromotionClick(id)
                }
            }
            
            // Extract cashback info from title
            val cashbackRegex = "(\\d+% Cashback)".toRegex()
            val cashbackMatch = cashbackRegex.find(title)
            val cashbackText = cashbackMatch?.value ?: ""
            val cleanTitle = title.replace(cashbackRegex, "").trim()
            
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
                timeLabel = when {
                    startDiff < 3 -> "NEW!"
                    startDiff < 7 -> "THIS WEEK!"
                    else -> null
                }
            }
            
            // Add time label if found
            timeLabel?.let { label ->
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
            if (promoCode != null) {
                // Add bullet separator
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
                
                // Promo code with tag icon
                val promoCodeContainer = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(
                        (8 * context.resources.displayMetrics.density).toInt(),
                        (4 * context.resources.displayMetrics.density).toInt(),
                        (8 * context.resources.displayMetrics.density).toInt(),
                        (4 * context.resources.displayMetrics.density).toInt()
                    )
                    
                    // Blue background
                    val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#EFF6FF")) // blue-50
                        cornerRadius = 4f * context.resources.displayMetrics.density
                    }
                    background = bgDrawable
                    
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = (8 * context.resources.displayMetrics.density).toInt()
                    }
                }
                
                // Add tag icon (same as header pill)
                val tagIcon = createHeaderTagIcon()
                promoCodeContainer.addView(tagIcon)
                
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
                promoCodeContainer.addView(promoCodeView)
                bottomContainer.addView(promoCodeContainer)
            }
            
            // Extract time remaining from promotion data
            val timeRemaining = getTimeRemainingForPromotion(id)
            if (timeRemaining != null) {
                // Add bullet separator before time
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
            container.removeView(rootView)
            onClose()
        }
        
        rootView.addView(sheetContainer)
        return rootView
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
                strokeWidth = (1 * context.resources.displayMetrics.density)
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
                
                // Create tag path (same as header icon)
                val path = android.graphics.Path().apply {
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
                val circlePaint = android.graphics.Paint().apply {
                    color = Color.parseColor("#0B72AC")
                    style = android.graphics.Paint.Style.FILL
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
    
    private fun getPromoCodeForPromotion(promotionId: String): String? {
        promotionData ?: return null
        
        // Check featured promotion
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        if (featuredPromotion != null && featuredPromotion["id"] == promotionId) {
            return featuredPromotion["couponCode"] as? String
        }
        
        // Check regular promotions
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promotion ->
            if (promotion["id"] == promotionId) {
                return promotion["couponCode"] as? String
            }
        }
        
        return null
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
}