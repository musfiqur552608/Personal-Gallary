package com.freedu.personalgallary.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.R
import com.freedu.personalgallary.ui.theme.BrandOrange
import com.freedu.personalgallary.ui.theme.BrandPink
import com.freedu.personalgallary.ui.theme.BrandViolet

@Composable
fun OnboardingScreen(onGrant: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.personal_gallary_logo),
                    contentDescription = "Personal Gallary logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Personal Gallary",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Your own private social feed — 100% offline, 100% yours.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            FeatureRow(Icons.Default.Movie, "Reels from your short videos")
            FeatureRow(Icons.Default.PhotoLibrary, "Feed + grid from photos & long videos")
            FeatureRow(Icons.Default.Lock, "Optional app lock — nothing leaves this device")
            Spacer(Modifier.height(32.dp))
            Button(onClick = onGrant, modifier = Modifier.height(52.dp)) {
                Text("Allow media access & continue")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (Build.VERSION.SDK_INT >= 33)
                    "We request ${Manifest.permission.READ_MEDIA_IMAGES} + ${Manifest.permission.READ_MEDIA_VIDEO} once."
                else "We request storage access once to index your existing photos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(4.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
