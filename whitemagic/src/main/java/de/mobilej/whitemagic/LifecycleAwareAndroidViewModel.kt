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
import android.arch.lifecycle.*

open class LifecycleAwareAndroidViewModel(app: Application) : AndroidViewModel(app), LifecycleOwner {

    private var myLifecycle = LifecycleWrapper()

    fun attach(lifecycle: LifecycleOwner) {
        myLifecycle.setLifecycle(lifecycle)
        registerComponentForAsyncTask()
    }

    override fun getLifecycle(): Lifecycle {
        return myLifecycle
    }

    override fun onCleared() {
        asyncTaskCleanUp(ComponentId.from(this).name)
        super.onCleared()
        myLifecycle.publishDestroy()
    }
}

class LifecycleWrapper : Lifecycle(), LifecycleObserver {

    private var wrapped: LifecycleRegistry? = null
    private var observers = mutableListOf<LifecycleObserver>()

    fun setLifecycle(lifecycle: LifecycleOwner) {
        wrapped?.removeObserver(this)

        val lr = LifecycleRegistry(lifecycle)
        wrapped = lr
        observers.forEach {
            lr.addObserver(it)
        }

        lifecycle.lifecycle.addObserver(this)
    }

    override fun addObserver(observer: LifecycleObserver) {
        observer.let {
            wrapped?.addObserver(it)
            observers.add(it)
        }
    }

    override fun removeObserver(observer: LifecycleObserver) {
        observer.let {
            wrapped?.removeObserver(it)
            observers.remove(it)
        }
    }

    override fun getCurrentState(): State {
        return wrapped?.currentState ?: State.INITIALIZED
    }

    @OnLifecycleEvent(Event.ON_CREATE)
    fun onCreate() {
        wrapped?.handleLifecycleEvent(Event.ON_CREATE)
    }

    @OnLifecycleEvent(Event.ON_START)
    fun onStart() {
        wrapped?.handleLifecycleEvent(Event.ON_START)
    }

    @OnLifecycleEvent(Event.ON_RESUME)
    fun onResume() {
        wrapped?.handleLifecycleEvent(Event.ON_RESUME)
    }

    @OnLifecycleEvent(Event.ON_PAUSE)
    fun onPause() {
        wrapped?.handleLifecycleEvent(Event.ON_PAUSE)
    }

    @OnLifecycleEvent(Event.ON_STOP)
    fun onStop() {
        wrapped?.handleLifecycleEvent(Event.ON_STOP)
    }

    @OnLifecycleEvent(Event.ON_DESTROY)
    fun onDestroy() {
        // this would stop the LiveData observer ... so don't do it
        //wrapped?.handleLifecycleEvent(Event.ON_DESTROY)
    }

    fun publishDestroy() {
        wrapped?.handleLifecycleEvent(Event.ON_DESTROY)
        wrapped = null
    }
}