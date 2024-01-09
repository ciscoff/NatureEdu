package dev.barabu.base.extentions

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size
import android.view.WindowInsets

val Context.isActualGlEsSupporting: Boolean
    get() = with(getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager) {
        deviceConfigurationInfo.reqGlEsVersion >= 0x30000 ||
                (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86"));
    }

val Context.screenSize: Size?
    get() {
        val wm = (this as? Activity)?.windowManager ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            with(wm.currentWindowMetrics) {
                val insets =
                    windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
                Size(
                    bounds.width() - (insets.left + insets.right),
                    bounds.height() - (insets.top + insets.bottom)
                )
            }

        } else {
            with(DisplayMetrics()) {
                wm.defaultDisplay.getMetrics(this)
                Size(widthPixels, heightPixels)
            }
        }
    }