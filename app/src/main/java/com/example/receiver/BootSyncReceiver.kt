package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.SoundNotificationHelper
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootSyncReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootSyncReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device boot completed or package updated, initiating background cloud sync")
            val appContext = context.applicationContext
            SoundNotificationHelper.initializeChannel(appContext)

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val db = AppDatabase.getDatabase(appContext, scope)
                    val repository = AppRepository(db)
                    val firebaseManager = FirebaseManager.getInstance(appContext)
                    firebaseManager.syncChangedCollectionsOnce(repository)
                } catch (e: Exception) {
                    Log.w(TAG, "Error initiating boot sync", e)
                }
            }
        }
    }
}
