package com.freedu.personalgallary.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.R
import com.freedu.personalgallary.ui.components.GradientButton
import com.freedu.personalgallary.ui.theme.BrandOrange
import com.freedu.personalgallary.ui.theme.BrandPink
import com.freedu.personalgallary.ui.theme.BrandViolet
import com.freedu.personalgallary.ui.theme.brandHorizontal
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onGrant: () -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            // soft brand blobs
            Box(
                Modifier
                    .size(340.dp)
                    .offset(x = (-110).dp, y = (-90).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                BrandPink.copy(alpha = 0.28f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                Modifier
                    .size(300.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 90.dp, y = 70.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                BrandViolet.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Column(Modifier.fillMaxSize()) {
                HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                    when (page) {
                        0 -> WelcomePage()
                        1 -> FeaturesPage()
                        else -> PrivacyPage(onGrant)
                    }
                }
                // dots
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { i ->
                        val w by animateDpAsState(
                            if (i == pager.currentPage) 26.dp else 8.dp,
                            label = "dot"
                        )
                        val dotBrush: Brush = if (i == pager.currentPage) brandHorizontal
                        else Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(w)
                                .background(dotBrush, CircleShape)
                        )
                        if (i < 2) Spacer(Modifier.width(8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (pager.currentPage < 2) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            scope.launch { pager.animateScrollToPage(2) }
                        }) { Text("Skip") }
                        Spacer(Modifier.weight(1f))
                        GradientButton(
                            text = "Next",
                            onClick = {
                                scope.launch {
                                    pager.animateScrollToPage(pager.currentPage + 1)
                                }
                            },
                            modifier = Modifier.width(150.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.height(52.dp))
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painterResource(R.drawable.personal_gallary_logo),
            contentDescription = "Personal Gallary logo",
            modifier = Modifier
                .size(124.dp)
                .clip(RoundedCornerShape(32.dp))
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Personal Gallary",
            style = MaterialTheme.typography.displaySmall.copy(brush = brandHorizontal),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your own private social feed — 100% offline, 100% yours.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniChip("Offline")
            MiniChip("Private")
            MiniChip("Yours")
        }
    }
}

@Composable
private fun MiniChip(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun FeaturesPage() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Made from your memories",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "No uploads. No account. Just your camera roll, reimagined.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        FeatureCard(
            Icons.Default.Movie,
            listOf(BrandPink, BrandOrange),
            "Reels",
            "Short videos become a full-screen swipeable feed."
        )
        Spacer(Modifier.height(12.dp))
        FeatureCard(
            Icons.Default.PhotoLibrary,
            listOf(BrandViolet, BrandPink),
            "Feed + Grid",
            "Photos and long videos as posts, with search and albums."
        )
        Spacer(Modifier.height(12.dp))
        FeatureCard(
            Icons.Default.Lock,
            listOf(BrandOrange, BrandViolet),
            "Vault lock",
            "PIN + biometrics guard the app and private albums."
        )
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    gradient: List<Color>,
    title: String,
    desc: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrivacyPage(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(brandHorizontal),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Nothing ever leaves this device",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We ask for media access once to index your existing photos and videos. No internet, no ads, no tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        GradientButton(
            text = "Allow media access & continue",
            onClick = onGrant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "You can include or exclude any folder later in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
