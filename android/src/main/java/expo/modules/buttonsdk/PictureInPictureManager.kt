package expo.modules.buttonsdk

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Bundle
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
    private var pipTaskId: Int = -1
    private var isPipHidden = false
    private var mainActivityLifecycleCallback: Application.ActivityLifecycleCallbacks? = null
    private var hideOnAppBackground = false
    private var isRestoringPip = false

    init {
        val animationConfig = options["animationConfig"] as? Map<String, Any>
        val pipConfig = animationConfig?.get("pictureInPicture") as? Map<String, Any>
        pipConfig?.let { config ->
            (config["chevronColor"] as? String)?.let { colorString ->
                try {
                    chevronColor = Color.parseColor(colorString)
                } catch (e: Exception) { }
            }
            earnText = config["earnText"] as? String
            (config["earnTextColor"] as? String)?.let { colorString ->
                try {
                    earnTextColor = Color.parseColor(colorString)
                } catch (e: Exception) { }
            }
            (config["earnTextBackgroundColor"] as? String)?.let { colorString ->
                try {
                    earnTextBackgroundColor = Color.parseColor(colorString)
                } catch (e: Exception) { }
            }
            (config["androidAspectRatio"] as? Map<String, Any>)?.let { ratio ->
                pipAspectRatioWidth = (ratio["width"] as? Number)?.toInt() ?: 16
                pipAspectRatioHeight = (ratio["height"] as? Number)?.toInt() ?: 9
            }
            hideOnAppBackground = config["hideOnAppBackground"] as? Boolean ?: false
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
            } catch (e: Exception) { }
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

    private fun setupMainActivityLifecycleCallback() {
        if (!hideOnAppBackground) return

        removeMainActivityLifecycleCallback()

        val application = (context as? Application) ?: (context.applicationContext as? Application) ?: return

        mainActivityLifecycleCallback = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
                if (activity.javaClass.simpleName == "MainActivity") {
                    if (isRestoringPip) return
                    hidePipInternal()
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (activity.javaClass.simpleName == "MainActivity") {
                    showPipInternal()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }

        application.registerActivityLifecycleCallbacks(mainActivityLifecycleCallback)
    }

    private fun removeMainActivityLifecycleCallback() {
        mainActivityLifecycleCallback?.let { callback ->
            val application = (context as? Application) ?: (context.applicationContext as? Application)
            application?.unregisterActivityLifecycleCallbacks(callback)
            mainActivityLifecycleCallback = null
        }
    }

    private fun hidePipInternal() {
        Handler(Looper.getMainLooper()).post {
            val activity = currentPipActivity?.get()
            if (activity == null || isPipHidden) return@post

            try {
                pipTaskId = activity.taskId
                activity.moveTaskToBack(true)
                isPipHidden = true
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error in hidePipInternal", e)
            }
        }
    }

    private fun showPipInternal() {
        Handler(Looper.getMainLooper()).post {
            val activity = currentPipActivity?.get()
            if (activity == null || pipTaskId == -1 || !isPipHidden) return@post

            try {
                isRestoringPip = true
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.moveTaskToFront(pipTaskId, 0)
                isPipHidden = false

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                val pipParams = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(pipAspectRatioWidth, pipAspectRatioHeight))
                                    .build()
                                activity.enterPictureInPictureMode(pipParams)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PictureInPictureManager", "Error entering PiP in showPipInternal", e)
                    } finally {
                        isRestoringPip = false
                    }
                }, 300)
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error in showPipInternal", e)
                isRestoringPip = false
            }
        }
    }

    fun hidePip() {
        Handler(Looper.getMainLooper()).post {
            val activity = currentPipActivity?.get() ?: return@post
            if (isPipHidden) return@post

            try {
                pipTaskId = activity.taskId
                activity.moveTaskToBack(true)
                isPipHidden = true
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error hiding PiP", e)
            }
        }
    }

    fun showPip() {
        Handler(Looper.getMainLooper()).post {
            val activity = currentPipActivity?.get()
            if (activity == null || pipTaskId == -1 || !isPipHidden) return@post

            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                activityManager.moveTaskToFront(pipTaskId, 0)
                isPipHidden = false

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                val pipParams = android.app.PictureInPictureParams.Builder()
                                    .setAspectRatio(android.util.Rational(pipAspectRatioWidth, pipAspectRatioHeight))
                                    .build()
                                activity.enterPictureInPictureMode(pipParams)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PictureInPictureManager", "Error entering PiP", e)
                    }
                }, 300)
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error showing PiP", e)
            }
        }
    }

    fun onPictureInPictureModeChanged(isInPipMode: Boolean) {
        if (!isInPipMode && isMinimized && !isRestoringPip) {
            hidePipOverlay()
            removeMainActivityLifecycleCallback()
            isMinimized = false
            isPipHidden = false
            pipTaskId = -1
            delegate?.didRestore()
        }
    }

    fun closePipAndProceed(onComplete: () -> Unit) {
        if (!isMinimized) {
            onComplete()
            return
        }

        isClosingForNewContent = true

        delegate = object : PictureInPictureManagerDelegate {
            override fun didMinimize() { }

            override fun didRestore() {
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (targetActivity.isInPictureInPictureMode) {
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
        Handler(Looper.getMainLooper()).postDelayed({
            BrowserScrollEventBus.getInstance().addVisibilityObserver(this)
            BrowserScrollEventBus.getInstance().startMonitoring(browser)
        }, 1000)
    }

    fun createMinimizeButton(context: Context, browser: BrowserInterface? = null): View {
        val button = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = null
            isClickable = true
            isFocusable = true

            setOnClickListener {
                val browserToUse = browser ?: originalBrowser
                if (browserToUse != null) {
                    originalBrowser = browserToUse
                }
                minimizeButtonTapped()
            }
        }

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
        return button
    }

    override fun onScrollVisibilityChanged(event: ScrollVisibilityEvent) { }

    private fun minimizeButtonTapped() {
        if (isAnimating) return

        if (!isMinimized) {
            minimizeToPiP()
        } else {
            restoreFromPiP()
        }
    }

    private fun minimizeToPiP() {
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            try {
                val pipParamsBuilder = android.app.PictureInPictureParams.Builder()
                val aspectRatio = android.util.Rational(pipAspectRatioWidth, pipAspectRatioHeight)
                pipParamsBuilder.setAspectRatio(aspectRatio)
                val pipParams = pipParamsBuilder.build()

                val success = buttonActivity.enterPictureInPictureMode(pipParams)

                if (success) {
                    currentPipActivity = WeakReference(buttonActivity)
                    pipTaskId = buttonActivity.taskId
                    showPipOverlay(buttonActivity)
                    startPipModeChecker(buttonActivity)
                    setupMainActivityLifecycleCallback()
                }
            } catch (e: Exception) {
                Log.e("PictureInPictureManager", "Error entering PiP mode", e)
            }
        }
    }

    private fun showPipOverlay(activity: Activity) {
        if (coverImageUri == null && earnText == null) return

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
                            hidePipOverlay()
                            removeMainActivityLifecycleCallback()
                            isMinimized = false
                            isPipHidden = false
                            pipTaskId = -1
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
        if (!isMinimized || isAnimating) return

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
        removeMainActivityLifecycleCallback()
        originalBrowser = null
        containerView = null
        isMinimized = false
        isPipHidden = false
        isRestoringPip = false
        pipTaskId = -1
    }
}
