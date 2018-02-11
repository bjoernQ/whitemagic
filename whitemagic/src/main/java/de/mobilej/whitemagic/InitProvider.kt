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

package de.mobilej.whitemagic

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class InitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val stateRetainer = AppStateRetainer()
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(stateRetainer)
        setCurrentStateRetainer(stateRetainer)
        return true
    }

    override fun insert(uri: Uri?, values: ContentValues?): Uri {
        return Uri.parse("")
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        return MatrixCursor(listOf("").toTypedArray())
    }


    override fun update(uri: Uri?, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun getType(uri: Uri?): String {
        return ""
    }

}
