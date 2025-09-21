package com.paradoxcat.waveformtest

import android.app.Application
import com.paradoxcat.waveformtest.data.di.dataModule
import com.paradoxcat.waveformtest.di.appModule
import com.paradoxcat.waveformtest.domain.di.domainModule
import com.paradoxcat.waveformtest.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for Koin + Android context, logger.
 */
class MainApplication : Application() {
    /**
     * when the application starts, modules are injected !
     */
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MainApplication)
            modules(appModule, dataModule, uiModule, domainModule)
        }
    }
}
