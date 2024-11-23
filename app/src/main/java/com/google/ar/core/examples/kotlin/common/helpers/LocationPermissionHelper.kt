/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.kotlin.common.helpers

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/** Helper to ask location permission.  */
object LocationPermissionHelper {
    private const val LOCATION_PERMISSION_CODE = 1
    private const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION

    /** Check to see we have the necessary permissions for this app.  */
    fun hasFineLocationPermission(activity: Activity): Boolean {
        return (ContextCompat.checkSelfPermission(activity, LOCATION_PERMISSION)
                == PackageManager.PERMISSION_GRANTED)
    }

    /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
    fun requestFineLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(LOCATION_PERMISSION), LOCATION_PERMISSION_CODE
        )
    }

    /** Check to see if the array of given permissions contain the location permission.  */
    fun hasFineLocationPermissionsResponseInResult(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (LOCATION_PERMISSION == permission) {
                return true
            }
        }

        return false
    }

    /** Check to see if we need to show the rationale for this permission.  */
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, LOCATION_PERMISSION)
    }

    /** Launch Application Setting to grant permission.  */
    fun launchPermissionSettings(activity: Activity) {
        val intent = Intent()
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.fromParts("package", activity.packageName, null))
        activity.startActivity(intent)
    }
}
