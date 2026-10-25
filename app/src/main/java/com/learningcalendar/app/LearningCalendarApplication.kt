package com.learningcalendar.app

import android.app.Application
import com.learningcalendar.app.di.AppContainer

class LearningCalendarApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
