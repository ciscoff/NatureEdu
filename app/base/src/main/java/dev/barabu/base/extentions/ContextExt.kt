package dev.barabu.base.extentions

import android.app.ActivityManager
import android.content.Context
import android.os.Build

val Context.isActualGlEsSupporting: Boolean
    get() = with(getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager) {
        deviceConfigurationInfo.reqGlEsVersion >= 0x30000 ||
                (Build.FINGERPRINT.startsWith("generic")
                        || Build.FINGERPRINT.startsWith("unknown")
                        || Build.MODEL.contains("google_sdk")
                        || Build.MODEL.contains("Emulator")
                        || Build.MODEL.contains("Android SDK built for x86"));
    }