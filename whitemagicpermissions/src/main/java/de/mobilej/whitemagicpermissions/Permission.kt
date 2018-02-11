/*
   Copyright (C) 2017 Björn Quentin
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
     http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package de.mobilej.whitemagicpermissions

import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import de.mobilej.whitemagic.AdvancedAsyncTask
import de.mobilej.whitemagic.findCallbackFor

/*
 while the dialog (or the perm-request) is shown
 and the app is killed in background it takes a long time for the app to come up
 BUT in the emulator it's the same for e.g. the camera app
*/

fun <C> AdvancedAsyncTask<C>.requestPermission(activity: AppCompatActivity, permission: String, title: String, message: String): String {
    val permissionRequestFragment = PermissionRequestFragment()
    val fragmentArgs = Bundle()
    fragmentArgs.putString(PermissionRequestFragment.ARG_PERMISSION, permission)
    fragmentArgs.putString(PermissionRequestFragment.ARG_TITLE, title)
    fragmentArgs.putString(PermissionRequestFragment.ARG_MESSAGE, message)
    fragmentArgs.putString(PermissionRequestFragment.ARG_OWNER_CLAZZNAME, this.componentName.name)
    permissionRequestFragment.arguments = fragmentArgs

    val oldFrgmt = activity.supportFragmentManager.findFragmentByTag("TAG_PERMISSION_FRAGMENT")

    if (oldFrgmt != null) {
        activity.supportFragmentManager
                .beginTransaction()
                .remove(oldFrgmt)
                .commitAllowingStateLoss()
    }

    activity.supportFragmentManager
            .beginTransaction()
            .add(permissionRequestFragment, "TAG_PERMISSION_FRAGMENT")
            .commitAllowingStateLoss()

    return "PERMISSION_REQUEST"
}

class PermissionRequestFragment() : Fragment() {
    companion object {
        const val ARG_PERMISSION = "permission"
        const val ARG_TITLE = "title"
        const val ARG_MESSAGE = "message"
        const val ARG_OWNER_CLAZZNAME = "owner_clazzname"

        const val SAVE_STATE_KEY_FIRST_START = "first_start"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            if (!savedInstanceState.getBoolean(SAVE_STATE_KEY_FIRST_START, true)) {
                return
            }
        }

        val permission = arguments.getString(ARG_PERMISSION)
        val ownerClazzname = arguments.getString(ARG_OWNER_CLAZZNAME)

        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val callback = findCallbackFor(ownerClazzname, "PERMISSION_REQUEST")
            callback?.onResult(true)
        } else {
            if (shouldShowRequestPermissionRationale(permission)) {
                val dlg = ExplanationDialog()

                val args = Bundle()
                args.putString(ARG_TITLE, arguments.getString(ARG_TITLE))
                args.putString(ARG_MESSAGE, arguments.getString(ARG_MESSAGE))
                dlg.arguments = args
                dlg.setTargetFragment(this, 43)
                dlg.show(fragmentManager, "EXPLAINATION")
            } else {
                requestPermissions(
                        listOf(permission).toTypedArray(),
                        42);
            }
        }

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean(SAVE_STATE_KEY_FIRST_START, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 42) {
            val ownerClazzname = arguments.getString(ARG_OWNER_CLAZZNAME)
            val callback = findCallbackFor(ownerClazzname, "PERMISSION_REQUEST")

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callback?.onResult(true)
            } else {
                callback?.onResult(false)
            }
        }
    }

    fun dialogDismissed() {
        val permission = arguments.getString(ARG_PERMISSION)
        requestPermissions(
                listOf(permission).toTypedArray(),
                42);
    }

}


class ExplanationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(context)
                .setTitle(arguments.getString(PermissionRequestFragment.ARG_TITLE))
                .setMessage(arguments.getString(PermissionRequestFragment.ARG_MESSAGE))
                .setPositiveButton("Ok") { _: DialogInterface, i: Int ->
                    dismiss()
                }
                .setCancelable(true)
                .create()
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        val activity = targetFragment
        if (activity != null) {
            (activity as PermissionRequestFragment).dialogDismissed()
        }
    }
}