package org.haokee.recorder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.haokee.recorder.data.repository.SettingsRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val settingsRepository = SettingsRepository(context.applicationContext)
            if (settingsRepository.getAutoStart()) {
                KeepAliveService.start(context.applicationContext)
            }
        }
    }
}
