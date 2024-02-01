package expo.modules.buttonsdk

import android.content.Context
import expo.modules.core.interfaces.Package

import expo.modules.core.interfaces.ReactActivityLifecycleListener

class ExpoButtonSdkPackage : Package {
    override fun createReactActivityLifecycleListeners(activityContext: Context): List<ReactActivityLifecycleListener> {
        return listOf(ButtonSdkActivityLifecycleListener(activityContext))
    }
}