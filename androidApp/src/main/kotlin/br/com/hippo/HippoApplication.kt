package br.com.hippo

import android.app.Application
import br.com.hippo.di.initKoin
import org.koin.android.ext.koin.androidContext

class HippoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@HippoApplication)
        }
    }
}
