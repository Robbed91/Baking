package com.robbiebedford.bakebook

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import com.robbiebedford.bakebook.data.database.BakeBookDatabase
import com.robbiebedford.bakebook.data.repository.BakeBookRepository
import com.robbiebedford.bakebook.navigation.BakeBookApp
import com.robbiebedford.bakebook.ui.theme.BakeBookTheme
import com.robbiebedford.bakebook.viewmodels.BakeBookViewModel
import com.robbiebedford.bakebook.viewmodels.BakeBookViewModelFactory

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val viewModel: BakeBookViewModel by viewModels {
        BakeBookViewModelFactory(BakeBookRepository(BakeBookDatabase.get(this)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createTimerChannel()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            BakeBookTheme {
                BakeBookApp(viewModel = viewModel, onTimerFinished = { showTimerNotification(it) })
            }
        }
    }

    private fun createTimerChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("timers", "Baking timers", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showTimerNotification(name: String) {
        val notification = NotificationCompat.Builder(this, "timers")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("BakeBook timer finished")
            .setContentText(name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(name.hashCode(), notification)
    }
}
