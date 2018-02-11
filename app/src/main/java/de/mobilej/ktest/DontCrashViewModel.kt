package de.mobilej.ktest

import android.app.Application
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import de.mobilej.whitemagic.LifecycleAwareAndroidViewModel
import de.mobilej.whitemagic.async

class DontCrashViewModel(app: Application) : LifecycleAwareAndroidViewModel(app) {


    fun init() {
        lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun onPause() {
                println("pause")
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun onResume() {
                println("resume")
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                println("destroy")
            }
        })


        async<DontCrashViewModel> {
            //inBackground { SystemClock.sleep(2000) }

            //try {
            val res = inBackground {
                //throw Exception("Exception")
                "test"
            }

            target.printIt()
            println("res is $res")

            //} catch (e: Exception) {
            //    e.printStackTrace()
            //    println(e)
            //}
        }

    }

    fun printIt() {
        println("PrintIt executed")
    }

}