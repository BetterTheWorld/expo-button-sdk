package expo.modules.buttonsdk

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
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
    
    var delegate: PictureInPictureManagerDelegate? = null
    
    interface PictureInPictureManagerDelegate {
        fun didMinimize()
        fun didRestore()
    }
    
    fun addMinimizeButton(browser: BrowserInterface) {
        originalBrowser = browser
        Handler(Looper.getMainLooper()).postDelayed({
            createMinimizeButtonInHeader(browser)
        }, 1000)
    }
    
    private fun createMinimizeButtonInHeader(browser: BrowserInterface) {
        val context = this.context
        
        val containerView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        this.containerView = containerView
        
        val minimizeButton = TextView(context).apply {
            text = "⌄"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(
                dpToPx(8),
                dpToPx(4),
                dpToPx(8),
                dpToPx(4)
            )
            
            // Add background to make it more visible
            setBackgroundColor(Color.parseColor("#AA000000"))
            
            setOnClickListener { 
                Log.d("PictureInPictureManager", "Minimize button clicked!")
                minimizeButtonTapped() 
            }
        }
        
        // For now, just add the minimize button. 
        // In the future, this could be enhanced to wrap existing views
        containerView.addView(minimizeButton)
        
        try {
            browser.header.setCustomActionView(containerView)
        } catch (e: Exception) {
            Log.w("PictureInPictureManager", "Failed to set custom action view", e)
        }
        
        Handler(Looper.getMainLooper()).postDelayed({
            BrowserScrollEventBus.getInstance().addVisibilityObserver(this)
            BrowserScrollEventBus.getInstance().startMonitoring(browser)
        }, 200)
    }
    
    override fun onScrollVisibilityChanged(event: ScrollVisibilityEvent) {
        setContainerVisibility(event.shouldShow)
    }
    
    private fun setContainerVisibility(visible: Boolean) {
        val container = containerView ?: return
        
        isButtonHidden = !visible
        
        container.animate()
            .alpha(if (visible) 1.0f else 0.0f)
            .setDuration(300)
            .start()
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