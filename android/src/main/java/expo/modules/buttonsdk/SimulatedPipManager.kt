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
 * Persistent Simulated PiP: shows a floating draggable bubble on MainActivity.
 * The browser is dismissed (SDK kills it), and the bubble allows re-opening.
 * The bubble survives HOME press by re-attaching on activity resume.
 */
class SimulatedPipManager(
    private val context: Context,
    private val options: Map<String, Any>,
    private val onReopen: (() -> Unit)? = null
) {

    companion object {
        private const val TAG = "SimulatedPipManager"
    }

    private var isActive = false
    private var bubbleView: FrameLayout? = null
    private var hostActivityRef: WeakReference<Activity>? = null
    private var windowManager: WindowManager? = null
    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var isBubbleAttached = false
    private var pendingBubbleShow = false

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

    fun isActive(): Boolean = isActive

    /**
     * Enter persistent PiP mode: register lifecycle callbacks and show bubble
     * when the host activity is next resumed (after the browser closes).
     */
    fun enterPersistentMode(hostActivity: Activity) {
        if (isActive) return
        hostActivityRef = WeakReference(hostActivity)
        isActive = true
        pendingBubbleShow = true
        registerLifecycleCallbacks(hostActivity)
        Log.d(TAG, "Entered persistent mode, waiting for activity resume to show bubble")
    }

    private fun addBubble(activity: Activity) {
        if (isBubbleAttached) return

        try {
            val token = activity.window?.decorView?.windowToken
            if (token == null) {
                Log.w(TAG, "Window token is null, cannot add bubble")
                return
            }

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
                this.token = token
            }

            wm.addView(bubble, params)
            bubbleView = bubble
            bubbleLayoutParams = params
            isBubbleAttached = true

            // Scale-in animation
            bubble.scaleX = 0f
            bubble.scaleY = 0f
            bubble.animate().scaleX(1f).scaleY(1f).setDuration(200).start()

            Log.d(TAG, "Bubble shown on ${activity.javaClass.simpleName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bubble: ${e.message}", e)
            isBubbleAttached = false
        }
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
                    if (!isDragging) onBubbleTapped() else snapToEdge(view)
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

    private fun onBubbleTapped() {
        Log.d(TAG, "Bubble tapped - re-opening browser")
        // Scale-out animation then reopen
        val bubble = bubbleView
        if (bubble != null && isBubbleAttached) {
            bubble.animate().scaleX(0f).scaleY(0f).setDuration(150).withEndAction {
                removeBubble()
                removeLifecycleCallbacks()
                isActive = false
                pendingBubbleShow = false
                onReopen?.invoke()
            }.start()
        } else {
            removeBubble()
            removeLifecycleCallbacks()
            isActive = false
            pendingBubbleShow = false
            onReopen?.invoke()
        }
    }

    private fun snapToEdge(view: View) {
        val activity = hostActivityRef?.get() ?: return
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

    fun hide() {
        Handler(Looper.getMainLooper()).post {
            if (!isActive) return@post
            removeBubble()
        }
    }

    fun show() {
        Handler(Looper.getMainLooper()).post {
            val activity = hostActivityRef?.get() ?: return@post
            if (isActive && !isBubbleAttached) {
                addBubble(activity)
            }
        }
    }

    fun cleanup() {
        removeBubble()
        removeLifecycleCallbacks()
        isActive = false
        pendingBubbleShow = false
        hostActivityRef = null
        lastSavedX = null
        lastSavedY = null
        windowManager = null
        Log.d(TAG, "Cleaned up")
    }

    // --- Lifecycle callbacks ---

    private fun registerLifecycleCallbacks(activity: Activity) {
        removeLifecycleCallbacks()
        val app = activity.application ?: return

        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                val isHost = a === hostActivityRef?.get()
                Log.d(TAG, "onActivityResumed: ${a.javaClass.simpleName} isHost=$isHost isActive=$isActive isBubbleAttached=$isBubbleAttached pending=$pendingBubbleShow")
                if (isHost && isActive) {
                    if (pendingBubbleShow || !isBubbleAttached) {
                        Log.d(TAG, "Host activity resumed - showing bubble")
                        addBubble(a)
                        pendingBubbleShow = false
                    }
                }
            }
            override fun onActivityPaused(a: Activity) {
                val isHost = a === hostActivityRef?.get()
                Log.d(TAG, "onActivityPaused: ${a.javaClass.simpleName} isHost=$isHost isActive=$isActive isBubbleAttached=$isBubbleAttached")
                if (isHost && isActive && isBubbleAttached) {
                    Log.d(TAG, "Host activity paused - removing bubble to prevent leak")
                    removeBubble()
                }
            }
            override fun onActivityDestroyed(a: Activity) {
                val isHost = a === hostActivityRef?.get()
                Log.d(TAG, "onActivityDestroyed: ${a.javaClass.simpleName} isHost=$isHost")
                if (isHost) {
                    Log.d(TAG, "Host activity destroyed - cleaning up")
                    cleanup()
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
            val app = hostActivityRef?.get()?.application
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
