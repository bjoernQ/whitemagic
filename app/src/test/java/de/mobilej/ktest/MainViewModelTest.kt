package de.mobilej.ktest

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LiveData
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import de.mobilej.kmockit.*
import de.mobilej.ktest.model.AppDatabase
import de.mobilej.ktest.model.WebSite
import de.mobilej.ktest.model.WebSiteDao
import de.mobilej.whitemagic.advancedAsyncTaskMockMode
import mockit.Mocked
import mockit.integration.junit4.JMockit
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.reflect.KFunction

@RunWith(JMockit::class)
class MainViewModelTest {

    @Mocked
    lateinit var app: App

    @Mocked
    lateinit var appDatabase: AppDatabase

    @Mocked
    lateinit var mockDao: WebSiteDao

    @Mocked
    lateinit var mockLiveData: LiveData<List<WebSite>>

    @Mocked
    lateinit var mockActivity: AppCompatActivity

    @Mocked
    lateinit var mockCallbacks: MainViewModel.ActionCallbacks

    @Test
    fun testBasicFlow() {
        advancedAsyncTaskMockMode = true

        mockup<Toast> {
            on(forceFnType<(Context, CharSequence, Int) -> Toast>(Toast::makeText)) { Toast(it[0] as Context) }
            on(Toast::show) { }
        }

        mockup<App.Companion> {
            on(App.Companion::component) { app }
        }

        every { appDatabase.webSiteDao() } returns { mockDao }
        every { mockDao.getAllLiveData() } returns { mockLiveData }

        val sut = MainViewModel(App())

        val lifecycleOwner = MockLifecycleOwner()
        sut.attach(lifecycle = lifecycleOwner)

        sut.attachCallbacks(mockCallbacks)

        every { mockCallbacks.getActivity() } returns { mockActivity }
        every { mockCallbacks.showInputDialog() } returns { "mock1" }
        every { mockCallbacks.showWebsiteStats(anyString()) } returns { "mock2" }

        lifecycleOwner.getMockLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        sut.onClick()

        answerCallback(sut, "PERMISSION_REQUEST", true)
        answerCallback(sut, "mock1", "TypedText")
        answerCallback(sut, "mock2", true)

        val website = WebSite("TypedText")

        verifications {
            once { mockDao.insert(website) }
        }
    }

}

inline fun <T> forceFnType(fn: T) = fn as KFunction<*>