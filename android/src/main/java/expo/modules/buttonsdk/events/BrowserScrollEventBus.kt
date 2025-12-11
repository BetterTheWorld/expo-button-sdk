package expo.modules.buttonsdk.events

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.WebView
import android.widget.ScrollView
import com.usebutton.sdk.purchasepath.BrowserInterface
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

class BrowserScrollEventBus private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: BrowserScrollEventBus? = null
        
        fun getInstance(): BrowserScrollEventBus {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BrowserScrollEventBus().also { INSTANCE = it }
            }
        }
    }
    
    private val visibilityObservers = ConcurrentHashMap<String, WeakReference<ScrollVisibilityObserver>>()
    private val monitoredWebViews = mutableListOf<WeakReference<WebView>>()
    private val scrollListeners = mutableMapOf<WebView, View.OnScrollChangeListener>()
    
    private var currentBrowser: WeakReference<BrowserInterface>? = null
    private var currentVisibilityState = true
    private var lastVisibilityCheckTime = 0L
    private val visibilityCheckThrottle = 16L // 60fps max
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val maxObserverCount = 50
    private val cleanupInterval = 30000L // 30 seconds
    private var lastCleanupTime = 0L
    
    fun addVisibilityObserver(observer: ScrollVisibilityObserver) {
        cleanupStaleObserversIfNeeded()
        
        if (visibilityObservers.size >= maxObserverCount) {
            Log.w("BrowserScrollEventBus", "Maximum visibility observer count reached. Skipping: ${observer.observerId}")
            return
        }
        
        visibilityObservers[observer.observerId] = WeakReference(observer)
        Log.d("BrowserScrollEventBus", "Added visibility observer: ${observer.observerId}")
        
        mainHandler.postDelayed({
            sendCurrentVisibilityState(observer)
        }, 100)
    }
    
    fun removeVisibilityObserver(observer: ScrollVisibilityObserver) {
        visibilityObservers.remove(observer.observerId)
        Log.d("BrowserScrollEventBus", "Removed visibility observer: ${observer.observerId}")
    }
    
    fun startMonitoring(webView: WebView) {
        if (monitoredWebViews.any { it.get() === webView }) {
            Log.d("BrowserScrollEventBus", "Already monitoring webView: ${System.identityHashCode(webView)}")
            return
        }
        
        monitoredWebViews.add(WeakReference(webView))
        
        val scrollListener = View.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastVisibilityCheckTime >= visibilityCheckThrottle) {
                checkVisibilityThreshold(ScrollEventSource.WEB_VIEW)
                lastVisibilityCheckTime = currentTime
            }
        }
        
        scrollListeners[webView] = scrollListener
        webView.setOnScrollChangeListener(scrollListener)
        
        Log.d("BrowserScrollEventBus", "Started monitoring webView: ${System.identityHashCode(webView)}")
        
        checkVisibilityThreshold(ScrollEventSource.USER_TRIGGERED)
    }
    
    fun startMonitoring(browser: BrowserInterface) {
        currentBrowser = WeakReference(browser)
        
        val webView = findWebView(browser.viewContainer)
        if (webView != null) {
            startMonitoring(webView)
        }
        
        checkVisibilityThreshold(ScrollEventSource.USER_TRIGGERED)
    }
    
    fun stopMonitoring(webView: WebView) {
        monitoredWebViews.removeAll { it.get() === webView }
        
        scrollListeners.remove(webView)?.let { listener ->
            webView.setOnScrollChangeListener(null)
        }
        
        Log.d("BrowserScrollEventBus", "Stopped monitoring webView: ${System.identityHashCode(webView)}")
    }
    
    fun stopMonitoring(browser: BrowserInterface?) {
        if (browser != null) {
            val webView = findWebView(browser.viewContainer)
            if (webView != null) {
                stopMonitoring(webView)
            }
        }
        
        if (browser === currentBrowser?.get()) {
            currentBrowser = null
        }
    }
    
    fun stopAllMonitoring() {
        val webViewsCopy = monitoredWebViews.mapNotNull { it.get() }
        for (webView in webViewsCopy) {
            stopMonitoring(webView)
        }
        
        currentBrowser = null
        visibilityObservers.clear()
        scrollListeners.clear()
        
        Log.d("BrowserScrollEventBus", "Stopped all monitoring")
    }
    
    fun checkVisibilityNow() {
        checkVisibilityThreshold(ScrollEventSource.USER_TRIGGERED)
    }
    
    fun getActiveObserverCount(): Int {
        cleanupStaleObserversIfNeeded()
        return visibilityObservers.size
    }
    
    fun getMonitoredWebViewCount(): Int {
        cleanupStaleWebViewsIfNeeded()
        return monitoredWebViews.size
    }
    
    private fun checkVisibilityThreshold(source: ScrollEventSource) {
        val safeAreaInsets = getSafeAreaInsets()
        val shouldHide = isInScrollHideZone(safeAreaInsets)
        val shouldShow = !shouldHide
        
        if (shouldShow != currentVisibilityState) {
            currentVisibilityState = shouldShow
            
            val reason = if (shouldShow) "Scroll position allows UI visibility" else "Scroll position conflicts with system UI"
            val triggerFrame = getAnyHeaderView()?.let { headerView ->
                val location = IntArray(2)
                headerView.getLocationOnScreen(location)
                Rect(location[0], location[1], location[0] + headerView.width, location[1] + headerView.height)
            }
            
            val event = ScrollVisibilityEvent(
                timestamp = System.currentTimeMillis(),
                source = source,
                shouldShow = shouldShow,
                reason = reason,
                triggerFrame = triggerFrame,
                safeAreaInsets = safeAreaInsets,
                hasNotch = safeAreaInsets.top > 72 // roughly 24dp converted to px
            )
            
            notifyVisibilityObservers(event)
        }
    }
    
    private fun isInScrollHideZone(safeAreaInsets: Rect): Boolean {
        val statusBarHeight = safeAreaInsets.top
        val hasNotch = statusBarHeight > 72 // roughly 24dp
        
        val headerView = getAnyHeaderView()
        if (headerView != null) {
            val location = IntArray(2)
            headerView.getLocationOnScreen(location)
            val viewFrame = Rect(location[0], location[1], location[0] + headerView.width, location[1] + headerView.height)
            
            return isViewInNotchArea(viewFrame, safeAreaInsets, hasNotch)
        }
        
        val webView = monitoredWebViews.firstOrNull()?.get()
        if (webView != null) {
            val scrollOffset = webView.scrollY
            val hideThreshold = if (hasNotch) 150 else 60 // roughly 50dp and 20dp converted to px
            return scrollOffset > hideThreshold
        }
        
        return false
    }
    
    private fun isViewInNotchArea(viewFrame: Rect, safeAreaInsets: Rect, hasNotch: Boolean): Boolean {
        val isViewOffScreen = viewFrame.top < -30 // roughly -10dp
        
        if (isViewOffScreen) {
            return true
        }
        
        if (hasNotch) {
            val notchHeight = safeAreaInsets.top
            val conflictThreshold = notchHeight - 30 // roughly 10dp buffer below notch
            val viewTooHighInNotch = viewFrame.top < conflictThreshold
            
            return viewTooHighInNotch
        }
        
        return false
    }
    
    private fun getAnyHeaderView(): View? {
        // For now, return null to avoid customActionView dependency
        // The visibility logic will use scroll position instead
        return null
    }
    
    private fun getSafeAreaInsets(): Rect {
        val context = getCurrentContext() ?: return Rect()
        
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
        
        return Rect(0, statusBarHeight, 0, 0)
    }
    
    private fun getCurrentContext(): Context? {
        val webView = monitoredWebViews.firstOrNull()?.get()
        return webView?.context
    }
    
    private fun findWebView(view: View?): WebView? {
        if (view == null) return null
        if (view is WebView) return view
        
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val webView = findWebView(view.getChildAt(i))
                if (webView != null) return webView
            }
        }
        
        return null
    }
    
    private fun notifyVisibilityObservers(event: ScrollVisibilityEvent) {
        val activeObservers = visibilityObservers.values.mapNotNull { it.get() }
        for (observer in activeObservers) {
            try {
                observer.onScrollVisibilityChanged(event)
            } catch (e: Exception) {
                Log.w("BrowserScrollEventBus", "Error notifying observer ${observer.observerId}", e)
            }
        }
    }
    
    private fun sendCurrentVisibilityState(observer: ScrollVisibilityObserver) {
        val safeAreaInsets = getSafeAreaInsets()
        val shouldHide = isInScrollHideZone(safeAreaInsets)
        val shouldShow = !shouldHide
        
        val event = ScrollVisibilityEvent(
            timestamp = System.currentTimeMillis(),
            source = ScrollEventSource.USER_TRIGGERED,
            shouldShow = shouldShow,
            reason = "Initial state for new observer (re-evaluated)",
            triggerFrame = getAnyHeaderView()?.let { headerView ->
                val location = IntArray(2)
                headerView.getLocationOnScreen(location)
                Rect(location[0], location[1], location[0] + headerView.width, location[1] + headerView.height)
            },
            safeAreaInsets = safeAreaInsets,
            hasNotch = safeAreaInsets.top > 72
        )
        
        currentVisibilityState = shouldShow
        
        try {
            observer.onScrollVisibilityChanged(event)
        } catch (e: Exception) {
            Log.w("BrowserScrollEventBus", "Error sending initial state to observer ${observer.observerId}", e)
        }
    }
    
    private fun cleanupStaleObserversIfNeeded() {
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastCleanupTime > cleanupInterval) {
            cleanupStaleObservers()
            cleanupStaleWebViewsIfNeeded()
            lastCleanupTime = currentTime
        }
    }
    
    private fun cleanupStaleObservers() {
        val visibilityBefore = visibilityObservers.size
        
        val iterator = visibilityObservers.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                iterator.remove()
            }
        }
        
        val visibilityAfter = visibilityObservers.size
        
        if (visibilityBefore != visibilityAfter) {
            Log.d("BrowserScrollEventBus", "Cleaned up ${visibilityBefore - visibilityAfter} visibility observers")
        }
    }
    
    private fun cleanupStaleWebViewsIfNeeded() {
        val staleBefore = monitoredWebViews.size
        
        val iterator = monitoredWebViews.iterator()
        while (iterator.hasNext()) {
            val wrapper = iterator.next()
            if (wrapper.get() == null) {
                iterator.remove()
            }
        }
        
        val staleAfter = monitoredWebViews.size
        
        if (staleBefore != staleAfter) {
            Log.d("BrowserScrollEventBus", "Cleaned up ${staleBefore - staleAfter} stale webViews")
        }
    }
}