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

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

var advancedAsyncTaskMockMode = false
var advancedAsyncTaskDebugLogging = false
var advancedAsyncTaskRuntimeChecksEnabled = false
var advancedAsyncTaskRuntimeChecksPenalty = Penalty.LOG

private val handler = HandlerWrapper(Handler())
private val newExecutor = ExecutorWrapper(Executors.newFixedThreadPool(10)!!)
private val componentNameToComponent = HashMap<String, WeakReference<Any>>()
private val parkedForComponent = HashMap<String, ArrayList<ParkedCoroutine>>()
private val currentBackgroundOperationsForComponent = HashMap<String, ArrayList<Future<*>>>()

private val callbacks = HashMap<String, AwaitCallback<Any>>() // key=componentName/tag

private val observers = hashMapOf<String, LifecycleObserver>()

var stateRetainer: StateRetainer? = null


fun setCurrentStateRetainer(retainer: StateRetainer) {
    stateRetainer = retainer
}

fun findCallbackFor(which: Any, tag: String): AwaitCallback<Any>? {
    return findCallbackFor(which::class.java.canonicalName, tag)
}

fun findCallbackFor(componentName: String, tag: String): AwaitCallback<Any>? {
    val callbackToCall = callbacks["$componentName/$tag"]
    callbackToCall?.let {
        callbacks.remove(componentName)
    }
    if (callbackToCall != null) {
        return callbackToCall
    }

    // create a new callback from the saved state
    val state = stateRetainer?.restoreState(tag)
    stateRetainer?.removeState(tag)

    val cb = AwaitCallback<Any>()

    cb.observe(object : AwaitCallback.AwaitCallbackListener<Any> {
        override fun onResult(v: Any) {

            // continue only if component is Resumed - otherwise park it
            val tagFromState = state?.corotag
            if (tagFromState != null) {
                @Suppress("UNCHECKED_CAST")
                val coro = state.coroutine as Continuation<Any>

                if (isComponentActive(componentName)) {
                    coro.resume(v)
                } else {
                    parkCoroutineForLaterExecution(componentName, coro, v)
                }
            }
        }
    })

    return cb
}

fun <C> LifecycleOwner.async(c: suspend AdvancedAsyncTask<C>.() -> Unit): AdvancedAsyncTask<C> {
    if (advancedAsyncTaskRuntimeChecksEnabled) {
        checkForUnwantedReferences(c)
    }

    val componentName = ComponentId(this::class.java.canonicalName)
    registerComponentForAsyncTask(componentName, this)
    return asyncTask(componentName, c)
}

fun <C> asyncTask(componentName: ComponentId, c: suspend AdvancedAsyncTask<C>.() -> Unit): AdvancedAsyncTask<C> {

    val controller = AdvancedAsyncTask<C>(componentName)
    val coroutine = c.createCoroutine(controller, completion = object : Continuation<Unit> {

        override fun resumeWith(result: Result<Unit>) {
        }

        override val context: CoroutineContext = EmptyCoroutineContext
    })

    if (isComponentActive(componentName.name)) {
        coroutine.resume(Unit)
    } else {
        parkCoroutineForLaterExecution(componentName.name, coroutine, Unit)
    }

    return controller
}

fun LifecycleOwner.registerComponentForAsyncTask() {
    val componentName = ComponentId(this::class.java.canonicalName)
    registerComponentForAsyncTask(componentName, this)
}

fun registerComponentForAsyncTask(componentName: ComponentId, component: LifecycleOwner) {
    observers[componentName.name]?.let {
        component.lifecycle.removeObserver(it)
    }

    val observer = object : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            handler.post {
                log { "---- ON_RESUME for ${componentName.name}::$component" }
                asyncTaskResume(componentName.name, component)
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            handler.post {
                log { "---- ON_PAUSE for ${componentName.name}" }
                asyncTaskPause(componentName.name)
            }
        }

    }
    observers[componentName.name] = observer

    component.lifecycle.addObserver(observer)
}

private fun asyncTaskPause(componentName: String) {
    componentNameToComponent.remove(componentName)
}

internal fun asyncTaskCleanUp(componentName: String) {
    componentNameToComponent.remove(componentName)
    val removeFutures = currentBackgroundOperationsForComponent.remove(componentName)
    removeFutures?.forEach {
        try {
            if (!it.isCancelled && !it.isDone) {
                it.cancel(true)
            }
        } catch (e: Throwable){
            // ignored
        }
    }

    parkedForComponent.remove(componentName)
    callbacks.remove(componentName)
}

