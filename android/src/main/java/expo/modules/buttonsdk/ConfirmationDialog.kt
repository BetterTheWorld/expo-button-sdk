package expo.modules.buttonsdk

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.widget.RelativeLayout
import android.graphics.drawable.GradientDrawable
import android.graphics.Typeface
import android.util.TypedValue

import com.usebutton.sdk.purchasepath.BrowserInterface

object ConfirmationDialog {
    fun showExitConfirmationAlert(
        context: Context,
        browser: BrowserInterface,
        title: String?,
        message: String?,
        stayButtonLabel: String?,
        leaveButtonLabel: String?,
        callback: (Boolean) -> Unit
    ) {
        show(
            context,
            title ?: "Are you sure you want to leave?",
            message ?: "You may lose your progress and any available deals.",
            stayButtonLabel ?: "Stay",
            leaveButtonLabel ?: "Leave",
            browser
        )
    }
    
    fun show(
        context: Context,
        title: String,
        message: String,
        stayButtonLabel: String,
        leaveButtonLabel: String,
        browser: BrowserInterface
    ) {

        // Use BrowserInterface's view container instead of AlertDialog
        Handler(Looper.getMainLooper()).post {
            try {
                val container = browser.viewContainer
                
                if (container == null) {
                    Log.e("ConfirmationDialog", "Could not get view container from browser")
                    return@post
                }

                val dialogView = createDialogView(context, title, message, stayButtonLabel, leaveButtonLabel, browser, container)
                
                // Add to container
                container.addView(dialogView)
                Log.d("ConfirmationDialog", "Dialog overlay added successfully")
                
            } catch (e: Exception) {
                Log.e("ConfirmationDialog", "Error creating dialog overlay", e)
            }
        }
    }
    
    private fun createDialogView(
        context: Context,
        title: String,
        message: String,
        stayButtonLabel: String,
        leaveButtonLabel: String,
        browser: BrowserInterface,
        container: ViewGroup
    ): View {
        val density = context.resources.displayMetrics.density
        
        // Background overlay
        val dialogContainer = RelativeLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000"))
            
            // Background tap to dismiss
            setOnClickListener {
                Log.d("ConfirmationDialog", "Background tapped - user chose to stay")
                container.removeView(this)
            }
        }
        
        // Modal container with rounded corners and shadow
        val modalContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            
            // Modal background
            val background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 12 * density
            }
            setBackground(background)
            
            // More padding (32dp equivalent)
            val paddingPx = (32 * density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
                // Add margins so modal doesn't touch screen edges
                val marginPx = (20 * density).toInt()
                setMargins(marginPx, marginPx, marginPx, marginPx)
            }
            
            // Consume clicks
            setOnClickListener { /* consume click */ }
        }
        
        // Title section container
        val titleSection = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val bottomMarginPx = (24 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, bottomMarginPx)
            }
        }
        
        // Main title (bold, centered, larger)
        val titleView = TextView(context).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(Color.parseColor("#1C1C1C"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            val bottomMarginPx = (16 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, bottomMarginPx)
            }
        }
        
        // Subtitle (smaller, darker gray)
        val messageView = TextView(context).apply {
            text = message
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(Color.parseColor("#282B30"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Button container (horizontal, equal width)
        val buttonsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Stay button
        val stayButton = createStyledButton(context, stayButtonLabel, true) {
            Log.d("ConfirmationDialog", "User chose to stay")
            container.removeView(dialogContainer)
        }
        
        // Leave button
        val leaveButton = createStyledButton(context, leaveButtonLabel, false) {
            Log.d("ConfirmationDialog", "User chose to leave")
            container.removeView(dialogContainer)
            browser.dismiss()
        }
        
        // Button spacing
        val spacingPx = (8 * density).toInt()
        stayButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginEnd = spacingPx / 2
        }
        
        leaveButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginStart = spacingPx / 2
        }
        
        // Assemble modal
        titleSection.addView(titleView)
        titleSection.addView(messageView)
        
        buttonsContainer.addView(stayButton)
        buttonsContainer.addView(leaveButton)
        
        modalContainer.addView(titleSection)
        modalContainer.addView(buttonsContainer)
        
        dialogContainer.addView(modalContainer)
        
        return dialogContainer
    }
    
    private fun createStyledButton(
        context: Context,
        text: String,
        isStayButton: Boolean,
        onClick: () -> Unit
    ): Button {
        val density = context.resources.displayMetrics.density
        
        return Button(context).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT
            
            val background = GradientDrawable().apply {
                cornerRadius = 8 * density
                if (isStayButton) {
                    setColor(Color.parseColor("#074a7b"))
                } else {
                    setColor(Color.WHITE)
                    setStroke((1 * density).toInt(), Color.parseColor("#D3D9E0"))
                }
            }
            setBackground(background)
            
            if (isStayButton) {
                setTextColor(Color.WHITE)
            } else {
                setTextColor(Color.parseColor("#677080"))
            }
            
            val verticalPaddingPx = (2 * density).toInt()
            val horizontalPaddingPx = (4 * density).toInt()
            setPadding(horizontalPaddingPx, verticalPaddingPx, horizontalPaddingPx, verticalPaddingPx)
            
            // Override minimum dimensions
            minHeight = (36 * density).toInt()
            minimumHeight = (36 * density).toInt()
            
            setOnClickListener { onClick() }
            
            // Remove default styling
            stateListAnimator = null
            elevation = 0f
        }
    }
}
