package de.mobilej.kmockit

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LifecycleRegistry
import de.mobilej.whitemagic.findCallbackFor

class MockLifecycleOwner : LifecycleOwner {

    private val mockLifecycle = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle {
        return mockLifecycle
    }

    fun getMockLifecycle() = mockLifecycle
}

fun answerCallback(sut: Any, tag: String, response: Any) {
    findCallbackFor(sut, tag)?.onResult(response)
}