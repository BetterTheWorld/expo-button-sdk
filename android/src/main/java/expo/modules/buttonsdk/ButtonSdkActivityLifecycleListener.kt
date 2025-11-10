package expo.modules.buttonsdk

import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import expo.modules.core.interfaces.ApplicationLifecycleListener
import android.app.Application
import android.content.Context
import android.os.Bundle

import com.usebutton.sdk.Button
import com.usebutton.sdk.BuildConfig

import expo.modules.core.interfaces.ReactActivityLifecycleListener

class ButtonSdkActivityLifecycleListener(activityContext: Context) : ReactActivityLifecycleListener {
    override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        // Debugging enabled (do not include in production)
        if (BuildConfig.DEBUG) {
            Button.debug().isLoggingEnabled = true
        }

        // Attempt to fetch the Button SDK App ID from AndroidManifest.xml
        val appId = getAppIdFromManifest(activity) // Use application context here

        // Configure the Button SDK with the retrieved App ID
        appId?.let {
            Button.configure(activity, it) // Assuming Button.configure can accept Context
            Log.i("ButtonSdkActivityLifecycleListener", "Button SDK configured with App ID: $it")
        } ?: Log.e("ButtonSdkActivityLifecycleListener", "Button SDK App ID not found in AndroidManifest.xml")
    }

    private fun getAppIdFromManifest(context: Context): String? {
        try {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val bundle = applicationInfo.metaData
            return bundle.getString("com.usebutton.sdk.ApplicationId")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("ButtonSdkActivityLifecycleListener", "Failed to load meta-data, NameNotFound: ${e.message}")
        } catch (e: NullPointerException) {
            Log.e("ButtonSdkActivityLifecycleListener", "Failed to load meta-data, NullPointer: ${e.message}")
        }
        return null
    }

    override fun onDestroy(activity: Activity) {
        // Clean up any global state when activity is destroyed
        try {
            GlobalLoaderManager.getInstance().hideLoader()
            Log.d("ButtonSdkActivityLifecycleListener", "Activity destroyed, cleaned up global state")
        } catch (e: Exception) {
            Log.e("ButtonSdkActivityLifecycleListener", "Error cleaning up on activity destroy", e)
        }
    }

    override fun onPause(activity: Activity) {
        // Hide loader when activity is paused to prevent stale loaders
        try {
            GlobalLoaderManager.getInstance().hideLoader()
        } catch (e: Exception) {
            Log.e("ButtonSdkActivityLifecycleListener", "Error hiding loader on pause", e)
        }
    }
}