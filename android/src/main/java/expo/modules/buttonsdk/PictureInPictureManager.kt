package expo.modules.buttonsdk

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.usebutton.sdk.purchasepath.BrowserInterface
import expo.modules.buttonsdk.events.BrowserScrollEventBus
import expo.modules.buttonsdk.events.ScrollVisibilityObserver
import expo.modules.buttonsdk.events.ScrollVisibilityEvent

class PictureInPictureManager(
    private val context: Context,
    private val options: Map<String, Any>
) : ScrollVisibilityObserver {
    
    private var isMinimized = false
    private var isAnimating = false
    private var originalBrowser: BrowserInterface? = null
    private var containerView: LinearLayout? = null
    private var isButtonHidden = false
    private var isClosingForNewContent = false
    
    var delegate: PictureInPictureManagerDelegate? = null
    
    interface PictureInPictureManagerDelegate {
        fun didMinimize()
        fun didRestore()
    }
    
    fun isPipActive(): Boolean {
        return isMinimized
    }
    
    fun closePipAndProceed(onComplete: () -> Unit) {
        Log.d("PictureInPictureManager", "Closing PiP and proceeding with callback")
        
        if (!isMinimized) {
            Log.d("PictureInPictureManager", "PiP not active, proceeding immediately")
            onComplete()
            return
        }
        
        // Set flag to bypass exit confirmation
        isClosingForNewContent = true
        
        // Set callback for when PiP is closed
        delegate = object : PictureInPictureManagerDelegate {
            override fun didMinimize() {
                // Not needed
            }
            
            override fun didRestore() {
                Log.d("PictureInPictureManager", "PiP closed, executing callback")
                isClosingForNewContent = false
                onComplete()
                delegate = null // Clean up
            }
        }
        
        // Close PiP
        exitPipForNewContent()
    }
    
    fun isClosingForNewContent(): Boolean {
        return isClosingForNewContent
    }
    
    private fun exitPipForNewContent() {
        if (!isMinimized) return
        
        Log.d("PictureInPictureManager", "Exiting PiP mode due to new content")
        
        // Find the Button SDK activity and exit PiP mode
        val browser = originalBrowser ?: return
        val browserView = browser.viewContainer ?: return
        
        var buttonSdkActivity: Activity? = null
        var currentView = browserView.parent
        
        while (currentView != null) {
            if (currentView is android.view.ViewGroup) {
                val context = currentView.context
                if (context is Activity && context.javaClass.name.contains("WebViewActivity")) {
                    buttonSdkActivity = context
                    break
                }
            }
            currentView = currentView.parent
        }
        
        val targetActivity = buttonSdkActivity ?: return
        
        try {
            // Exit PiP mode programmatically
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (targetActivity.isInPictureInPictureMode) {
                    // Cannot directly exit PiP, but we can finish the activity and restart
                    Log.d("PictureInPictureManager", "Activity is in PiP mode - bringing it back to front")
                    
                    val intent = android.content.Intent(targetActivity, targetActivity.javaClass)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or 
                                  android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                  android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    targetActivity.startActivity(intent)
                }
            }
            
            isMinimized = false
            delegate?.didRestore()
            
        } catch (e: Exception) {
            Log.e("PictureInPictureManager", "Error exiting PiP for new URL", e)
        }
    }
    
    fun addMinimizeButton(browser: BrowserInterface) {
        originalBrowser = browser
        // PromotionManager will handle adding the minimize button now
        // We just need to store the browser reference for PiP functionality
        Handler(Looper.getMainLooper()).postDelayed({
            BrowserScrollEventBus.getInstance().addVisibilityObserver(this)
            BrowserScrollEventBus.getInstance().startMonitoring(browser)
        }, 1000)
    }
    
    fun createMinimizeButton(context: Context, browser: BrowserInterface? = null): View {
        Log.d("PictureInPictureManager", "createMinimizeButton called")
        
        // Create the same type of button container as the promotions button
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            
            // Increase padding for better touch area
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            
            // Create background similar to promotion button
            val pillBackground = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#44666666")) // Dark semi-transparent background
                cornerRadius = dpToPx(10).toFloat()
            }
            
            val rippleDrawable = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.parseColor("#20FFFFFF")),
                pillBackground,
                null
            )
            background = rippleDrawable
            
            isClickable = true
            isFocusable = true
            
            setOnClickListener { 
                Log.d("PictureInPictureManager", "Minimize button clicked!")
                Log.d("PictureInPictureManager", "originalBrowser is: ${if (originalBrowser != null) "not null" else "null"}")
                Log.d("PictureInPictureManager", "passed browser is: ${if (browser != null) "not null" else "null"}")
                
                // Use the passed browser or fallback to originalBrowser
                val browserToUse = browser ?: originalBrowser
                if (browserToUse != null) {
                    // Store it for future use
                    originalBrowser = browserToUse
                }
                
                // Call minimize immediately, remove animation that might interfere
                minimizeButtonTapped() 
            }
        }
        
        // Create the chevron icon
        val chevronIcon = ImageView(context).apply {
            val iconSize = dpToPx(24) // Larger icon for better visibility
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            
            // Create chevron down drawable
            val chevronDrawable = createChevronDownDrawable(Color.WHITE)
            setImageDrawable(chevronDrawable)
        }
        
        button.addView(chevronIcon)
        
        Log.d("PictureInPictureManager", "Minimize button container created successfully")
        return button
    }
    
    
    override fun onScrollVisibilityChanged(event: ScrollVisibilityEvent) {
        // The PromotionManager will handle visibility now
        // This is just to maintain the interface
    }
    
    private fun minimizeButtonTapped() {
        Log.d("PictureInPictureManager", "minimizeButtonTapped - isMinimized: $isMinimized, isAnimating: $isAnimating")
        
        // Prevent clicks during animations
        if (isAnimating) {
            Log.d("PictureInPictureManager", "Animation in progress, ignoring click")
            return
        }
        
        if (!isMinimized) {
            Log.d("PictureInPictureManager", "Calling minimizeToPiP")
            minimizeToPiP()
        } else {
            Log.d("PictureInPictureManager", "Calling restoreFromPiP")
            restoreFromPiP()
        }
    }
    
    private fun minimizeToPiP() {
        Log.d("PictureInPictureManager", "minimizeToPiP called - Finding Button SDK Activity")
        
        val browser = originalBrowser ?: run {
            Log.e("PictureInPictureManager", "originalBrowser is null")
            return
        }
        
        val browserView = browser.viewContainer ?: run {
            Log.e("PictureInPictureManager", "browser.viewContainer is null")
            return
        }
        
        // Find the WebViewActivity from the browser view chain
        var buttonSdkActivity: Activity? = null
        var currentView = browserView.parent
        
        while (currentView != null) {
            if (currentView is android.view.ViewGroup) {
                val context = currentView.context
                if (context is Activity && context.javaClass.name.contains("WebViewActivity")) {
                    buttonSdkActivity = context
                    Log.d("PictureInPictureManager", "Found Button SDK Activity: ${context.javaClass.name}")
                    break
                }
            }
            currentView = currentView.parent
        }
        
        val targetActivity = buttonSdkActivity ?: run {
            Log.e("PictureInPictureManager", "Could not find Button SDK WebViewActivity")
            return
        }
        
        Log.d("PictureInPictureManager", "Entering native Android PiP mode")
        
        isAnimating = true
        
        try {
            tryNativeAndroidPiP(targetActivity)
            
            isAnimating = false
            isMinimized = true
            delegate?.didMinimize()
            
        } catch (e: Exception) {
            Log.e("PictureInPictureManager", "Failed to enter PiP mode", e)
            isAnimating = false
        }
    }
    
    private fun tryNativeAndroidPiP(buttonActivity: Activity) {
        Log.d("PictureInPictureManager", "Entering native Android Picture-in-Picture mode")
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                // Create PiP params
                val pipParamsBuilder = android.app.PictureInPictureParams.Builder()
                val aspectRatio = android.util.Rational(16, 9)
                pipParamsBuilder.setAspectRatio(aspectRatio)
                val pipParams = pipParamsBuilder.build()
                
                // Enter Picture-in-Picture mode
                val success = buttonActivity.enterPictureInPictureMode(pipParams)
                
                if (success) {
                    Log.d("PictureInPictureManager", "Successfully entered native PiP mode")
                } else {
                    Log.w("PictureInPictureManager", "Failed to enter PiP mode")
                }
                
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error entering PiP mode", e)
            }
        } else {
            Log.w("PictureInPictureManager", "Picture-in-Picture not supported on Android < 8.0")
        }
    }
    
    private fun restoreFromPiP() {
        Log.d("PictureInPictureManager", "restoreFromPiP called")
        
        if (!isMinimized) {
            Log.d("PictureInPictureManager", "Not minimized, nothing to restore")
            return
        }
        
        if (isAnimating) {
            Log.d("PictureInPictureManager", "Animation in progress, ignoring restore request")
            return
        }
        
        Log.d("PictureInPictureManager", "Exiting PiP mode - Android will automatically restore the activity")
        
        isAnimating = false
        isMinimized = false
        delegate?.didRestore()
    }
    
    private fun createChevronDownDrawable(color: Int): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(2).toFloat() // Good visibility
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            override fun draw(canvas: Canvas) {
                val bounds = getBounds()
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()
                
                // Draw chevron down (V shape) - bigger for visibility and touch
                val size = dpToPx(7).toFloat() // Bigger chevron for better visibility
                val path = Path()
                
                // Start from top-left point
                path.moveTo(centerX - size, centerY - size/2)
                // Draw to bottom point
                path.lineTo(centerX, centerY + size/2)
                // Draw to top-right point
                path.lineTo(centerX + size, centerY - size/2)
                
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }

            override fun setColorFilter(colorFilter: ColorFilter?) {
                paint.colorFilter = colorFilter
            }

            override fun getOpacity(): Int {
                return PixelFormat.TRANSLUCENT
            }

            override fun getIntrinsicWidth(): Int = dpToPx(18)
            override fun getIntrinsicHeight(): Int = dpToPx(18)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    fun cleanup() {
        BrowserScrollEventBus.getInstance().removeVisibilityObserver(this)
        
        originalBrowser = null
        containerView = null
        isMinimized = false
        
        Log.d("PictureInPictureManager", "Cleanup completed")
    }
}