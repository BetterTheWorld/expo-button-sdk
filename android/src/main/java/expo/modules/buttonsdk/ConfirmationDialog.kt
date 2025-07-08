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
        // Create semi-transparent background
        val dialogContainer = RelativeLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Semi-transparent black
        }
        
        // Create dialog content
        val dialogContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(60, 40, 60, 40)
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
        }
        
        // Title
        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }
        
        // Message
        val messageView = TextView(context).apply {
            text = message
            textSize = 14f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        
        // Buttons container
        val buttonsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        // Stay button
        val stayButton = Button(context).apply {
            text = stayButtonLabel
            setOnClickListener {
                Log.d("ConfirmationDialog", "User chose to stay")
                container.removeView(dialogContainer)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 20
            }
        }
        
        // Leave button
        val leaveButton = Button(context).apply {
            text = leaveButtonLabel
            setOnClickListener {
                Log.d("ConfirmationDialog", "User chose to leave")
                container.removeView(dialogContainer)
                browser.dismiss()
            }
        }
        
        // Assemble the dialog
        buttonsContainer.addView(stayButton)
        buttonsContainer.addView(leaveButton)
        
        dialogContent.addView(titleView)
        dialogContent.addView(messageView)
        dialogContent.addView(buttonsContainer)
        
        dialogContainer.addView(dialogContent)
        
        return dialogContainer
    }
}
