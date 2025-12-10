package expo.modules.buttonsdk.events

import android.graphics.Rect

data class ScrollVisibilityEvent(
    val timestamp: Long,
    val source: ScrollEventSource,
    val shouldShow: Boolean,
    val reason: String,
    val triggerFrame: Rect? = null,
    val safeAreaInsets: Rect = Rect(),
    val hasNotch: Boolean = false
)

enum class ScrollEventSource {
    WEB_VIEW,
    DISPLAY_LINK,
    KEY_VALUE_OBSERVER,
    USER_TRIGGERED,
    ORIENTATION_CHANGE,
    KEYBOARD_SHOW,
    KEYBOARD_HIDE
}