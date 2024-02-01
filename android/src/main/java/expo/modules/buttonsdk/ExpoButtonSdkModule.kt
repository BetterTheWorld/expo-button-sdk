package expo.modules.buttonsdk

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.Promise

import android.util.Log
import android.graphics.Color

import com.usebutton.sdk.Button
import com.usebutton.sdk.purchasepath.*

class ExpoButtonSdkModule() : Module() {
  override fun definition() = ModuleDefinition {

    Name("ExpoButtonSdk")

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
            Button.purchasePath().extension = CustomPurchasePathExtension(params)
            var activity = appContext.activityProvider?.currentActivity

            if (activity != null) {
              purchasePath.start(activity) // Correct context access
            } else {
              promise.reject("ERROR", "Not context for purchasePath", throwable)
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

  class CustomPurchasePathExtension(private val options: Map<String, Any>) : PurchasePathExtension {

    override fun onInitialized(browser: BrowserInterface) {
      with(browser.header) {
        title.text = options["headerTitle"] as? String ?: ""
        subtitle.text = options["headerSubtitle"] as? String ?: ""

        // Parse colors from hex strings
        title.color = parseColor(options["headerTitleColor"] as? String, Color.WHITE) // Default to WHITE if not specified or parsing fails
        subtitle.color = parseColor(options["headerSubtitleColor"] as? String, Color.WHITE)
        backgroundColor = parseColor(options["headerBackgroundColor"] as? String, Color.BLUE)
        tintColor = parseColor(options["headerTintColor"] as? String, Color.BLUE)
      }

      with(browser.footer) {
        backgroundColor = parseColor(options["footerBackgroundColor"] as? String, Color.BLUE)
        tintColor = parseColor(options["footerTintColor"] as? String, Color.BLUE)
      }
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

    // Implement other required methods with empty bodies or as needed
    override fun onStartNavigate(browser: BrowserInterface) {}
    override fun onPageNavigate(browser: BrowserInterface, page: BrowserPage) {}
    override fun onProductNavigate(browser: BrowserInterface, page: ProductPage) {}
    override fun onPurchaseNavigate(browser: BrowserInterface, page: PurchasePage) {}
    override fun onClosed() {}
  }
}
