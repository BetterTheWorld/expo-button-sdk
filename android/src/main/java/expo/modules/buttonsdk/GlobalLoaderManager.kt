package expo.modules.buttonsdk

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference

class GlobalLoaderManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: GlobalLoaderManager? = null
        
        fun getInstance(): GlobalLoaderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlobalLoaderManager().also { INSTANCE = it }
            }
        }
    }
    
    @Volatile
    private var currentLoaderView: View? = null
    @Volatile
    private var currentActivityRef: WeakReference<Activity>? = null
    
    fun showLoader(activity: Activity, message: String = "Loading promotion...", loaderColor: Int? = null) {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                showLoader(activity, message, loaderColor)
            }
            return
        }
        
        try {
            hideLoader() // Remove any existing loader first
            
            currentActivityRef = WeakReference(activity)
            
            // Get the root view of the activity (this will be above everything including WebView)
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            
            val loaderOverlay = createLoaderView(activity, message, loaderColor)
            currentLoaderView = loaderOverlay
            
            rootView?.addView(loaderOverlay)
            
            android.util.Log.d("GlobalLoaderManager", "✅ Loader shown over root view")
            
        } catch (e: Exception) {
            android.util.Log.e("GlobalLoaderManager", "❌ Error showing loader", e)
        }
    }
    
    fun showCopyLoader(activity: Activity, promoCode: String) {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                showCopyLoader(activity, promoCode)
            }
            return
        }
        
        try {
            hideLoader() // Remove any existing loader first
            
            currentActivityRef = WeakReference(activity)
            
            // Get the root view of the activity (this will be above everything including WebView)
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            
            val loaderOverlay = createCopyLoaderView(activity, promoCode)
            currentLoaderView = loaderOverlay
            
            rootView?.addView(loaderOverlay)
            
            android.util.Log.d("GlobalLoaderManager", "✅ Copy loader shown with promo code: $promoCode")
            
        } catch (e: Exception) {
            android.util.Log.e("GlobalLoaderManager", "❌ Error showing copy loader", e)
        }
    }
    
    fun hideLoader() {
        // Ensure we're on the main thread
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                hideLoader()
            }
            return
        }
        
        try {
            currentLoaderView?.let { loader ->
                currentActivityRef?.get()?.let { activity ->
                    val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                    rootView?.removeView(loader)
                    android.util.Log.d("GlobalLoaderManager", "✅ Loader hidden")
                }
            }
            currentLoaderView = null
            currentActivityRef = null
        } catch (e: Exception) {
            android.util.Log.e("GlobalLoaderManager", "❌ Error hiding loader", e)
        }
    }
    
    private fun createLoaderView(activity: Activity, message: String, loaderColor: Int?): View {
        // Semi-transparent background overlay that covers everything
        val overlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
            isClickable = true // Block touches to underlying views
            isFocusable = true
        }
        
        val loaderContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 60, 80, 60)
            
            // Create custom rounded background
            val roundedBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 24f // Rounded corners
                // Add subtle shadow effect
                setStroke(1, Color.parseColor("#E0E0E0"))
            }
            background = roundedBackground
            
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(48, 48, 48, 48)
            }
            layoutParams = params
            
            elevation = 12f // Higher elevation for better shadow
        }
        
        // Progress bar with better styling
        val progressBar = ProgressBar(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 40
            }
            scaleX = 1.2f
            scaleY = 1.2f
            loaderColor?.let {
                indeterminateTintList = android.content.res.ColorStateList.valueOf(it)
            }
        }
        
        val loadingText = TextView(activity).apply {
            text = message
            textSize = 17f
            setTextColor(Color.parseColor("#2C2C2C"))
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Better font weight
            setTypeface(typeface, android.graphics.Typeface.NORMAL)
        }
        
        loaderContainer.addView(progressBar)
        loaderContainer.addView(loadingText)
        overlay.addView(loaderContainer)
        
        return overlay
    }
    
    private fun createCopyLoaderView(activity: Activity, promoCode: String): View {
        // Semi-transparent background overlay that covers everything
        val overlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
            isClickable = true // Block touches to underlying views
            isFocusable = true
        }
        
        val loaderContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 60, 80, 60)
            
            // Create custom rounded background
            val roundedBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 24f // Rounded corners
                // Add subtle shadow effect
                setStroke(1, Color.parseColor("#E0E0E0"))
            }
            background = roundedBackground
            
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(48, 48, 48, 48)
            }
            layoutParams = params
            
            elevation = 12f // Higher elevation for better shadow
        }
        
        // Blue progress bar
        val progressBar = ProgressBar(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 32
            }
            scaleX = 1.2f
            scaleY = 1.2f
            indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0B72AC"))
        }
        
        // "Copying to clipboard..." text
        val copyingText = TextView(activity).apply {
            text = "Copying to clipboard..."
            textSize = 16f
            setTextColor(Color.parseColor("#6B7280"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32
            }
        }
        
        // Promo code pill
        val promoCodePill = createPromoCodePill(activity, promoCode)
        promoCodePill.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = 32
        }
        
        // "Loading promotion..." text at bottom
        val loadingText = TextView(activity).apply {
            text = "Loading promotion..."
            textSize = 14f
            setTextColor(Color.parseColor("#9CA3AF"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        loaderContainer.addView(progressBar)
        loaderContainer.addView(copyingText)
        loaderContainer.addView(promoCodePill)
        loaderContainer.addView(loadingText)
        overlay.addView(loaderContainer)
        
        return overlay
    }
    
    private fun createPromoCodePill(activity: Activity, promoCode: String): View {
        val pillContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                (12 * activity.resources.displayMetrics.density).toInt(),
                (6 * activity.resources.displayMetrics.density).toInt(),
                (12 * activity.resources.displayMetrics.density).toInt(),
                (6 * activity.resources.displayMetrics.density).toInt()
            )
            
            val pillBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#EFF6FF")) // blue-50
                cornerRadius = 8f * activity.resources.displayMetrics.density
            }
            background = pillBackground
        }
        
        // Add tag icon
        val tagIcon = createTagIcon(activity)
        tagIcon.layoutParams = LinearLayout.LayoutParams(
            (12 * activity.resources.displayMetrics.density).toInt(),
            (12 * activity.resources.displayMetrics.density).toInt()
        ).apply {
            rightMargin = (4 * activity.resources.displayMetrics.density).toInt()
        }
        
        // Promo code text
        val promoLabel = TextView(activity).apply {
            text = promoCode
            textSize = 10f
            setTextColor(Color.parseColor("#0B72AC"))
        }
        
        pillContainer.addView(tagIcon)
        pillContainer.addView(promoLabel)
        
        return pillContainer
    }
    
    private fun createTagIcon(activity: Activity): View {
        return android.widget.ImageView(activity).apply {
            setImageDrawable(createTagIconDrawable(activity))
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        }
    }
    
    private fun createTagIconDrawable(activity: Activity): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = android.graphics.Paint().apply {
                color = Color.parseColor("#0B72AC")
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = (1 * activity.resources.displayMetrics.density)
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
                
                val path = android.graphics.Path().apply {
                    moveTo(7f, 3f)
                    lineTo(12f, 3f)
                    lineTo(12.586f, 3.586f)
                    lineTo(19.586f, 10.586f)
                    lineTo(19.586f, 13.414f)
                    lineTo(12.586f, 20.414f)
                    lineTo(9.758f, 20.414f)
                    lineTo(2.758f, 13.414f)
                    lineTo(3f, 12f)
                    lineTo(3f, 7f)
                    lineTo(7f, 3f)
                    close()
                }
                
                canvas.drawPath(path, paint)
                
                // Draw the small hole/dot inside the tag
                val holePaint = android.graphics.Paint().apply {
                    color = Color.parseColor("#0B72AC")
                    style = android.graphics.Paint.Style.FILL
                    isAntiAlias = true
                }
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
}