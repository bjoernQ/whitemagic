@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package de.mobilej.ktest

import android.Manifest
import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import de.mobilej.ktest.model.WebSite
import de.mobilej.whitemagic.LifecycleAwareAndroidViewModel
import de.mobilej.whitemagic.UsesAdvancedAsyncTask
import de.mobilej.whitemagic.async
import de.mobilej.whitemagicpermissions.requestPermission
import me.tatarka.bindingcollectionadapter2.ItemBinding
import me.tatarka.bindingcollectionadapter2.collections.DiffObservableList


@UsesAdvancedAsyncTask // to make the error checking work
class MainViewModel(app: Application) : LifecycleAwareAndroidViewModel(app) {

    interface ActionCallbacks {
        fun showInputDialog(): String

        fun showWebsiteStats(websiteUrl: String): String

        fun getActivity(): AppCompatActivity
    }

    val items = DiffObservableList<WebSite>(object : DiffObservableList.Callback<WebSite> {
        override fun areContentsTheSame(oldItem: WebSite?, newItem: WebSite?): Boolean {
            return oldItem?.url == newItem?.url
        }

        override fun areItemsTheSame(oldItem: WebSite?, newItem: WebSite?): Boolean {
            return oldItem?.url == newItem?.url
        }
    })

    val itemBinding: ItemBinding<WebSite> = ItemBinding.of<WebSite>(BR.item, R.layout.item)

    protected lateinit var actionCallbacks: ActionCallbacks

    private var liveData: LiveData<List<WebSite>>

    init {
        liveData = App.component().getDatabase().webSiteDao().getAllLiveData()
        liveData.observe(this, Observer<List<WebSite>> { t ->
            if (t != null) {
                items.update(t)
            }
        })
    }

    fun attachCallbacks(callbacks: ActionCallbacks) {
        actionCallbacks = callbacks
    }

    override fun onCleared() {
        super.onCleared()
        liveData.removeObservers(this)

    }

    fun onClick() {
        async<MainViewModel> {
            // we certainly don't need the permission but just for demonstration we request it
            val locationPermissionGranted = call<Boolean> { requestPermission(target.actionCallbacks.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION, "We need Location Permission", "Not really but this is a sample app.") }

            Toast.makeText(target.getApplication<App>(), "Value is $locationPermissionGranted", Toast.LENGTH_SHORT).show()

            if (locationPermissionGranted) {
                val websiteUrl = call<String> { target.actionCallbacks.showInputDialog() }

                val shouldAddToList = call<Boolean> { target.actionCallbacks.showWebsiteStats(websiteUrl) }

                if (shouldAddToList) {
                    inBackground {
                        App.component().getDatabase().webSiteDao().insert(WebSite(websiteUrl))
                    }
                } else {
                    println("Error/Canceled")
                }
            }

        }
    }

}