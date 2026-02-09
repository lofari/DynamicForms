package com.lfr.dynamicforms.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lfr.dynamicforms.domain.usecase.SyncWorkScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SyncWorkSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncWorkScheduler {

    override fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncSubmissionsWorker>()
            .setConstraints(constraints)
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
    }

    companion object {
        const val TAG = "sync_submissions"
    }
}
