package com.lfr.dynamicforms.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lfr.dynamicforms.presentation.components.ShimmerSkeleton
import com.lfr.dynamicforms.presentation.theme.DynamicFormsTheme

@Composable
fun FormSkeletonScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress bar skeleton
        ShimmerSkeleton(height = 4.dp)
        ShimmerSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth(0.3f))

        Spacer(Modifier.height(8.dp))

        // Section card skeleton
        repeat(2) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section header
                    ShimmerSkeleton(height = 20.dp, modifier = Modifier.fillMaxWidth(0.5f))
                    ShimmerSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth(0.7f))

                    Spacer(Modifier.height(4.dp))

                    // Field skeletons
                    repeat(3) {
                        ShimmerSkeleton(height = 14.dp, modifier = Modifier.fillMaxWidth(0.4f))
                        ShimmerSkeleton(height = 48.dp)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FormSkeletonScreenPreview() {
    DynamicFormsTheme(dynamicColor = false) {
        FormSkeletonScreen()
    }
}
