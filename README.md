# Whitemagic

An easy way to do lifecycle aware async task style thing on Android with Kotlin

# About

Ever wanted to write code like this?
```kotlin
async {
    loadingIndicatorShown = true
    val success = inBackground { Repository.longRunningOperation() }
    loadingIndicatorShown = false
    if(!success){
        showError = true
    }
}
```

Now you can!

# How to

The library defines a couple of extension functions for LifecycleOwner:

Function|Description
--------|-----------
async | wrap your async code inside the async lambda - it's important to not reference the outer scope from here. Therefore you get "target" to reference it.
inBackground | inside "async" you can use this to execute the given lambda on a background thread, the lambda can return a result
call | the lambda needs to return a tag (String) which you can refer to with findCallbackFor to return the actual result any time later

The best way to learn about the usage might be to look into the sample code - code is worth a thousand words, you know

There is also a LifecycleAwareAndroidViewModel which extends AndroidViewModel but a Lifecycle can be attached.

It's easy to use this library with it and also LiveData is easy to use.

To make use of LifecycleAwareAndroidViewModel you need to attach an actual Lifecycle

```kotlin
vm.attach(this)
```

Actual code in the ViewModel could look like this (code from the weirdo sample app :) )
```kotlin
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
```

# Things missing

- better documentation (obviously)
- a better sample app (the current is really weird, I know)
- unit tests at least for the core functionality

# Versioning and how to get it

For now it's just available via JitPack.

Versions below 1.0.0 are considered "early access" and things might change. Starting with 1.0.0 the API should be stable and the versioning scheme will follow the rules of semantic versioning.
