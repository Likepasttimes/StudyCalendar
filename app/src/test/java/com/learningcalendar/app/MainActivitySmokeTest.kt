package com.learningcalendar.app

import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MainActivitySmokeTest {
    @Test
    fun applicationAndCalendarActivityStartWithoutCrashing() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()

        assertFalse(controller.get().isFinishing)

        controller.pause().stop().destroy()
    }
}
