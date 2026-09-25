package com.fuzinewatch

import android.app.Application
import com.fuzinewatch.notif.Notifier

class FuzineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
        ScrapeWorker.enqueue(this)
    }
}
