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

  companion object {
    private var currentPictureInPictureManager: PictureInPictureManager? = null
    private var isNewPurchasePathStarting = false

    // Persistent simulated PiP state (survives browser close)
    var activeSimPipManager: SimulatedPipManager? = null
    var isSimPipDismissing = false
    private var simPipReopenFn: (() -> Unit)? = null

    fun reopenFromSimPip() {
      activeSimPipManager = null
      isSimPipDismissing = false
      val reopenFn = simPipReopenFn
      simPipReopenFn = null
      reopenFn?.invoke() // invoke AFTER nulling so launchPurchasePath can set the next one
    }

    fun cleanupSimPip() {
      activeSimPipManager?.cleanup()
      activeSimPipManager = null
      simPipReopenFn = null
      isSimPipDismissing = false
    }
  }

  override fun definition() = ModuleDefinition {
    Name("ExpoButtonSdk")

    Events("onPromotionClick", "onHeaderButtonClick", "onClose")

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

      // Clean up any persistent simulated PiP
      cleanupSimPip()

      // URL validation
      if (url.isBlank()) {
        promise.reject("INVALID_URL", "URL cannot be blank", null)
        return@AsyncFunction
      }

      if (!isValidUrlSafe(url)) {
        Log.w("ButtonSdk", "URL might be invalid but proceeding for backwards compatibility: $url")
      }

      launchPurchasePath(params)
      promise.resolve(null)
    }
  }

  private fun launchPurchasePath(params: Map<String, Any>) {
    // Store reopen function for simulated PiP (captures params for re-launch)
    simPipReopenFn = { launchPurchasePath(params) }

    val url = params["url"] as? String ?: return
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
            Button.purchasePath().extension = CustomPurchasePathExtension(activity, params, { promotionId ->
              try {
                val eventBody = Bundle().apply {
                  putString("promotionId", promotionId)
                }
                sendEvent("onPromotionClick", eventBody)
              } catch (e: Exception) {
                Log.e("ButtonSdk", "Error sending promotion click event", e)
              }
            }, {
              try {
                sendEvent("onClose", Bundle())
              } catch (e: Exception) {
                Log.e("ButtonSdk", "Error sending close event", e)
              }
            })
            purchasePath.start(activity)
          }
        } else {
          Log.e("ButtonSdk", "Error fetching purchasePath", throwable)
        }
      }
    })
  }

  class CustomPurchasePathExtension(
    activity: Activity,
    private val options: Map<String, Any>,
    private val onPromotionClick: (String) -> Unit,
    private val onClose: () -> Unit
  ) : PurchasePathExtension {

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
      val exitConfirmationConfig = options["exitConfirmation"] as? Map<String, Any>
      exitConfirmationEnabled = exitConfirmationConfig?.get("enabled") as? Boolean ?: false
      exitConfirmationTitle = exitConfirmationConfig?.get("title") as? String ?: "Are you sure you want to leave?"
      exitConfirmationMessage = exitConfirmationConfig?.get("message") as? String ?: "You may lose your progress and any available deals."
      stayButtonLabel = exitConfirmationConfig?.get("stayButtonLabel") as? String ?: "Stay"
      leaveButtonLabel = exitConfirmationConfig?.get("leaveButtonLabel") as? String ?: "Leave"

      closeOnPromotionClick = options["closeOnPromotionClick"] as? Boolean ?: true

      promotionBadgeLabel = options["promotionBadgeLabel"] as? String ?: "Offers"
      promotionListTitle = options["promotionListTitle"] as? String ?: "Available Promotions"
      promotionBadgeFontSize = (options["promotionBadgeFontSize"] as? Number)?.toFloat() ?: 11f

      // Initialize promotion manager if promotion data is provided
      val promotionData = options["promotionData"] as? Map<String, Any>
      if (promotionData != null) {
        val currentActivity = activityRef.get()
        if (currentActivity != null) {
          promotionManager = expo.modules.buttonsdk.promotion.PromotionManager(currentActivity, promotionData, { promotionId, browser ->
            Log.d("CustomPurchasePathExtension", "Received promotion click: $promotionId")
            onPromotionClick(promotionId)

            if (closeOnPromotionClick) {
              browser?.dismiss()
            } else {
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
          currentPictureInPictureManager = pictureInPictureManager
        }
      }
    }

    override fun onInitialized(browser: BrowserInterface) {
      GlobalLoaderManager.getInstance().hideLoader()

      promotionManager?.showPendingPromoCodeToast()

      with(browser.header) {
        title.text = options["headerTitle"] as? String ?: ""
        subtitle.text = options["headerSubtitle"] as? String ?: ""
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
          pipManager.closePipAndProceed {}
        }
      }

      // Ensure pictureInPictureManager is created
      if (pictureInPictureManager == null) {
        val animationConfig = options["animationConfig"] as? Map<String, Any>
        val pipConfig = animationConfig?.get("pictureInPicture") as? Map<String, Any>
        if (pipConfig?.get("enabled") == true) {
          val currentActivity = activityRef.get()
          if (currentActivity != null) {
            pictureInPictureManager = PictureInPictureManager(currentActivity, options)
            currentPictureInPictureManager = pictureInPictureManager
          }
        }
      }

      // Setup promotions badge and/or minimize button
      val promMgr = promotionManager
      val pipMgr = pictureInPictureManager

      if (promMgr != null) {
        promMgr.setupPromotionsBadge(browser, pipMgr)
      } else if (pipMgr != null) {
        val currentActivity = activityRef.get()
        if (currentActivity != null) {
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
          } catch (e: Exception) {
            Log.e("CustomPurchasePathExtension", "Failed to set standalone minimize button", e)
          }
        }
      }

      pictureInPictureManager?.addMinimizeButton(browser)
    }

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

    override fun onStartNavigate(browser: BrowserInterface) {}
    override fun onPageNavigate(browser: BrowserInterface, page: BrowserPage) {}
    override fun onProductNavigate(browser: BrowserInterface, page: ProductPage) {}
    override fun onPurchaseNavigate(browser: BrowserInterface, page: PurchasePage) {}

    override fun onShouldClose(browserInterface: BrowserInterface): Boolean {
      Log.d("CustomPurchasePathExtension", "onShouldClose called")

      // Simulated PiP is dismissing the browser - allow it silently
      if (isSimPipDismissing) {
        Log.d("CustomPurchasePathExtension", "Simulated PiP dismissing - allowing close")
        return true
      }

      // New purchase path starting - allow system closure
      if (isNewPurchasePathStarting) {
        Log.d("CustomPurchasePathExtension", "New purchase path starting - allowing close")
        isNewPurchasePathStarting = false
        return true
      }

      // PiP closing for new content
      val pipManager = pictureInPictureManager ?: currentPictureInPictureManager
      if (pipManager != null && pipManager.isClosingForNewContent()) {
        return true
      }

      // Show exit confirmation if enabled
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
          return false
        }
      }
      return true
    }

    override fun onClosed() {
      try {
        Log.d("CustomPurchasePathExtension", "onClosed called, isSimPipDismissing=$isSimPipDismissing")

        // Clean up promotions
        promotionManager?.cleanup()
        promotionManager = null

        if (isSimPipDismissing) {
          // Browser was dismissed for simulated PiP - DON'T send close event
          // DON'T clean up the persistent sim pip manager (it's in the module companion)
          // Only clean up non-pip stuff
          pictureInPictureManager?.cleanup()
          pictureInPictureManager = null
          GlobalLoaderManager.getInstance().hideLoader()
          isSimPipDismissing = false
          Log.d("CustomPurchasePathExtension", "Sim PiP dismiss cleanup done (no close event sent)")
        } else {
          // Normal close - clean up everything
          pictureInPictureManager?.cleanup()
          pictureInPictureManager = null
          cleanupSimPip() // Also clean up any lingering sim pip
          GlobalLoaderManager.getInstance().hideLoader()
          onClose()
        }
      } catch (e: Exception) {
        Log.e("CustomPurchasePathExtension", "Error during cleanup", e)
      }
    }
  }

  private fun isValidUrlSafe(url: String): Boolean {
    return try {
      when {
        url.isBlank() -> false
        url.startsWith("http://") || url.startsWith("https://") -> URLUtil.isValidUrl(url)
        url.contains("://") -> true
        else -> false
      }
    } catch (e: Exception) {
      true
    }
  }
}
