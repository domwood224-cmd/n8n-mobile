package com.napcity.n8nmobile.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.napcity.n8nmobile.R
import com.napcity.n8nmobile.api.N8nClient
import com.napcity.n8nmobile.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExecutionMonitorWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val settings = SettingsStore(context)
            if (!settings.isConfigured) return@withContext Result.success()

            val api = N8nClient.getApi(settings.baseUrl, settings.apiKey)
            val response = api.getExecutions(limit = 10)
            if (!response.isSuccessful) return@withContext Result.retry()

            val executions = response.body()?.data ?: emptyList()
            val failed = executions.filter { it.status.equals("error", ignoreCase = true) }

            // Check against last seen
            val prefs = context.getSharedPreferences("monitor", Context.MODE_PRIVATE)
            val lastSeenIds = prefs.getStringSet("seen_failed", emptySet()) ?: emptySet()
            val newFailed = failed.filter { it.id !in lastSeenIds }

            if (newFailed.isNotEmpty()) {
                showNotification(newFailed.size, newFailed.firstOrNull()?.workflowId ?: "")
                prefs.edit()
                    .putStringSet("seen_failed", (lastSeenIds + newFailed.map { it.id }).takeLast(50).toSet())
                    .apply()
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(count: Int, workflowId: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "n8n_failures"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Workflow Failures", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("n8n: $count workflow${if (count > 1) "s" else ""} failed")
            .setContentText("Tap to view failed executions")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        nm.notify(1001, notification)
    }
}
