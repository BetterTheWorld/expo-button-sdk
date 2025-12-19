package expo.modules.buttonsdk

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.graphics.Color
import com.usebutton.sdk.Button
import com.usebutton.sdk.purchasepath.*
import java.lang.ref.WeakReference
import android.webkit.URLUtil

class ExpoButtonSdkModule() : Module() {
  
  // Keep reference to current PictureInPictureManager to close PiP on new purchase paths
  companion object {
    private var currentPictureInPictureManager: PictureInPictureManager? = null
    private var isNewPurchasePathStarting = false
  }

  override fun definition() = ModuleDefinition {
    Name("ExpoButtonSdk")

    Events("onPromotionClick")

    AsyncFunction("initializeSDK") { promise: Promise ->
      promise.resolve(true)
    }

    Function("setIdentifier") { identifier: String ->
      Log.d("ButtonSdk", "setIdentifier: $identifier")
      Button.user().setIdentifier(identifier)
    }

    Function("clearAllData") {
      Log.d("ButtonSdk", "clearAllData")
      Button.clearAllData()
    }
    
    Function("hidePip") {
      Log.d("ButtonSdk", "hidePip")
      currentPictureInPictureManager?.hidePip()
    }
    
    Function("showPip") {
      Log.d("ButtonSdk", "showPip")
      currentPictureInPictureManager?.showPip()
    }

    AsyncFunction("startPurchasePath") { params: Map<String, Any>, promise: Promise ->
      val url = params["url"] as? String ?: ""
      val token = params["token"] as? String

      // Set flag that new purchase path is starting from React Native
      isNewPurchasePathStarting = true
      Log.d("ButtonSdk", "NEW PURCHASE PATH STARTING - Flag set to bypass exit modal")
      
      // Safety reset after 5 seconds in case something fails
      Handler(Looper.getMainLooper()).postDelayed({
        if (isNewPurchasePathStarting) {
          Log.d("ButtonSdk", "SAFETY: Resetting isNewPurchasePathStarting flag after timeout")
          isNewPurchasePathStarting = false
        }
      }, 5000)

      // Check if PiP is active and close it BEFORE starting new purchase path
      currentPictureInPictureManager?.let { pipManager ->
        if (pipManager.isPipActive()) {
          Log.d("ButtonSdk", "PiP is active, closing it before new startPurchasePath")
          pipManager.closePipAndProceed {
            Log.d("ButtonSdk", "PiP closed, continuing with new purchase path")
          }
        }
      }

      // URL validation with backwards compatibility
      if (url.isBlank()) {
        promise.reject("INVALID_URL", "URL cannot be blank", null)
        return@AsyncFunction
      }

      // Validate URL format but allow edge cases for backwards compatibility
      if (!isValidUrlSafe(url)) {
        Log.w("ButtonSdk", "URL might be invalid but proceeding for backwards compatibility: $url")
      }

      val request = PurchasePathRequest(url).apply {
        token?.let {
          Log.d("ButtonSdk", "startPurchasePath, token: $it")
          this.pubRef = it
        }
      }

      Button.purchasePath().fetch(request, object : PurchasePathListener {
        override fun onComplete(purchasePath: PurchasePath?, throwable: Throwable?) {
          if (purchasePath != null) {
            val activity = appContext.activityProvider?.currentActivity
            if (activity != null) {
              Button.purchasePath().extension = CustomPurchasePathExtension(activity, params) { promotionId ->
                try {
                  val eventBody = Bundle().apply {
                    putString("promotionId", promotionId)
                  }
                  sendEvent("onPromotionClick", eventBody)
                } catch (e: Exception) {
                  Log.e("ButtonSdk", "Error sending promotion click event", e)
                }
              }
              purchasePath.start(activity)
            } else {
              promise.reject("ERROR", "No context for purchasePath", throwable)
            }
            promise.resolve(null)
          } else {
            Log.e("ButtonSdk", "Error fetching purchasePath", throwable)
            promise.reject("ERROR", "Error fetching purchasePath", throwable)
          }
        }
      })
    }
  }

