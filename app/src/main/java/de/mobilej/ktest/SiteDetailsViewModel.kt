@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package de.mobilej.ktest

import android.app.Application
import android.databinding.ObservableBoolean
import android.databinding.ObservableField
import com.facebook.stetho.okhttp3.StethoInterceptor
import de.mobilej.whitemagic.LifecycleAwareAndroidViewModel
import de.mobilej.whitemagic.UsesAdvancedAsyncTask
import de.mobilej.whitemagic.async
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException


@UsesAdvancedAsyncTask // to make the error checking work
class SiteDetailsViewModel(app: Application) : LifecycleAwareAndroidViewModel(app) {

    interface ActionCallbacks {
        fun exitWithResult(result: Boolean)
    }

    val result = ObservableField<String>()
    val error = ObservableBoolean(true)
    val loading = ObservableBoolean(false)

    private lateinit var actionCallbacks: ActionCallbacks

    fun setActionCallbacks(actionCallbacks: ActionCallbacks) {
        this.actionCallbacks = actionCallbacks
    }

    fun setSiteUrl(url: String) {
        async<SiteDetailsViewModel> {
            target.loading.set(true)

            try {
                val result = inBackground<String> {
                    val client = OkHttpClient.Builder()
                            .addNetworkInterceptor(StethoInterceptor())
                            .build()
                    val request = Request.Builder()
                            .url("https://api.letsvalidate.com/v1/technologies/?url=$url")
                            .build()

                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        target.error.set(false)
                    } else {
                        target.error.set(true)
                    }
                    target.loading.set(false)
                    response.body()?.string() ?: ""
                }
                
                target.result.set(result)

            } catch (ioException: IOException) {
                target.error.set(true)
                target.loading.set(false)
                target.result.set("Error $ioException for $url")
            }
        }
    }

    fun onOkClicked() {
        actionCallbacks.exitWithResult(!error.get())
    }
}