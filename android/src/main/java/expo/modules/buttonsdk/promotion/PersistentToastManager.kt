package expo.modules.buttonsdk.promotion

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object PersistentToastManager {
    
    fun showToast(activity: Activity, promoCode: String) {
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (20 * activity.resources.displayMetrics.density).toInt(),
                (16 * activity.resources.displayMetrics.density).toInt(),
                (20 * activity.resources.displayMetrics.density).toInt(),
                (16 * activity.resources.displayMetrics.density).toInt()
            )
            
            val backgroundDrawable = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#CC000000"))
                cornerRadius = 12f * activity.resources.displayMetrics.density
            }
            background = backgroundDrawable
        }
        
        val textView = TextView(activity).apply {
            text = "✓ $promoCode copied"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        
        layout.addView(textView)

        val toast = Toast(activity).apply {
            duration = Toast.LENGTH_LONG
            view = layout
            
            setGravity(
                Gravity.BOTTOM or Gravity.RIGHT,
                (20 * activity.resources.displayMetrics.density).toInt(),
                (60 * activity.resources.displayMetrics.density).toInt()
            )
        }
        toast.show()
    }
}