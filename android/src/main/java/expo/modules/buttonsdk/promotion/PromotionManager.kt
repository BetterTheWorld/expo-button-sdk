package expo.modules.buttonsdk.promotion

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.usebutton.sdk.purchasepath.BrowserInterface
import expo.modules.buttonsdk.GlobalLoaderManager
import expo.modules.buttonsdk.events.BrowserScrollEventBus
import expo.modules.buttonsdk.events.ScrollVisibilityObserver
import expo.modules.buttonsdk.events.ScrollVisibilityEvent
import java.lang.ref.WeakReference

class PromotionManager(
    context: Context,
    private val promotionData: Map<String, Any>?,
    private var onPromotionClickCallback: ((String, BrowserInterface?) -> Unit)?,
    private val badgeLabel: String = "Offers",
    private val listTitle: String = "Available Promotions",
    private val badgeFontSize: Float = 11f
) : ScrollVisibilityObserver {
    
    private val contextRef = WeakReference(context)
    private var isBottomSheetOpen = false
    private var currentBrowserRef: WeakReference<BrowserInterface>? = null
    private var isButtonHidden = false
    
    companion object {
        private var sharedPendingPromoCode: String? = null
    }
    
    fun setOnPromotionClickCallback(callback: (String, BrowserInterface?) -> Unit) {
        this.onPromotionClickCallback = callback
    }
    
    fun setupPromotionsBadge(browser: BrowserInterface, pipManager: expo.modules.buttonsdk.PictureInPictureManager? = null) {
        android.util.Log.d("PromotionManager", "setupPromotionsBadge called with PiP manager: ${pipManager != null}")
        currentBrowserRef = WeakReference(browser)
        val context = contextRef.get() ?: return
        
        val promotionCount = getPromotionCount()
        android.util.Log.d("PromotionManager", "Promotion count: $promotionCount")
        
        // Create container that will hold both promotion button and minimize button
        val headerContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Add promotion button if there are promotions (left side)
        if (promotionCount > 0) {
            val promotionButton = PromotionUIFactory.createHeaderPromotionButton(
                context, 
                promotionCount,
                badgeLabel,
                badgeFontSize
            ) {
                android.util.Log.d("PromotionManager", "Header promotion button clicked")
                showPromotionsList()
            }
            
            if (promotionButton != null) {
                headerContainer.addView(promotionButton)
            }
        }
        
        // Add flexible spacer to push minimize button to the right
        if (pipManager != null) {
            val spacer = android.view.View(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(0, 1).apply {
                    weight = 1.0f // This will take all available space
                }
            }
            headerContainer.addView(spacer)
        }
        
        // Add minimize button if PiP manager is available (right side)
        pipManager?.let { 
            android.util.Log.d("PromotionManager", "Creating minimize button with PiP manager")
            try {
                val minimizeButton = it.createMinimizeButton(context, browser)
                headerContainer.addView(minimizeButton)
                android.util.Log.d("PromotionManager", "Minimize button added to container")
            } catch (e: Exception) {
                android.util.Log.e("PromotionManager", "Error creating minimize button", e)
            }
        } ?: run {
            android.util.Log.w("PromotionManager", "No PiP manager available - minimize button not added")
        }
        
        try {
            browser.header.setCustomActionView(headerContainer)
            BrowserScrollEventBus.getInstance().addVisibilityObserver(this)
            BrowserScrollEventBus.getInstance().startMonitoring(browser)
            android.util.Log.d("PromotionManager", "Header container set with ${headerContainer.childCount} children")
        } catch (e: Exception) {
            android.util.Log.e("PromotionManager", "Failed to set custom action view", e)
        }
    }
    
    override fun onScrollVisibilityChanged(event: ScrollVisibilityEvent) {
        setButtonVisibility(event.shouldShow)
    }
    
    private fun setButtonVisibility(visible: Boolean) {
        val browser = currentBrowserRef?.get() ?: return
        
        // Try to get customActionView using reflection to avoid compilation issues
        val customActionView = try {
            val headerClass = browser.header.javaClass
            val field = headerClass.getDeclaredField("customActionView")
            field.isAccessible = true
            field.get(browser.header) as? android.view.View
        } catch (e: Exception) {
            android.util.Log.w("PromotionManager", "Could not get custom action view", e)
            null
        } ?: return
        
        isButtonHidden = !visible
        customActionView.animate()
            .alpha(if (visible) 1.0f else 0.0f)
            .setDuration(300)
            .start()
    }
    
    private fun getPromotionCount(): Int {
        promotionData ?: return 0
        
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        val featuredCount = if (featuredPromotion != null) 1 else 0
        
        val promotions = promotionData["promotions"] as? List<*>
        val regularCount = promotions?.size ?: 0
        
        return featuredCount + regularCount
    }
    
    private fun showPromotionsList() {
        promotionData ?: return
        val currentBrowser = currentBrowserRef?.get() ?: return
        val context = contextRef.get() ?: return
        
        val promotionItems = mutableListOf<Pair<String, String>>()
        
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        featuredPromotion?.let { promo ->
            val title = promo["description"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val startsAt = promo["startsAt"] as? String
            
            var actionTitle = title
            if (startsAt != null && PromotionUtils.isPromotionNew(startsAt)) {
                actionTitle = "⭐ $actionTitle"
            }
            
            promotionItems.add(Pair(actionTitle, id))
        }
        
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promo ->
            val title = promo["description"] as? String ?: "Promotion"
            val id = promo["id"] as? String ?: ""
            val startsAt = promo["startsAt"] as? String
            
            var actionTitle = title
            if (startsAt != null && PromotionUtils.isPromotionNew(startsAt)) {
                actionTitle = "NEW! $actionTitle"
            }
            
            if (id.isNotEmpty()) {
                promotionItems.add(Pair(actionTitle, id))
            }
        }
        
        showPromotionsBottomSheet(promotionItems)
    }
    
    private fun showPromotionsBottomSheet(promotions: List<Pair<String, String>>) {
        if (isBottomSheetOpen) {
            android.util.Log.d("PromotionManager", "Bottom sheet already open, ignoring request")
            return
        }

        val browser = currentBrowserRef?.get() ?: return
        val container = browser.viewContainer
        val context = contextRef.get() ?: return
        
        if (container == null) {
            android.util.Log.e("PromotionManager", "Could not get view container from browser")
            return
        }
        
        try {
            isBottomSheetOpen = true
            
            val bottomSheet = PromotionBottomSheetFactory.createBottomSheet(
                context = context,
                promotionData = promotionData,
                listTitle = listTitle,
                onPromotionClick = { promotionId: String ->
                    isBottomSheetOpen = false
                    
                    savePromoCodeForPromotion(promotionId)
                    
                    val activity = context as? Activity
                    if (activity != null) {
                        if (sharedPendingPromoCode != null && sharedPendingPromoCode!!.isNotEmpty()) {
                            GlobalLoaderManager.getInstance().showCopyLoader(activity, sharedPendingPromoCode!!)
                            android.util.Log.d("PromotionManager", "🔄 Copy loader shown for promotion with code: $sharedPendingPromoCode")
                        } else {
                            val promotionLoaderColor = android.graphics.Color.parseColor("#0B72AC")
                            GlobalLoaderManager.getInstance().showLoader(activity, "Loading promotion...", promotionLoaderColor)
                            android.util.Log.d("PromotionManager", "🔄 Generic loader shown for promotion without code")
                            
                            Handler(Looper.getMainLooper()).postDelayed({
                                GlobalLoaderManager.getInstance().hideLoader()
                                android.util.Log.d("PromotionManager", "🔄 Generic loader hidden after 2 seconds (forced)")
                            }, 2000)
                        }
                    }
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        android.util.Log.d("PromotionManager", "📤 Invoking promotion callback for: $promotionId")
                        onPromotionClickCallback?.invoke(promotionId, currentBrowserRef?.get())
                        android.util.Log.d("PromotionManager", "✅ Promotion callback completed")
                    }, 1000)
                },
                onClose = {
                    isBottomSheetOpen = false
                }
            )
            
            container.addView(bottomSheet)
            PromotionBottomSheetFactory.animateBottomSheetEntry(bottomSheet, container)
            
            android.util.Log.d("PromotionManager", "Bottom sheet overlay added successfully with animation")
            
        } catch (e: Exception) {
            android.util.Log.e("PromotionManager", "Error creating bottom sheet overlay", e)
            isBottomSheetOpen = false
        }
    }
    
    private fun savePromoCodeForPromotion(promotionId: String) {
        promotionData ?: run {
            android.util.Log.d("PromotionManager", "🔄 No promotion data available")
            return
        }
        
        sharedPendingPromoCode = null
        
        val featuredPromotion = promotionData["featuredPromotion"] as? Map<String, Any>
        if (featuredPromotion != null) {
            val featuredId = featuredPromotion["id"] as? String
            if (featuredId == promotionId) {
                val promoCode = featuredPromotion["couponCode"] as? String ?: featuredPromotion["code"] as? String
                if (!promoCode.isNullOrEmpty()) {
                    sharedPendingPromoCode = promoCode
                    android.util.Log.d("PromotionManager", "🔄 Saved promo code for featured promotion: $promoCode")
                    return
                }
            }
        }
        
        val promotions = promotionData["promotions"] as? List<Map<String, Any>>
        promotions?.forEach { promo ->
            val id = promo["id"] as? String
            if (id == promotionId) {
                val promoCode = promo["couponCode"] as? String ?: promo["code"] as? String
                if (!promoCode.isNullOrEmpty()) {
                    sharedPendingPromoCode = promoCode
                    android.util.Log.d("PromotionManager", "🔄 Saved promo code for promotion: $promoCode")
                    return
                }
            }
        }
        
        android.util.Log.d("PromotionManager", "🔄 No promo code found for promotion ID: $promotionId")
    }
    
    fun showPendingPromoCodeToast() {
        android.util.Log.d("PromotionManager", "🔄 showPendingPromoCodeToast called, sharedPendingPromoCode: ${sharedPendingPromoCode ?: "nil"}")
        val promoCode = sharedPendingPromoCode
        if (promoCode.isNullOrEmpty()) {
            android.util.Log.d("PromotionManager", "🔄 No pending promo code to show")
            return
        }
        
        val context = contextRef.get() ?: return
        
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clipData = android.content.ClipData.newPlainText("Promo Code", promoCode)
        clipboardManager.setPrimaryClip(clipData)
        android.util.Log.d("PromotionManager", "🔄 Promo code copied: $promoCode")
        
        val activity = context as? Activity
        if (activity != null) {
            activity.runOnUiThread {
                PersistentToastManager.showToast(activity, promoCode)
                android.util.Log.d("PromotionManager", "🔄 Toast shown immediately when Button SDK opened")
            }
        }
        
        Handler(Looper.getMainLooper()).postDelayed({
            GlobalLoaderManager.getInstance().hideLoader()
            android.util.Log.d("PromotionManager", "🔄 Loader hidden after 2 seconds (forced)")
        }, 2000)
        
        sharedPendingPromoCode = null
    }
    
    private fun dpToPx(dp: Int): Int {
        val context = contextRef.get() ?: return dp
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    fun cleanup() {
        try {
            BrowserScrollEventBus.getInstance().removeVisibilityObserver(this)
            android.util.Log.d("PromotionManager", "PromotionManager cleaned up")
        } catch (e: Exception) {
            android.util.Log.e("PromotionManager", "Error during cleanup", e)
        }
    }
}