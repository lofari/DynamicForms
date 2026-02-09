package com.lfr.dynamicforms.presentation.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.domain.model.PendingSubmission
import com.lfr.dynamicforms.domain.model.SubmissionStatus
import com.lfr.dynamicforms.feature.formlist.R

@Composable
fun PendingSubmissionsSection(
    submissions: List<PendingSubmission>,
    onRetry: (String) -> Unit,
    onDiscard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (submissions.isEmpty()) return

    var expanded by remember { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pending Submissions (${submissions.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (expanded) R.string.collapse_content_description
                    else R.string.expand_content_description
                )
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                submissions.forEach { submission ->
                    PendingSubmissionItem(
                        submission = submission,
                        onRetry = { onRetry(submission.id) },
                        onDiscard = { onDiscard(submission.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PendingSubmissionItem(
    submission: PendingSubmission,
    onRetry: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_submission_${submission.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (submission.status == SubmissionStatus.FAILED)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = submission.formTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(submission.status)
            }

            val errMsg = submission.errorMessage
            if (submission.status == SubmissionStatus.FAILED && errMsg != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = errMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (submission.status == SubmissionStatus.FAILED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDiscard) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.discard_submission_content_description))
                        Spacer(Modifier.width(4.dp))
                        Text("Discard")
                    }
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.retry_submission_content_description))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: SubmissionStatus) {
    val (text, color) = when (status) {
        SubmissionStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.secondary
        SubmissionStatus.SYNCING -> "Syncing" to MaterialTheme.colorScheme.primary
        SubmissionStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
