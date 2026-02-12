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
    private const val DIALOG_TAG = "exit_confirmation_dialog"

    fun show(
        context: Context,
        title: String,
        message: String,
        stayButtonLabel: String,
        leaveButtonLabel: String,
        browser: BrowserInterface,
        titleColor: Int? = null,
        stayButtonTextColor: Int? = null,
        stayButtonBackgroundColor: Int? = null,
        leaveButtonTextColor: Int? = null,
        leaveButtonBackgroundColor: Int? = null,
        buttonBorderColor: Int? = null,
        fontFamily: String? = null,
        messageColor: Int? = null,
        titleFontSize: Float? = null,
        messageFontSize: Float? = null,
        buttonFontSize: Float? = null
    ) {

        // Use BrowserInterface's view container instead of AlertDialog
        Handler(Looper.getMainLooper()).post {
            try {
                val container = browser.viewContainer

                if (container == null) {
                    Log.e("ConfirmationDialog", "Could not get view container from browser")
                    return@post
                }

                // Prevent stacking — if a dialog is already showing, ignore
                if (container.findViewWithTag<View>(DIALOG_TAG) != null) {
                    Log.d("ConfirmationDialog", "Dialog already visible, ignoring duplicate")
                    return@post
                }

                val dialogView = createDialogView(
                    context, title, message, stayButtonLabel, leaveButtonLabel,
                    browser, container,
                    titleColor, stayButtonTextColor, stayButtonBackgroundColor,
                    leaveButtonTextColor, leaveButtonBackgroundColor,
                    buttonBorderColor, fontFamily,
                    messageColor, titleFontSize, messageFontSize, buttonFontSize
                )
                dialogView.tag = DIALOG_TAG

                // Add to container
                container.addView(dialogView)
                Log.d("ConfirmationDialog", "Dialog overlay added successfully")

            } catch (e: Exception) {
                Log.e("ConfirmationDialog", "Error creating dialog overlay", e)
            }
        }
    }

    private fun loadTypeface(context: Context, fontFamily: String?): Typeface? {
        if (fontFamily == null) return null
        val paths = listOf(
            "fonts/$fontFamily.ttf",   // react-native asset linking
            "fonts/$fontFamily.otf",
            "$fontFamily.ttf",         // expo-font plugin (root of assets)
            "$fontFamily.otf"
        )
        for (path in paths) {
            try {
                return Typeface.createFromAsset(context.assets, path)
            } catch (_: Exception) { }
        }
        Log.w("ConfirmationDialog", "Could not load font: $fontFamily")
        return null
    }

    private fun createDialogView(
        context: Context,
        title: String,
        message: String,
        stayButtonLabel: String,
        leaveButtonLabel: String,
        browser: BrowserInterface,
        container: ViewGroup,
        titleColor: Int?,
        stayButtonTextColor: Int?,
        stayButtonBackgroundColor: Int?,
        leaveButtonTextColor: Int?,
        leaveButtonBackgroundColor: Int?,
        buttonBorderColor: Int?,
        fontFamily: String?,
        messageColor: Int?,
        titleFontSize: Float?,
        messageFontSize: Float?,
        buttonFontSize: Float?
    ): View {
        val density = context.resources.displayMetrics.density
        val customTypeface = loadTypeface(context, fontFamily)

        // Resolve colors with defaults
        val resolvedTitleColor = titleColor ?: Color.parseColor("#1C1C1C")
        val resolvedMessageColor = messageColor ?: Color.parseColor("#282B30")
        val resolvedTitleFontSize = titleFontSize ?: 20f
        val resolvedMessageFontSize = messageFontSize ?: 14f
        val resolvedButtonFontSize = buttonFontSize ?: 12f
        val resolvedStayTextColor = stayButtonTextColor ?: Color.WHITE
        val resolvedStayBgColor = stayButtonBackgroundColor ?: Color.parseColor("#074a7b")
        val resolvedLeaveTextColor = leaveButtonTextColor ?: Color.parseColor("#677080")
        val resolvedLeaveBgColor = leaveButtonBackgroundColor ?: Color.WHITE
        val resolvedBorderColor = buttonBorderColor ?: Color.parseColor("#D3D9E0")

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
                val marginPx = (30 * density).toInt()
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedTitleFontSize)
            setTextColor(resolvedTitleColor)
            gravity = Gravity.CENTER
            typeface = customTypeface ?: Typeface.DEFAULT_BOLD
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
            setTextSize(TypedValue.COMPLEX_UNIT_SP, resolvedMessageFontSize)
            setTextColor(resolvedMessageColor)
            gravity = Gravity.CENTER
            if (customTypeface != null) typeface = customTypeface
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
        val stayButton = createStyledButton(
            context, stayButtonLabel,
            resolvedStayTextColor, resolvedStayBgColor, resolvedBorderColor,
            customTypeface, resolvedButtonFontSize
        ) {
            Log.d("ConfirmationDialog", "User chose to stay")
            container.removeView(dialogContainer)
        }

        // Leave button
        val leaveButton = createStyledButton(
            context, leaveButtonLabel,
            resolvedLeaveTextColor, resolvedLeaveBgColor, resolvedBorderColor,
            customTypeface, resolvedButtonFontSize
        ) {
            Log.d("ConfirmationDialog", "User chose to leave")
            container.removeView(dialogContainer)
            browser.dismiss()
        }

        // Button spacing — leave first (left), stay second (right)
        val spacingPx = (8 * density).toInt()
        leaveButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginEnd = spacingPx / 2
        }

        stayButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ).apply {
            marginStart = spacingPx / 2
        }

        // Assemble modal
        titleSection.addView(titleView)
        titleSection.addView(messageView)

        buttonsContainer.addView(leaveButton)
        buttonsContainer.addView(stayButton)

        modalContainer.addView(titleSection)
        modalContainer.addView(buttonsContainer)

        dialogContainer.addView(modalContainer)

        return dialogContainer
    }

    private fun createStyledButton(
        context: Context,
        text: String,
        textColor: Int,
        bgColor: Int,
        borderColor: Int,
        customTypeface: Typeface?,
        fontSize: Float,
        onClick: () -> Unit
    ): Button {
        val density = context.resources.displayMetrics.density

        return Button(context).apply {
            this.text = text
            isAllCaps = false
            transformationMethod = null
            setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
            typeface = customTypeface ?: Typeface.DEFAULT

            val background = GradientDrawable().apply {
                cornerRadius = 20 * density
                setColor(bgColor)
                setStroke((2.5f * density).toInt(), borderColor)
            }
            setBackground(background)

            setTextColor(textColor)

            val verticalPaddingPx = (8 * density).toInt()
            val horizontalPaddingPx = (12 * density).toInt()
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