private fun asyncTaskResume(componentName: String, component: Any) {
    log { "--- asyncTaskResume for $componentName with $component" }
    componentNameToComponent[componentName] = WeakReference(component)

    val parkedForCurrentComponent = parkedForComponent[componentName]
    parkedForComponent.remove(componentName)
    parkedForCurrentComponent?.forEach {
        log { "--- exec $it" }
        if (it.exception == null) {
            handler.post {
                if (isComponentActive(componentName)) {
                    it.coroutine.resume(it.result)
                } else {
                    parkCoroutineForLaterExecution(componentName, it.coroutine, it.result, it.exception)
                }
            }
        } else {
            handler.post {
                if (isComponentActive(componentName)) {
                    it.coroutine.resumeWithException(it.exception)
                } else {
                    parkCoroutineForLaterExecution(componentName, it.coroutine, it.result, it.exception)
                }
            }
        }
    }
}

@UsesAdvancedAsyncTask
class AdvancedAsyncTask<out C>(val componentName: ComponentId) {

    val target: C
        get() = getTargetImpl()

    private fun getTargetImpl(): C {
        val x = componentNameToComponent[componentName.name]
        @Suppress("UNCHECKED_CAST")
        return (x?.get() as C)
    }

    suspend fun <V> call(f: () -> String): V {
        if (advancedAsyncTaskRuntimeChecksEnabled) {
            checkForUnwantedReferences(f)
        }

        return suspendCoroutine {
            val tag = f()

            stateRetainer?.retainState(tag, RetainedState(it, tag))

            val cb = AwaitCallback<V>()
            @Suppress("UNCHECKED_CAST")
            callbacks["${componentName.name}/$tag"] = cb as AwaitCallback<Any>

            cb.observe(object : AwaitCallback.AwaitCallbackListener<V> {
                override fun onResult(v: V) {
                    if (isComponentActive(componentName.name)) {
                        it.resume(v)
                    } else {
                        parkCoroutineForLaterExecution(componentName.name, it, v)
                    }
                }
            })
        }
    }

