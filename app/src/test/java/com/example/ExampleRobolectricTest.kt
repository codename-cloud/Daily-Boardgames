package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testAppName() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val appName = app.getString(R.string.app_name)
        assertEquals("Daily Boardgames", appName)
    }
}
