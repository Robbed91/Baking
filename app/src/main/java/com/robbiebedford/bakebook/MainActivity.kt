package com.robbiebedford.bakebook

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.robbiebedford.bakebook.data.database.BakeBookDatabase
import com.robbiebedford.bakebook.data.repository.BakeBookRepository
import com.robbiebedford.bakebook.navigation.BakeBookApp
import com.robbiebedford.bakebook.timer.BakeBookTimerScheduler
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
        BakeBookTimerScheduler.createTimerChannel(this)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            BakeBookTheme {
                BakeBookApp(viewModel = viewModel)
            }
        }
    }
}