    suspend fun <V> inBackground(f: () -> V): V {
        if (advancedAsyncTaskRuntimeChecksEnabled) {
            checkForUnwantedReferences(f)
        }

        return suspendCoroutine {
            val scheduledFuture = AtomicReference<Future<*>>()
            val callable = Runnable {
                try {
                    val result = f()
                    currentBackgroundOperationsForComponent[componentName.name]?.remove(scheduledFuture.get())

                    // check if canceled
                    if (scheduledFuture.get()?.isCancelled != true) {
                        handler.post {
                            if (isComponentActive(componentName.name)) {
                                it.resume(result)
                            } else {
                                parkCoroutineForLaterExecution(componentName.name, it, result)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    // check if canceled
                    if (scheduledFuture.get()?.isCancelled != true) {
                        handler.post {
                            if (isComponentActive(componentName.name)) {
                                it.resumeWithException(e)
                            } else {
                                parkCoroutineForLaterExecution(componentName.name, it, null, e)
                            }
                        }
                    }
                }
            }
            val future = newExecutor.submit(callable)
            scheduledFuture.set(future)
            addCurrentBackgroundOperationsForComponent(componentName.name, future)
        }
    }

}

open class AwaitCallback<V> {
    interface AwaitCallbackListener<in V> {
        fun onResult(v: V)
    }

    private lateinit var observer: AwaitCallbackListener<V>

    fun observe(listener: AwaitCallbackListener<V>) {
        observer = listener
    }

    fun onResult(result: V) {
        observer.onResult(result)
    }
}

class ComponentId(val name: String) {

    companion object {
        fun from(component: Any): ComponentId {
            return ComponentId(component::class.java.canonicalName)
        }
    }

}

@DslMarker
annotation class UsesAdvancedAsyncTask

class RetainedState : Parcelable {

    constructor()

    lateinit var coroutine: Continuation<*>
    lateinit var corotag: String

    constructor(coro: Continuation<*>, tag: String) {
        coroutine = coro
        corotag = tag
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeByteArray(SerialisationHelper.serialiseCoro(coroutine))
        dest?.writeString(corotag)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @Suppress("UNUSED")
        @JvmField
        val CREATOR: Parcelable.Creator<RetainedState> = object : Parcelable.Creator<RetainedState> {
            override fun createFromParcel(source: Parcel): RetainedState {
                return RetainedState().apply {
                    val coro = SerialisationHelper.deserialiseCoro(source.createByteArray())
                    val tag = source.readString()
                    coroutine = coro
                    corotag = tag
                }
            }

            override fun newArray(size: Int): Array<RetainedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}

interface StateRetainer {

    fun retainState(tag: String, data: RetainedState)

    fun restoreState(tag: String): RetainedState?

    fun removeState(tag: String)

}

// see https://gist.github.com/Restioson/fb5b92e16eaff3d9267024282cf1ed72
object SerialisationHelper {

    private val kryo = Kryo()

    init {
        kryo.instantiatorStrategy = Kryo.DefaultInstantiatorStrategy(StdInstantiatorStrategy())
        kryo.fieldSerializerConfig.isIgnoreSyntheticFields = false
    }

    fun <T : Any> serialiseCoro(coro: Continuation<T>): ByteArray {
        val out = ByteArrayOutputStream()
        val output = Output(out)
        kryo.writeClassAndObject(output, coro)
        output.close()
        return out.toByteArray()
    }

    fun deserialiseCoro(bytes: ByteArray): Continuation<Any> {
        val fis = Input(ByteArrayInputStream(bytes))

        @Suppress("UNCHECKED_CAST")
        val coro = kryo.readClassAndObject(fis) as Continuation<Any>
        fis.close()

        coro::class.java.getDeclaredField("result").apply {
            isAccessible = true
            set(coro, kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED)
        }

        return coro
    }
}

private fun <T> parkCoroutineForLaterExecution(componentName: String, coro: Continuation<T>, v: T?, t: Throwable? = null) {
    val listOfParked = parkedForComponent[componentName] ?: ArrayList<ParkedCoroutine>()
    parkedForComponent[componentName] = listOfParked
    @Suppress("UNCHECKED_CAST")
    listOfParked.add(ParkedCoroutine(coro as Continuation<Any?>, v as Any?, t))
}

private fun addCurrentBackgroundOperationsForComponent(componentName: String, future: Future<*>) {
    val listOfBackgroundOperationsForComponent = currentBackgroundOperationsForComponent[componentName]
            ?: ArrayList<Future<*>>()
    currentBackgroundOperationsForComponent[componentName] = listOfBackgroundOperationsForComponent
    listOfBackgroundOperationsForComponent.add(future)
}

private fun isComponentActive(componentName: String): Boolean =
        componentNameToComponent.containsKey(componentName) && componentNameToComponent[componentName]?.get() != null

data class ParkedCoroutine(val coroutine: Continuation<Any?>, val result: Any?, val exception: Throwable? = null)

class HandlerWrapper(private val realHandler: Handler) {

    fun post(block: () -> Unit) {
        if (!advancedAsyncTaskMockMode) {
            realHandler.post(block)
        } else {
            block()
        }
    }
}

class ExecutorWrapper(private val realExecutor: ExecutorService) {
    fun submit(callable: Runnable): Future<*> {
        if (!advancedAsyncTaskMockMode) {
            return realExecutor.submit(callable)
        } else {
            callable.run()
            return object : Future<Any?> {
                override fun isDone(): Boolean {
                    return true
                }

                override fun get(): Any? {
                    return null
                }

                override fun get(timeout: Long, unit: TimeUnit?): Any? {
                    return null
                }

                override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
                    return true
                }

                override fun isCancelled(): Boolean {
                    return false
                }

            }
        }
    }
}

enum class Penalty() {
    LOG {
        override fun visualize(path: String, field: Field) {
            logError {
                "$path.${field.name} of type ${field.type} (${field.toGenericString()}) referenced at ${getStackTrace()}"
            }
        }

    },
    DEATH {
        override fun visualize(path: String, field: Field) {
            throw RuntimeException("$path.${field.name} of type ${field.type} (${field.toGenericString()}) referenced at ${getStackTrace()}")
        }
    };

    abstract fun visualize(path: String, field: Field)

    protected fun getStackTrace(): String {
        val sb = StringBuilder()
        val stack = Thread.currentThread().stackTrace
        var relevant = false

        stack.forEach {
            if (relevant) {
                sb.append(it.toString())
                sb.append("\n")
            }

            relevant = relevant || it.toString().contains("async")
        }

        return sb.toString()
    }
}

private fun log(block: () -> String) {
    if (advancedAsyncTaskDebugLogging) {
        Log.v("Whitemagic", block.invoke())
    }
}

private fun logError(block: () -> String) {
    if (advancedAsyncTaskDebugLogging) {
        Log.e("Whitemagic", block.invoke())
    }
}

private fun checkForUnwantedReferences(c: Any, path: String = "", alreadySeen: MutableList<Field> = mutableListOf()) {
    val fields = c::class.java.declaredFields + c::class.java.fields

    fields.forEach {
        if (!alreadySeen.contains(it)) {
            alreadySeen.add(it)
            it.isAccessible = true
            val toCheck = it.get(c)
            if (toCheck != null) {
                if (toCheck is Context) {
                    advancedAsyncTaskRuntimeChecksPenalty.visualize(path, it)
                } else {
                    checkForUnwantedReferences(toCheck, "$path.${it.name}", alreadySeen)
                }
            }
        }
    }
}