package expo.modules.buttonsdk

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import java.lang.ref.WeakReference

/**
 * Simulated PiP for Android: makes the browser activity translucent so the
 * RN app is visible behind it, hides browser content, makes the window
 * non-touchable (touches pass through to the app), and shows a floating
 * draggable bubble via WindowManager. Tap the bubble to restore.
 */
class SimulatedPipManager(
    private val context: Context,
    private val options: Map<String, Any>,
    private val onRestore: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "SimulatedPipManager"
    }

    private var isMinimized = false
    private var bubbleView: FrameLayout? = null
    private var browserActivityRef: WeakReference<Activity>? = null
    private var isPipHidden = false
    private var hiddenViews: MutableList<Pair<View, Int>> = mutableListOf()
    private var windowManager: WindowManager? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var isBubbleAttached = false

    // Configuration
    private var pipWidth: Int = 120
    private var pipHeight: Int = 120
    private var pipX: Float = -1f
    private var pipY: Float = -1f
    private var chevronColor: Int = Color.WHITE
    private var earnText: String? = null
    private var earnTextColor: Int = Color.WHITE
    private var earnTextBackgroundColor: Int = Color.parseColor("#99000000")
    private var coverImageUri: String? = null
    private var coverImageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
    private var coverImageBackgroundColor: Int = Color.TRANSPARENT
    private var coverImagePadding: Int = 0

    // Drag state
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragStartParamX = 0
    private var dragStartParamY = 0
    private var lastSavedX: Float? = null
    private var lastSavedY: Float? = null

    init { parseConfig() }

    private fun parseConfig() {
        val animationConfig = options["animationConfig"] as? Map<String, Any>
        val pipConfig = animationConfig?.get("pictureInPicture") as? Map<String, Any>

        pipConfig?.let { config ->
            (config["size"] as? Map<String, Any>)?.let { size ->
                pipWidth = dpToPx((size["width"] as? Number)?.toInt() ?: 120)
                pipHeight = dpToPx((size["height"] as? Number)?.toInt() ?: 120)
            } ?: run {
                pipWidth = dpToPx(120)
                pipHeight = dpToPx(120)
            }
            (config["position"] as? Map<String, Any>)?.let { pos ->
                pipX = dpToPx((pos["x"] as? Number)?.toInt() ?: -1).toFloat()
                pipY = dpToPx((pos["y"] as? Number)?.toInt() ?: -1).toFloat()
            }
            (config["chevronColor"] as? String)?.let { try { chevronColor = Color.parseColor(it) } catch (_: Exception) {} }
            earnText = config["earnText"] as? String
            (config["earnTextColor"] as? String)?.let { try { earnTextColor = Color.parseColor(it) } catch (_: Exception) {} }
            (config["earnTextBackgroundColor"] as? String)?.let { try { earnTextBackgroundColor = Color.parseColor(it) } catch (_: Exception) {} }
        }

        val coverImage = options["coverImage"] as? Map<String, Any>
        coverImageUri = coverImage?.get("uri") as? String
        (coverImage?.get("scaleType") as? String)?.let {
            coverImageScaleType = when (it.lowercase()) {
                "contain" -> ImageView.ScaleType.FIT_CENTER
                "cover" -> ImageView.ScaleType.CENTER_CROP
                "center" -> ImageView.ScaleType.CENTER_INSIDE
                "stretch" -> ImageView.ScaleType.FIT_XY
                else -> ImageView.ScaleType.CENTER_CROP
            }
        }
        (coverImage?.get("backgroundColor") as? String)?.let { try { coverImageBackgroundColor = Color.parseColor(it) } catch (_: Exception) {} }
        (coverImage?.get("padding") as? Number)?.let { coverImagePadding = dpToPx(it.toInt()) }
    }

    fun isActive(): Boolean = isMinimized

    fun minimize(browserActivity: Activity, @Suppress("UNUSED_PARAMETER") mainActivity: Activity) {
        if (isMinimized) return
        browserActivityRef = WeakReference(browserActivity)

        Handler(Looper.getMainLooper()).post {
            val activity = browserActivityRef?.get()
            if (activity == null || activity.isFinishing || activity.isDestroyed) return@post

            try {
                val contentFrame = activity.findViewById<ViewGroup>(android.R.id.content) ?: return@post

                // 1. Make activity translucent FIRST so system renders RN activity behind
                convertToTranslucent(activity)

                // 2. Make ALL window backgrounds fully transparent
                activity.window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
                activity.window.decorView.setBackgroundColor(Color.TRANSPARENT)
                contentFrame.setBackgroundColor(Color.TRANSPARENT)

                // 3. Hide browser content views
                hiddenViews.clear()
                for (i in 0 until contentFrame.childCount) {
                    val child = contentFrame.getChildAt(i)
                    hiddenViews.add(Pair(child, child.visibility))
                    child.visibility = View.GONE
                }

                // 4. Make window not touchable - touches pass through to RN app behind
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

                // 5. Add floating bubble via WindowManager (receives its own touches)
                addBubble(activity)

                // 6. Register lifecycle callbacks
                registerLifecycleCallbacks(activity)

                isMinimized = true
                Log.d(TAG, "Minimized: translucent + transparent bg + not-touchable + bubble")
            } catch (e: Exception) {
                Log.e(TAG, "Error minimizing", e)
            }
        }
    }

    private fun addBubble(activity: Activity) {
        if (isBubbleAttached) return

        val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val screenW = activity.resources.displayMetrics.widthPixels
        val screenH = activity.resources.displayMetrics.heightPixels
        val x = lastSavedX?.toInt() ?: if (pipX >= 0) pipX.toInt() else (screenW - pipWidth - dpToPx(16))
        val y = lastSavedY?.toInt() ?: if (pipY >= 0) pipY.toInt() else (screenH - pipHeight - dpToPx(100))

        val bubble = createBubble(activity)
        val params = WindowManager.LayoutParams(
            pipWidth, pipHeight,
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
            token = activity.window.decorView.windowToken
        }

        wm.addView(bubble, params)
        bubbleView = bubble
        bubbleLayoutParams = params
        isBubbleAttached = true

        // Scale-in animation
        bubble.scaleX = 0f
        bubble.scaleY = 0f
        bubble.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun removeBubble() {
        if (!isBubbleAttached) return
        try {
            bubbleView?.let { windowManager?.removeViewImmediate(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Error removing bubble", e)
        }
        bubbleView = null
        bubbleLayoutParams = null
        isBubbleAttached = false
    }

    private fun createBubble(activity: Activity): FrameLayout {
        val bubble = FrameLayout(activity).apply {
            val bgDrawable = GradientDrawable().apply {
                setColor(if (coverImageUri != null) coverImageBackgroundColor else Color.parseColor("#333333"))
                cornerRadius = dpToPx(12).toFloat()
            }
            background = bgDrawable
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, dpToPx(12).toFloat())
                }
            }
            elevation = dpToPx(8).toFloat()
        }

        if (coverImageUri != null) {
            val iv = ImageView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply { setMargins(coverImagePadding, coverImagePadding, coverImagePadding, coverImagePadding) }
                scaleType = coverImageScaleType
            }
            loadImageFromUrl(coverImageUri!!, iv)
            bubble.addView(iv)
        }

        bubble.addView(ImageView(activity).apply {
            val s = dpToPx(20)
            layoutParams = FrameLayout.LayoutParams(s, s).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dpToPx(8); rightMargin = dpToPx(8)
            }
            setImageDrawable(createChevronUpDrawable(chevronColor))
        })

        if (!earnText.isNullOrEmpty()) {
            bubble.addView(TextView(activity).apply {
                text = earnText; setTextColor(earnTextColor); textSize = 12f
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                background = GradientDrawable().apply {
                    setColor(earnTextBackgroundColor); cornerRadius = dpToPx(4).toFloat()
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; bottomMargin = dpToPx(8) }
            })
        }

        setupDrag(bubble)
        return bubble
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrag(view: FrameLayout) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    dragStartX = event.rawX; dragStartY = event.rawY
                    val params = bubbleLayoutParams ?: return@setOnTouchListener false
                    dragStartParamX = params.x; dragStartParamY = params.y
                    view.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartX; val dy = event.rawY - dragStartY
                    if (Math.sqrt((dx * dx + dy * dy).toDouble()) > dpToPx(5)) {
                        isDragging = true
                        val params = bubbleLayoutParams ?: return@setOnTouchListener false
                        params.x = (dragStartParamX + dx).toInt()
                        params.y = (dragStartParamY + dy).toInt()
                        try { windowManager?.updateViewLayout(view, params) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (!isDragging) restore() else snapToEdge(view)
                    isDragging = false; true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    isDragging = false; true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(view: View) {
        val activity = browserActivityRef?.get() ?: return
        val params = bubbleLayoutParams ?: return
        val sw = activity.resources.displayMetrics.widthPixels
        val sh = activity.resources.displayMetrics.heightPixels
        val m = dpToPx(10)
        val cx = params.x + view.width / 2
        val tx = if (cx < sw / 2) m else sw - view.width - m
        val ty = params.y.coerceIn(dpToPx(50), sh - view.height - dpToPx(50))
        params.x = tx; params.y = ty
        try { windowManager?.updateViewLayout(view, params) } catch (_: Exception) {}
        lastSavedX = tx.toFloat(); lastSavedY = ty.toFloat()
    }

    fun restore() {
        if (!isMinimized) return
        isMinimized = false

        Handler(Looper.getMainLooper()).post {
            val activity = browserActivityRef?.get()
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                removeBubble()
                removeLifecycleCallbacks()
                isPipHidden = false
                onRestore?.invoke()
                return@post
            }

            // 1. Scale-out bubble then remove
            val bubble = bubbleView
            if (bubble != null && isBubbleAttached) {
                bubble.animate().scaleX(0f).scaleY(0f).setDuration(150).withEndAction {
                    removeBubble()
                }.start()
            } else {
                removeBubble()
            }

            // 2. Undo window flags
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)

            // 3. Convert back from translucent and restore window background
            convertFromTranslucent(activity)
            activity.window.setBackgroundDrawableResource(android.R.color.white)

            // 4. Restore content visibility
            for ((view, vis) in hiddenViews) {
                view.visibility = vis
            }
            hiddenViews.clear()

            // 5. Remove lifecycle callbacks
            removeLifecycleCallbacks()

            isPipHidden = false
            Log.d(TAG, "Restored")
            onRestore?.invoke()
        }
    }

    fun closePipForNewContent() { if (isMinimized) restore() }

    fun hide() {
        Handler(Looper.getMainLooper()).post {
            if (!isMinimized || isPipHidden) return@post
            removeBubble()
            isPipHidden = true
        }
    }

    fun show() {
        Handler(Looper.getMainLooper()).post {
            val activity = browserActivityRef?.get() ?: return@post
            if (isMinimized && isPipHidden) {
                addBubble(activity)
                isPipHidden = false
            }
        }
    }

    fun cleanup() {
        removeBubble()
        removeLifecycleCallbacks()

        val activity = browserActivityRef?.get()
        if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
            Handler(Looper.getMainLooper()).post {
                try {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                    convertFromTranslucent(activity)
                    for ((v, vis) in hiddenViews) { v.visibility = vis }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during cleanup restore", e)
                }
            }
        }
        hiddenViews.clear()
        isMinimized = false
        isPipHidden = false
        browserActivityRef = null
        lastSavedX = null
        lastSavedY = null
        windowManager = null
    }

    // --- Translucent conversion via hidden API ---

    private fun convertToTranslucent(activity: Activity) {
        try {
            val method = Activity::class.java.getDeclaredMethod(
                "convertToTranslucent",
                Class.forName("android.app.Activity\$TranslucentConversionListener"),
                android.app.ActivityOptions::class.java
            )
            method.isAccessible = true
            method.invoke(activity, null, null)
            Log.d(TAG, "convertToTranslucent success")
        } catch (e: Exception) {
            Log.e(TAG, "convertToTranslucent failed", e)
        }
    }

    private fun convertFromTranslucent(activity: Activity) {
        try {
            val method = Activity::class.java.getDeclaredMethod("convertFromTranslucent")
            method.isAccessible = true
            method.invoke(activity)
            Log.d(TAG, "convertFromTranslucent success")
        } catch (e: Exception) {
            Log.e(TAG, "convertFromTranslucent failed", e)
        }
    }

    // --- Lifecycle callbacks ---

    private fun registerLifecycleCallbacks(activity: Activity) {
        removeLifecycleCallbacks()
        val app = activity.application ?: return

        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(a: Activity) {
                // Remove bubble before background to prevent WindowLeaked
                if (a === browserActivityRef?.get() && isMinimized && isBubbleAttached) {
                    Log.d(TAG, "Browser paused - removing bubble to prevent leak")
                    removeBubble()
                }
            }
            override fun onActivityResumed(a: Activity) {
                // Re-add bubble when coming back from background
                if (a === browserActivityRef?.get() && isMinimized && !isBubbleAttached && !isPipHidden) {
                    Log.d(TAG, "Browser resumed - re-adding bubble")
                    addBubble(a)
                }
            }
            override fun onActivityDestroyed(a: Activity) {
                if (a === browserActivityRef?.get()) {
                    Log.d(TAG, "Browser destroyed - cleaning up")
                    removeBubble()
                    removeLifecycleCallbacks()
                    hiddenViews.clear()
                    isMinimized = false
                    isPipHidden = false
                    browserActivityRef = null
                    windowManager = null
                }
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
        }

        app.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    private fun removeLifecycleCallbacks() {
        lifecycleCallbacks?.let {
            val app = browserActivityRef?.get()?.application
                ?: (context.applicationContext as? Application)
            app?.unregisterActivityLifecycleCallbacks(it)
            lifecycleCallbacks = null
        }
    }

    // --- Drawing helpers ---

    private fun createChevronUpDrawable(color: Int): Drawable {
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color; style = Paint.Style.STROKE
                strokeWidth = dpToPx(2).toFloat(); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            override fun draw(canvas: Canvas) {
                val b = getBounds(); val cx = b.centerX().toFloat(); val cy = b.centerY().toFloat()
                val s = dpToPx(6).toFloat(); val path = Path()
                path.moveTo(cx - s, cy + s * 0.4f); path.lineTo(cx, cy - s * 0.4f); path.lineTo(cx + s, cy + s * 0.4f)
                canvas.drawPath(path, paint)
            }
            override fun setAlpha(a: Int) { paint.alpha = a }
            override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
            override fun getIntrinsicWidth(): Int = dpToPx(16)
            override fun getIntrinsicHeight(): Int = dpToPx(16)
        }
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        Thread {
            try {
                val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                c.doInput = true; c.connect()
                val bmp = BitmapFactory.decodeStream(c.inputStream)
                Handler(Looper.getMainLooper()).post { imageView.setImageBitmap(bmp) }
            } catch (e: Exception) { Log.e(TAG, "Error loading image", e) }
        }.start()
    }

    private fun dpToPx(dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}
