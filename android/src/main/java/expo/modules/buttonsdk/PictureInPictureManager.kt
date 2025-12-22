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
import java.lang.ref.WeakReference

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
    private var chevronColor: Int = Color.WHITE
    private var earnText: String? = null
    private var earnTextColor: Int = Color.WHITE
    private var earnTextBackgroundColor: Int = Color.parseColor("#99000000")
    private var coverImageUri: String? = null
    private var coverImageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
    private var coverImageBackgroundColor: Int = Color.TRANSPARENT
    private var coverImagePadding: Int = 0
    private var pipAspectRatioWidth: Int = 16
    private var pipAspectRatioHeight: Int = 9
    private var pipOverlayView: View? = null
    private var currentPipActivity: WeakReference<Activity>? = null
    private var pipModeChecker: Runnable? = null
    private val pipModeHandler = Handler(Looper.getMainLooper())
    private var isPipHidden = false
    private var pipTaskId: Int = -1
    private var isRestoringPip = false
    
    init {
        val animationConfig = options["animationConfig"] as? Map<String, Any>
        val pipConfig = animationConfig?.get("pictureInPicture") as? Map<String, Any>
        pipConfig?.let { config ->
            (config["chevronColor"] as? String)?.let { colorString ->
                try {
                    chevronColor = Color.parseColor(colorString)
                } catch (e: Exception) {
                    Log.w("PictureInPictureManager", "Invalid chevronColor: $colorString")
                }
            }
            earnText = config["earnText"] as? String
            (config["earnTextColor"] as? String)?.let { colorString ->
                try {
                    earnTextColor = Color.parseColor(colorString)
                } catch (e: Exception) {
                    Log.w("PictureInPictureManager", "Invalid earnTextColor: $colorString")
                }
            }
            (config["earnTextBackgroundColor"] as? String)?.let { colorString ->
                try {
                    earnTextBackgroundColor = Color.parseColor(colorString)
                } catch (e: Exception) {
                    Log.w("PictureInPictureManager", "Invalid earnTextBackgroundColor: $colorString")
                }
            }
            (config["androidAspectRatio"] as? Map<String, Any>)?.let { ratio ->
                pipAspectRatioWidth = (ratio["width"] as? Number)?.toInt() ?: 16
                pipAspectRatioHeight = (ratio["height"] as? Number)?.toInt() ?: 9
            }
        }
        
        val coverImage = options["coverImage"] as? Map<String, Any>
        coverImageUri = coverImage?.get("uri") as? String
        (coverImage?.get("scaleType") as? String)?.let { scaleTypeString ->
            coverImageScaleType = when (scaleTypeString.lowercase()) {
                "contain" -> ImageView.ScaleType.FIT_CENTER
                "cover" -> ImageView.ScaleType.CENTER_CROP
                "center" -> ImageView.ScaleType.CENTER_INSIDE
                "stretch" -> ImageView.ScaleType.FIT_XY
                else -> ImageView.ScaleType.CENTER_CROP
            }
        }
        (coverImage?.get("backgroundColor") as? String)?.let { colorString ->
            try {
                coverImageBackgroundColor = Color.parseColor(colorString)
            } catch (e: Exception) {
                Log.w("PictureInPictureManager", "Invalid backgroundColor: $colorString")
            }
        }
        (coverImage?.get("padding") as? Number)?.let { padding ->
            coverImagePadding = dpToPx(padding.toInt())
        }
    }
    
    var delegate: PictureInPictureManagerDelegate? = null
    
    interface PictureInPictureManagerDelegate {
        fun didMinimize()
        fun didRestore()
    }
    
    fun isPipActive(): Boolean {
        return isMinimized
    }
    
    fun hidePip() {
        Handler(Looper.getMainLooper()).post {
            if (!isMinimized || isPipHidden) return@post
            
            currentPipActivity?.get()?.let { activity ->
                try {
                    pipTaskId = activity.taskId
                    activity.moveTaskToBack(true)
                    isPipHidden = true
                    Log.d("PictureInPictureManager", "PiP hidden, taskId: $pipTaskId")
                } catch (e: Exception) {
                    Log.e("PictureInPictureManager", "Error hiding PiP", e)
                }
            }
        }
    }
    
    fun showPip() {
        Handler(Looper.getMainLooper()).post {
            Log.d("PictureInPictureManager", "showPip called - isMinimized: $isMinimized, isPipHidden: $isPipHidden, taskId: $pipTaskId")
            if (!isMinimized || !isPipHidden) return@post
            
            try {
                if (pipTaskId != -1) {
                    isRestoringPip = true
                    
                    pipOverlayView?.alpha = 0f
                    currentPipActivity?.get()?.window?.decorView?.alpha = 0f
                    
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    activityManager.moveTaskToFront(pipTaskId, 0)
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentPipActivity?.get()?.let { activity ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                val pipParams = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(pipAspectRatioWidth, pipAspectRatioHeight))
                                    .build()
                                activity.enterPictureInPictureMode(pipParams)
                                Log.d("PictureInPictureManager", "Re-entered PiP mode")
                                
                                Handler(Looper.getMainLooper()).postDelayed({
                                    activity.window?.decorView?.animate()?.alpha(1f)?.setDuration(150)?.start()
                                    pipOverlayView?.animate()?.alpha(1f)?.setDuration(150)?.start()
                                }, 50)
                            }
                        }
                        isRestoringPip = false
                    }, 100)
                    
                    isPipHidden = false
                    Log.d("PictureInPictureManager", "PiP shown via moveTaskToFront")
                }
            } catch (e: Exception) {
                isRestoringPip = false
                Log.e("PictureInPictureManager", "Error showing PiP", e)
            }
        }
    }
    
    fun onPictureInPictureModeChanged(isInPipMode: Boolean) {
        Log.d("PictureInPictureManager", "onPictureInPictureModeChanged: $isInPipMode")
        if (!isInPipMode && isMinimized) {
            hidePipOverlay()
            isMinimized = false
            delegate?.didRestore()
        }
    }
    
    fun closePipAndProceed(onComplete: () -> Unit) {
        Log.d("PictureInPictureManager", "Closing PiP and proceeding with callback, isMinimized: $isMinimized, isPipHidden: $isPipHidden")
        
        if (!isMinimized && !isPipHidden) {
            Log.d("PictureInPictureManager", "PiP not active, proceeding immediately")
            onComplete()
            return
        }
        
        if (isPipHidden) {
            Log.d("PictureInPictureManager", "PiP is hidden, cleaning up and proceeding")
            Handler(Looper.getMainLooper()).post {
                hidePipOverlay()
                isMinimized = false
                isPipHidden = false
                isRestoringPip = false
                pipTaskId = -1
                onComplete()
            }
            return
        }
        
        isClosingForNewContent = true
        
        delegate = object : PictureInPictureManagerDelegate {
            override fun didMinimize() {
            }
            
            override fun didRestore() {
                Log.d("PictureInPictureManager", "PiP closed, executing callback")
                isClosingForNewContent = false
                onComplete()
                delegate = null
            }
        }
        
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
        
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            
            background = null
            
            isClickable = true
            isFocusable = true
            
            setOnClickListener { 
                Log.d("PictureInPictureManager", "Minimize button clicked!")
                
                val browserToUse = browser ?: originalBrowser
                if (browserToUse != null) {
                    originalBrowser = browserToUse
                }
                
                minimizeButtonTapped() 
            }
        }
        
        // Create the chevron icon
        val chevronIcon = ImageView(context).apply {
            val iconSize = dpToPx(28)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            
            val chevronDrawable = createChevronDownDrawableForHeader(chevronColor)
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
                val pipParamsBuilder = android.app.PictureInPictureParams.Builder()
                val aspectRatio = android.util.Rational(pipAspectRatioWidth, pipAspectRatioHeight)
                pipParamsBuilder.setAspectRatio(aspectRatio)
                val pipParams = pipParamsBuilder.build()
                
                val success = buttonActivity.enterPictureInPictureMode(pipParams)
                
                if (success) {
                    Log.d("PictureInPictureManager", "Successfully entered native PiP mode")
                    showPipOverlay(buttonActivity)
                    startPipModeChecker(buttonActivity)
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
    
    private fun showPipOverlay(activity: Activity) {
        if (coverImageUri == null && earnText == null) {
            Log.d("PictureInPictureManager", "No cover image or earn text, skipping overlay")
            return
        }
        
        currentPipActivity = WeakReference(activity)
        
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val rootView = activity.window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
                
                val overlayContainer = android.widget.FrameLayout(activity).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                        android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(coverImageBackgroundColor)
                    isClickable = false
                    isFocusable = false
                }
                
                if (coverImageUri != null) {
                    val imageView = ImageView(activity).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        ).apply {
                            setMargins(coverImagePadding, coverImagePadding, coverImagePadding, coverImagePadding)
                        }
                        scaleType = coverImageScaleType
                    }
                    loadImageFromUrl(coverImageUri!!, imageView)
                    overlayContainer.addView(imageView)
                }
                
                if (!earnText.isNullOrEmpty()) {
                    val earnLabel = TextView(activity).apply {
                        text = earnText
                        setTextColor(earnTextColor)
                        textSize = 12f
                        setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                        
                        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                            setColor(earnTextBackgroundColor)
                            cornerRadius = dpToPx(4).toFloat()
                        }
                        background = bgDrawable
                        
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            bottomMargin = dpToPx(16)
                        }
                    }
                    overlayContainer.addView(earnLabel)
                }
                
                rootView.addView(overlayContainer)
                pipOverlayView = overlayContainer
                
                Log.d("PictureInPictureManager", "PiP overlay added successfully")
                
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error showing PiP overlay", e)
            }
        }, 300)
    }
    
    private fun startPipModeChecker(activity: Activity) {
        stopPipModeChecker()
        
        pipModeChecker = object : Runnable {
            override fun run() {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        if (!activity.isInPictureInPictureMode && isMinimized && !isPipHidden && !isRestoringPip) {
                            Log.d("PictureInPictureManager", "Detected exit from PiP mode")
                            hidePipOverlay()
                            isMinimized = false
                            delegate?.didRestore()
                            stopPipModeChecker()
                            return
                        }
                    }
                    pipModeHandler.postDelayed(this, 200)
                } catch (e: Exception) {
                    Log.e("PictureInPictureManager", "Error in PiP mode checker", e)
                    stopPipModeChecker()
                }
            }
        }
        pipModeHandler.postDelayed(pipModeChecker!!, 500)
    }
    
    private fun stopPipModeChecker() {
        pipModeChecker?.let {
            pipModeHandler.removeCallbacks(it)
            pipModeChecker = null
        }
    }
    
    private fun hidePipOverlay() {
        stopPipModeChecker()
        pipOverlayView?.let { overlay ->
            try {
                (overlay.parent as? android.view.ViewGroup)?.removeView(overlay)
                pipOverlayView = null
                Log.d("PictureInPictureManager", "PiP overlay removed")
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error removing PiP overlay", e)
            }
        }
    }
    
    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                Handler(Looper.getMainLooper()).post {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error loading image from URL", e)
            }
        }.start()
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
        
        hidePipOverlay()
        
        isAnimating = false
        isMinimized = false
        delegate?.didRestore()
    }
    
    private fun createChevronDownDrawableForHeader(color: Int): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(3).toFloat()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            override fun draw(canvas: Canvas) {
                val bounds = getBounds()
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()
                
                val size = dpToPx(7).toFloat()
                val path = Path()
                
                path.moveTo(centerX - size, centerY - size * 0.4f)
                path.lineTo(centerX, centerY + size * 0.4f)
                path.lineTo(centerX + size, centerY - size * 0.4f)
                
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            override fun getIntrinsicWidth(): Int = dpToPx(20)
            override fun getIntrinsicHeight(): Int = dpToPx(20)
        }
    }
    
    private fun createChevronDownDrawable(color: Int): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = dpToPx(2).toFloat()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            override fun draw(canvas: Canvas) {
                val bounds = getBounds()
                val centerX = bounds.centerX().toFloat()
                val centerY = bounds.centerY().toFloat()
                
                val size = dpToPx(8).toFloat()
                val path = Path()
                
                path.moveTo(centerX - size, centerY - size * 0.4f)
                path.lineTo(centerX, centerY + size * 0.4f)
                path.lineTo(centerX + size, centerY - size * 0.4f)
                
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            override fun getIntrinsicWidth(): Int = dpToPx(16)
            override fun getIntrinsicHeight(): Int = dpToPx(16)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    fun cleanup() {
        BrowserScrollEventBus.getInstance().removeVisibilityObserver(this)
        
        stopPipModeChecker()
        hidePipOverlay()
        
        originalBrowser = null
        containerView = null
        isMinimized = false
        
        Log.d("PictureInPictureManager", "Cleanup completed")
    }
}