  class CustomPurchasePathExtension(
    activity: Activity,
    private val options: Map<String, Any>,
    private val onPromotionClick: (String) -> Unit
  ) : PurchasePathExtension {

    // Use WeakReference to prevent memory leaks
    private val activityRef = WeakReference(activity)

    // Exit confirmation configuration
    private val exitConfirmationEnabled: Boolean
    private val exitConfirmationTitle: String
    private val exitConfirmationMessage: String
    private val stayButtonLabel: String
    private val leaveButtonLabel: String
    private val closeOnPromotionClick: Boolean
    
    // Promotion labels
    private val promotionBadgeLabel: String
    private val promotionListTitle: String
    private val promotionBadgeFontSize: Float
    
    // Promotion manager
    private var promotionManager: expo.modules.buttonsdk.promotion.PromotionManager? = null
    
    // Picture in picture manager
    private var pictureInPictureManager: PictureInPictureManager? = null
    
    init {
      // Parse exit confirmation config
      val exitConfirmationConfig = options["exitConfirmation"] as? Map<String, Any>
      exitConfirmationEnabled = exitConfirmationConfig?.get("enabled") as? Boolean ?: false
      exitConfirmationTitle = exitConfirmationConfig?.get("title") as? String ?: "Are you sure you want to leave?"
      exitConfirmationMessage = exitConfirmationConfig?.get("message") as? String ?: "You may lose your progress and any available deals."
      stayButtonLabel = exitConfirmationConfig?.get("stayButtonLabel") as? String ?: "Stay"
      leaveButtonLabel = exitConfirmationConfig?.get("leaveButtonLabel") as? String ?: "Leave"
      
      // Parse closeOnPromotionClick option (default: true)
      closeOnPromotionClick = options["closeOnPromotionClick"] as? Boolean ?: true
      
      // Parse promotion label options
      promotionBadgeLabel = options["promotionBadgeLabel"] as? String ?: "Offers"
      promotionListTitle = options["promotionListTitle"] as? String ?: "Available Promotions"
      promotionBadgeFontSize = (options["promotionBadgeFontSize"] as? Number)?.toFloat() ?: 11f
      
      // Initialize promotion manager if promotion data is provided
      val promotionData = options["promotionData"] as? Map<String, Any>
      if (promotionData != null) {
        val currentActivity = activityRef.get()
        if (currentActivity != null) {
          promotionManager = expo.modules.buttonsdk.promotion.PromotionManager(currentActivity, promotionData, { promotionId, browser ->
            Log.d("CustomPurchasePathExtension", "🎯 Received promotion click: $promotionId")
            onPromotionClick(promotionId)
            Log.d("CustomPurchasePathExtension", "📤 Sent promotion event to TypeScript")
            
            if (closeOnPromotionClick) {
              Log.d("CustomPurchasePathExtension", "🚪 Closing browser (closeOnPromotionClick=true)")
              browser?.dismiss()
            } else {
              Log.d("CustomPurchasePathExtension", "🔄 Browser stays open (closeOnPromotionClick=false)")
              // Hide loader since we're not navigating to a new promotion
              GlobalLoaderManager.getInstance().hideLoader()
            }
          }, promotionBadgeLabel, promotionListTitle, promotionBadgeFontSize)
        }
      }
      
      // Initialize picture in picture manager if configured
      val animationConfig = options["animationConfig"] as? Map<String, Any>
      val pipConfig = animationConfig?.get("pictureInPicture") as? Map<String, Any>
      if (pipConfig?.get("enabled") == true) {
        val currentActivity = activityRef.get()
        if (currentActivity != null) {
          pictureInPictureManager = PictureInPictureManager(currentActivity, options)
          currentPictureInPictureManager = pictureInPictureManager // Save reference
        }
      }
    }

    override fun onInitialized(browser: BrowserInterface) {
      // Hide any global loader when new browser initializes
      GlobalLoaderManager.getInstance().hideLoader()
      Log.d("CustomPurchasePathExtension", "🔄 Global loader hidden on browser initialization")
      
      
      // Show pending promo code toast if available (no delay)
      promotionManager?.showPendingPromoCodeToast()
      
      with(browser.header) {
        title.text = options["headerTitle"] as? String ?: ""
        subtitle.text = options["headerSubtitle"] as? String ?: ""

        // Parse colors from hex strings
        title.color = parseColor(options["headerTitleColor"] as? String, Color.WHITE)
        subtitle.color = parseColor(options["headerSubtitleColor"] as? String, Color.WHITE)
        backgroundColor = parseColor(options["headerBackgroundColor"] as? String, Color.BLUE)
        tintColor = parseColor(options["headerTintColor"] as? String, Color.BLUE)
      }

      with(browser.footer) {
        backgroundColor = parseColor(options["footerBackgroundColor"] as? String, Color.BLUE)
        tintColor = parseColor(options["footerTintColor"] as? String, Color.BLUE)
      }
      
      // Check if PiP is active and close it for new purchase path
      pictureInPictureManager?.let { pipManager ->
        if (pipManager.isPipActive()) {
          Log.d("CustomPurchasePathExtension", "PiP is active, closing it for new purchase path")
          pipManager.closePipAndProceed {
            Log.d("CustomPurchasePathExtension", "PiP closed, continuing with browser setup")
          }
        }
      }
      
      // Ensure pictureInPictureManager is created before setting up promotions
      if (pictureInPictureManager == null) {
        val animationConfig = options["animationConfig"] as? Map<String, Any>
        val pipConfig = animationConfig?.get("pictureInPicture") as? Map<String, Any>
        if (pipConfig?.get("enabled") == true) {
          val currentActivity = activityRef.get()
          if (currentActivity != null) {
            pictureInPictureManager = PictureInPictureManager(currentActivity, options)
            currentPictureInPictureManager = pictureInPictureManager // Save reference
            Log.d("CustomPurchasePathExtension", "PictureInPictureManager created in onInitialized")
          }
        }
      }
      
      // Setup promotions badge if available (now includes minimize button)
      Log.d("CustomPurchasePathExtension", "About to setup promotions badge. PiP manager: ${if (pictureInPictureManager != null) "available" else "null"}")
      Log.d("CustomPurchasePathExtension", "PromotionManager: ${if (promotionManager != null) "available" else "null"}")
      
      val promMgr = promotionManager
      val pipMgr = pictureInPictureManager
      
      if (promMgr != null) {
        // If we have a promotion manager, let it handle both buttons
        promMgr.setupPromotionsBadge(browser, pipMgr)
      } else if (pipMgr != null) {
        // If no promotions but PiP is enabled, add just the minimize button
        Log.d("CustomPurchasePathExtension", "No promotions, setting up standalone minimize button")
        val currentActivity = activityRef.get()
        if (currentActivity != null) {
          // Create container to position minimize button on the right
          val rightAlignedContainer = android.widget.LinearLayout(currentActivity).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            layoutParams = android.widget.LinearLayout.LayoutParams(
              android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
              android.widget.LinearLayout.LayoutParams.MATCH_PARENT
            )
          }
          
          val minimizeButton = pipMgr.createMinimizeButton(currentActivity, browser)
          rightAlignedContainer.addView(minimizeButton)
          
          try {
            browser.header.setCustomActionView(rightAlignedContainer)
            Log.d("CustomPurchasePathExtension", "Standalone minimize button set successfully")
          } catch (e: Exception) {
            Log.e("CustomPurchasePathExtension", "Failed to set standalone minimize button", e)
          }
        }
      }
      
      // Setup picture in picture if available
      pictureInPictureManager?.addMinimizeButton(browser)
    }

