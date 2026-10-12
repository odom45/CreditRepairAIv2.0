package com.creditrepairai.v2.guidance

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.creditrepairai.v2.MainActivity
import com.creditrepairai.v2.R
import java.util.concurrent.TimeUnit

class FollowUpWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("checkins", "Private case check-ins", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(id.hashCode(), NotificationCompat.Builder(applicationContext, "checkins")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("CreditRepairAI check-in")
            .setContentText("Review your saved correspondence when convenient. This is not a promised response date.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(open).setAutoCancel(true).build())
        return Result.success()
    }
    companion object {
        fun schedule(context: Context, filingId: String, day: Int, due: Long) {
            WorkManager.getInstance(context).enqueueUniqueWork("filing-$filingId-$day", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<FollowUpWorker>().setInitialDelay((due - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
                    .addTag("private-case-checkins").build())
        }
        fun cancelAll(context: Context) { WorkManager.getInstance(context).cancelAllWorkByTag("private-case-checkins") }
    }
}
