package com.dspcontroller

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application class for DSP Controller.
 *
 * Annotated with [@HiltAndroidApp] to trigger Hilt code generation and
 * serve as the root of the dependency injection graph.
 */
@HiltAndroidApp
class DspApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // WHY: Timber is initialized here so logging is available from the earliest lifecycle point.
        // Debug tree logs everything; in release builds this tree would be replaced or omitted.
        Timber.plant(Timber.DebugTree())
        Timber.i("DSP Controller application started")
    }
}
