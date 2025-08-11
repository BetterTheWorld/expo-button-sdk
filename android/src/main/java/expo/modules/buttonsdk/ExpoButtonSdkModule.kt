package expo.modules.buttonsdk

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.graphics.Color
import com.usebutton.sdk.Button
import com.usebutton.sdk.purchasepath.*

class ExpoButtonSdkModule() : Module() {

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

    AsyncFunction("startPurchasePath") { params: Map<String, Any>, promise: Promise ->
      val url = params["url"] as? String ?: ""
      val token = params["token"] as? String

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
                val eventBody = Bundle().apply {
                  putString("promotionId", promotionId)
                }
                sendEvent("onPromotionClick", eventBody)
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
    private val activity: Activity,
    private val options: Map<String, Any>,
    private val onPromotionClick: (String) -> Unit
  ) : PurchasePathExtension {

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
    private var promotionManager: PromotionManager? = null
    
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
        promotionManager = PromotionManager(activity, promotionData, { promotionId, browser ->
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
      
      // Setup promotions badge if available
      promotionManager?.setupPromotionsBadge(browser)
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
      
      if (exitConfirmationEnabled) {
        ConfirmationDialog.show(
          activity,
          exitConfirmationTitle,
          exitConfirmationMessage,
          stayButtonLabel,
          leaveButtonLabel,
          browserInterface
        )
        return false // Prevent automatic closure
      }
      return true // Allow closure
    }

    override fun onClosed() {}
  }
}
