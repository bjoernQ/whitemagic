package de.mobilej.ktest

import android.arch.lifecycle.Lifecycle
import de.mobilej.kmockit.MockLifecycleOwner
import de.mobilej.kmockit.every
import de.mobilej.kmockit.mockup
import de.mobilej.whitemagic.advancedAsyncTaskMockMode
import junit.framework.Assert.assertEquals
import mockit.Mocked
import mockit.integration.junit4.JMockit
import okhttp3.*
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(JMockit::class)
class SiteDetailsViewModelTest {

    @Mocked
    lateinit var mockHttpClient: OkHttpClient

    @Mocked
    lateinit var mockResponse: Response

    @Mocked
    lateinit var mockCall: Call

    @Mocked
    lateinit var mockBody: ResponseBody

    @Mocked
    lateinit var mockCallbacks: SiteDetailsViewModel.ActionCallbacks

    @Mocked
    lateinit var mockBuilder: OkHttpClient.Builder

    @Test
    fun testBasicFlow() {

        mockup<OkHttpClient.Builder> {
            on(forceFnType<OkHttpClient.Builder.(Interceptor) -> OkHttpClient.Builder>(OkHttpClient.Builder::addNetworkInterceptor)) { mockBuilder }
            on(OkHttpClient.Builder::build) { mockHttpClient }
        }

        every { mockCall.execute() } returns { mockResponse }
        every { mockHttpClient.newCall(any(Request::class.java)) } returns { mockCall }
        every { mockResponse.isSuccessful } returns { true }
        every { mockResponse.body() } returns { mockBody }
        every { mockBody.string() } returns { "the_response" }

        advancedAsyncTaskMockMode = true

        val mockApp = App()

        val sut = SiteDetailsViewModel(mockApp)

        val lifecycleOwner = MockLifecycleOwner()
        sut.attach(lifecycle = lifecycleOwner)

        sut.setActionCallbacks(mockCallbacks)

        lifecycleOwner.getMockLifecycle().handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        sut.setSiteUrl("http://www.test.com")

        assertEquals(sut.result.get(), "the_response")
    }
}