    // Helper function to parse color from hex string
    private fun parseColor(colorString: String?, defaultColor: Int): Int {
      return if (colorString != null) {
        try {
          Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
          defaultColor
        }
      } else {
        defaultColor
      }
    }

    // Implement other required methods
    override fun onStartNavigate(browser: BrowserInterface) {}
    override fun onPageNavigate(browser: BrowserInterface, page: BrowserPage) {}
    override fun onProductNavigate(browser: BrowserInterface, page: ProductPage) {}
    override fun onPurchaseNavigate(browser: BrowserInterface, page: PurchasePage) {}
    override fun onShouldClose(browserInterface: BrowserInterface): Boolean {
      Log.d("CustomPurchasePathExtension", "onShouldClose called, exitConfirmationEnabled: $exitConfirmationEnabled")
      
      // Check if new purchase path is starting from React Native - ONLY skip modal for SYSTEM closure
      if (isNewPurchasePathStarting) {
        Log.d("CustomPurchasePathExtension", "NEW PURCHASE PATH STARTING - SYSTEM CLOSURE - SKIPPING EXIT MODAL")
        isNewPurchasePathStarting = false // Reset flag immediately after use
        return true // Allow closure without confirmation
      }

      // For USER actions, show modal normally if PiP is active but NOT closing for new content
      val pipManager = pictureInPictureManager ?: currentPictureInPictureManager
      if (pipManager != null && pipManager.isClosingForNewContent()) {
        Log.d("CustomPurchasePathExtension", "PiP is closing for new content (SYSTEM), skipping exit confirmation")
        return true // Allow closure without confirmation
      }
      
      // If PiP is just minimized but user is closing manually, SHOW the modal
      if (pipManager != null && pipManager.isPipActive()) {
        Log.d("CustomPurchasePathExtension", "PiP is active but USER is closing - SHOWING EXIT MODAL")
        // Let it fall through to show modal
      }
      
      if (exitConfirmationEnabled) {
        val currentActivity = activityRef.get()
        if (currentActivity != null) {
          ConfirmationDialog.show(
            currentActivity,
            exitConfirmationTitle,
            exitConfirmationMessage,
            stayButtonLabel,
            leaveButtonLabel,
            browserInterface
          )
          return false // Prevent automatic closure
        }
      }
      return true // Allow closure
    }

    override fun onClosed() {
      // Clean up references to prevent memory leaks
      try {
        Log.d("CustomPurchasePathExtension", "Cleaning up resources")
        promotionManager?.cleanup()
        promotionManager = null
        pictureInPictureManager?.cleanup()
        pictureInPictureManager = null
        GlobalLoaderManager.getInstance().hideLoader()
      } catch (e: Exception) {
        Log.e("CustomPurchasePathExtension", "Error during cleanup", e)
      }
    }
  }

  // Safe URL validation function
  private fun isValidUrlSafe(url: String): Boolean {
    return try {
      // Basic URL validation that allows for edge cases
      when {
        url.isBlank() -> false
        url.startsWith("http://") || url.startsWith("https://") -> {
          // Use URLUtil for more permissive validation
          URLUtil.isValidUrl(url)
        }
        // Allow custom schemes for backwards compatibility
        url.contains("://") -> true
        else -> false
      }
    } catch (e: Exception) {
      Log.w("ButtonSdk", "URL validation failed, allowing for backwards compatibility", e)
      true // Be permissive for backwards compatibility
    }
  }
}
