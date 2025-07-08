package expo.modules.buttonsdk

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
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
        
        // Loader container with custom rounded background
        val loaderContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(80, 60, 80, 60) // More generous padding
            
            // Create custom rounded background
            val roundedBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadius = 24f // Rounded corners
                // Add subtle shadow effect
                setStroke(1, Color.parseColor("#E0E0E0"))
            }
            background = roundedBackground
            
            // Center in screen with margin from edges
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(48, 48, 48, 48) // Larger margin from screen edges
            }
            layoutParams = params
            
            elevation = 12f // Higher elevation for better shadow
        }
        
        // Progress bar with better styling
        val progressBar = ProgressBar(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 40 // More spacing below progress bar
            }
            // Make progress bar slightly larger
            scaleX = 1.2f
            scaleY = 1.2f
        }
        
        // Loading text with better typography
        val loadingText = TextView(activity).apply {
            text = message
            textSize = 17f // Slightly larger text
            setTextColor(Color.parseColor("#2C2C2C")) // Darker, more readable color
            gravity = Gravity.CENTER
            setPadding(24, 0, 24, 0) // More horizontal padding
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Better font weight
            setTypeface(typeface, android.graphics.Typeface.NORMAL)
        }
        
        loaderContainer.addView(progressBar)
        loaderContainer.addView(loadingText)
        overlay.addView(loaderContainer)
        
        return overlay
    }
}