package dev.barabu.nature.permission

import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dev.barabu.nature.R
import java.lang.ref.WeakReference

class PermissionManager private constructor(private val componentActivity: WeakReference<ComponentActivity>) {

    private val requiredPermissions = mutableListOf<Permission>()
    private var rationale: String? = null
    private var detailedCallback: (Map<Permission, Boolean>) -> Unit = {}
    private var callback: (Boolean) -> Unit = {}

    private val activityResultLauncher =
        componentActivity.get()
            ?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
                sendResultAndCleanUp(grantResults)
            }

    // 1. Перечень (vararg) требуемых разрешений
    fun require(vararg permission: Permission): PermissionManager {
        requiredPermissions.addAll(permission)
        return this
    }

    // 2. Обоснование предоставления разрешений
    fun rationale(description: String): PermissionManager {
        rationale = description
        return this
    }

    // 3. Старт процесса проверки наличия разрешений. Callback получит true, если ВСЕ разрешения
    // предоставлены. Callback должен содержать логику под оба случая - разрешения предоставлены
    // или не предоставлены.
    fun checkPermissions(callback: (Boolean) -> Unit) {
        this.callback = callback
        handlePermissionRequest()
    }

    // Старт процесса проверки наличия разрешений. Callback получит словарь, в котором перечислены
    // отдельные разрешения и статус по ним.
    fun checkDetailedPermissions(callback: (Map<Permission, Boolean>) -> Unit) {
        this.detailedCallback = callback
        handlePermissionRequest()
    }

    private fun handlePermissionRequest() {
        componentActivity.get()?.let { activity ->
            when {
                areAllPermissionsGranted(activity) -> sendPositiveResult()
                shouldShowPermissionRationale(activity) -> displayRationale(activity)
                else -> requestPermissions()
            }
        }
    }

    private fun displayRationale(activity: ComponentActivity) {
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.dialog_permission_title))
            .setMessage(rationale ?: activity.getString(R.string.dialog_permission_default_message))
            .setCancelable(false)
            .setPositiveButton(activity.getString(R.string.dialog_permission_button_positive)) { _, _ ->
                requestPermissions()
            }.show()
    }

    private fun sendPositiveResult() {
        sendResultAndCleanUp(getPermissionList().associateWith { true })
    }

    private fun sendResultAndCleanUp(grantResults: Map<String, Boolean>) {
        callback(grantResults.values.all { it })
        detailedCallback(grantResults.mapKeys { Permission.from(it.key) })
        cleanUp()
    }

    private fun cleanUp() {
        requiredPermissions.clear()
        rationale = null
        callback = {}
        detailedCallback = {}
    }

    private fun requestPermissions() {
        activityResultLauncher?.launch(getPermissionList())
    }

    private fun Permission.isGranted(activity: ComponentActivity) =
        permissions.all { permission -> hasPermission(activity, permission) }

    private fun areAllPermissionsGranted(activity: ComponentActivity) =
        requiredPermissions.all { permission -> permission.isGranted(activity) }

    private fun shouldShowPermissionRationale(activity: ComponentActivity) =
        requiredPermissions.any { it.isRationaleRequired(activity) }

    private fun getPermissionList() =
        requiredPermissions.flatMap { it.permissions.toList() }.toTypedArray()


    private fun Permission.isRationaleRequired(activity: ComponentActivity) =
        permissions.any { permission -> activity.shouldShowRequestPermissionRationale(permission) }

    private fun hasPermission(activity: ComponentActivity, permission: String) =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        fun from(activity: ComponentActivity) = PermissionManager(WeakReference(activity))
    }
}