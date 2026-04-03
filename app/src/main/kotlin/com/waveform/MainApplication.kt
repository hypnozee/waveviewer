package com.waveform

import android.app.Application
import com.waveform.data.di.dataModule
import com.waveform.di.domainModule
import com.waveform.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for Koin + Android context, logger.
 */
class MainApplication : Application() {
    /**
     * when the application starts, modules are injected!
     */
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MainApplication)
            modules(dataModule, uiModule, domainModule)
        }
    }
}
