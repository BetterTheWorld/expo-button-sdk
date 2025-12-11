package expo.modules.buttonsdk.events

interface ScrollVisibilityObserver {
    val observerId: String
        get() = "${this::class.java.simpleName}_${System.identityHashCode(this)}"
    
    fun onScrollVisibilityChanged(event: ScrollVisibilityEvent)
}