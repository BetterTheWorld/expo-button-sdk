package expo.modules.buttonsdk

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import expo.modules.core.interfaces.ApplicationLifecycleListener
import android.content.Context
import android.os.Bundle

import com.usebutton.sdk.Button
import com.usebutton.sdk.BuildConfig

import expo.modules.core.interfaces.ReactActivityLifecycleListener

class ButtonSdkActivityLifecycleListener(activityContext: Context) : ReactActivityLifecycleListener {

    private var activityDiagnosticCallbacks: Application.ActivityLifecycleCallbacks? = null

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

        // DIAGNOSTIC: Register global activity lifecycle monitor
        if (activityDiagnosticCallbacks == null) {
            activityDiagnosticCallbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(a: Activity, b: Bundle?) {
                    Log.w("LIFECYCLE-DIAG", "🟢 CREATED: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
                override fun onActivityStarted(a: Activity) {
                    Log.w("LIFECYCLE-DIAG", "🔵 STARTED: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
                override fun onActivityResumed(a: Activity) {
                    Log.w("LIFECYCLE-DIAG", "🟣 RESUMED: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
                override fun onActivityPaused(a: Activity) {
                    Log.w("LIFECYCLE-DIAG", "🟡 PAUSED: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
                override fun onActivityStopped(a: Activity) {
                    Log.w("LIFECYCLE-DIAG", "🟠 STOPPED: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
                override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {
                    Log.w("LIFECYCLE-DIAG", "💾 SAVE_STATE: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
                override fun onActivityDestroyed(a: Activity) {
                    Log.w("LIFECYCLE-DIAG", "🔴 DESTROYED: ${a.javaClass.simpleName} finishing=${a.isFinishing}")
                }
            }
            activity.application.registerActivityLifecycleCallbacks(activityDiagnosticCallbacks)
            Log.w("LIFECYCLE-DIAG", "📊 Diagnostic lifecycle monitor registered")

            // Check ALWAYS_FINISH_ACTIVITIES
            try {
                val alwaysFinish = Settings.Global.getInt(
                    activity.contentResolver,
                    Settings.Global.ALWAYS_FINISH_ACTIVITIES,
                    0
                )
                Log.w("LIFECYCLE-DIAG", "⚠️ ALWAYS_FINISH_ACTIVITIES = $alwaysFinish (1 means 'Don't keep activities' is ON)")
            } catch (e: Exception) {
                Log.w("LIFECYCLE-DIAG", "Could not check ALWAYS_FINISH_ACTIVITIES", e)
            }
        }
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
