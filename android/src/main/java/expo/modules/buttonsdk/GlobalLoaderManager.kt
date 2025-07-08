package expo.modules.buttonsdk

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout

class GlobalLoaderManager private constructor() {
    
    companion object {
        @Volatile
        private var INSTANCE: GlobalLoaderManager? = null
        
        fun getInstance(): GlobalLoaderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlobalLoaderManager().also { INSTANCE = it }
            }
        }
    }
    
    private var currentLoaderView: View? = null
    private var currentActivity: Activity? = null
    
    fun showLoader(activity: Activity, message: String = "Loading promotion...") {
        try {
            hideLoader() // Remove any existing loader first
            
            currentActivity = activity
            
            // Get the root view of the activity (this will be above everything including WebView)
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            
            // Create loader overlay
            val loaderOverlay = createLoaderView(activity, message)
            currentLoaderView = loaderOverlay
            
            // Add to root view so it appears above everything
            rootView.addView(loaderOverlay)
            
            android.util.Log.d("GlobalLoaderManager", "✅ Loader shown over root view")
            
        } catch (e: Exception) {
            android.util.Log.e("GlobalLoaderManager", "❌ Error showing loader", e)
        }
    }
    
    fun hideLoader() {
        try {
            currentLoaderView?.let { loader ->
                currentActivity?.let { activity ->
                    val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
                    rootView.removeView(loader)
                    android.util.Log.d("GlobalLoaderManager", "✅ Loader hidden")
                }
            }
            currentLoaderView = null
        } catch (e: Exception) {
            android.util.Log.e("GlobalLoaderManager", "❌ Error hiding loader", e)
        }
    }
    
    private fun createLoaderView(activity: Activity, message: String): View {
        // Semi-transparent background overlay that covers everything
        val overlay = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
            isClickable = true // Block touches to underlying views
            isFocusable = true
        }
        
        // Loader container
        val loaderContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(64, 48, 64, 48) // Increased horizontal and vertical padding
            
            // Center in screen with margin from edges
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(32, 32, 32, 32) // Add margin from screen edges
            }
            layoutParams = params
            
            // Rounded corners effect
            background = activity.getDrawable(android.R.drawable.dialog_holo_light_frame)
            elevation = 8f // Add elevation for better shadow
        }
        
        // Progress bar
        val progressBar = ProgressBar(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 32 // Increased spacing below progress bar
            }
        }
        
        // Loading text
        val loadingText = TextView(activity).apply {
            text = message
            textSize = 16f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            setPadding(16, 0, 16, 0) // Add horizontal padding to text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        loaderContainer.addView(progressBar)
        loaderContainer.addView(loadingText)
        overlay.addView(loaderContainer)
        
        return overlay
    }
}