package dev.barabu.nature.permission

import android.Manifest

sealed class Permission(vararg val permissions: String) {

    // Отдельные разрешения
    data object Camera : Permission(Manifest.permission.CAMERA)

    data object ReadContacts : Permission(Manifest.permission.READ_CONTACTS)

    data object ImageCapture : Permission(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    data object Location : Permission(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    data object Storage : Permission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    companion object {
        fun from(permission: String) = when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> Location
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE -> Storage
            Manifest.permission.READ_CONTACTS -> ReadContacts
            Manifest.permission.CAMERA -> Camera
            else -> throw IllegalArgumentException("Unknown permission: $permission")
        }
    }
}