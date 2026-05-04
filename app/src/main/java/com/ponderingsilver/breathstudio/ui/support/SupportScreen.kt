package com.ponderingsilver.breathstudio.ui.support

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SupportScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val billing = remember { SupportBilling(context) }
    val billingState by billing.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(billing) {
        billing.connect()
    }

    DisposableEffect(billing) {
        onDispose {
            billing.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF091A21),
                        Color(0xFF123039),
                        Color(0xFF173B3F),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 18.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SupportTopBar(onBack = onBack)
            SupportHeader()
            SupportOptionsCard(
                products = billingState.products,
                enabled = billingState.connected,
                onSupport = { product -> billing.launchPurchase(context, product) },
            )
            billingState.message?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xDCE7EFEC),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            CreatorSiteCard(
                onOpenWebsite = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(PonderingSilverHomeUrl),
                        ),
                    )
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SupportTopBar(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onBack) {
            Text("Back", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "Support",
            color = Color(0xFFEFDDB4),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SupportHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x14FFFFFF),
        contentColor = Color(0xFFF7F1E5),
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Support Breath Studio",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFFF8F4EA),
            )
            Text(
                text = "This app is free and ad-free. If it has brought value to your practice, these optional Google Play support options help keep the work moving. Please give only if you're in a place of abundance right now.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xDCE7EFEC),
                lineHeight = 24.sp,
            )
        }
    }
}

@Composable
private fun SupportOptionsCard(
    products: List<SupportProduct>,
    enabled: Boolean,
    onSupport: (SupportProduct) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Options",
            color = Color(0xFFF7F1E5),
            style = MaterialTheme.typography.titleLarge,
        )
        products.forEach { product ->
            SupportProductRow(
                product = product,
                enabled = enabled && product.productDetails != null,
                onClick = { onSupport(product) },
            )
        }
    }
}

@Composable
private fun SupportProductRow(
    product: SupportProduct,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0x10FFFFFF),
        contentColor = Color(0xFFF7F1E5),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFF8F4EA),
                )
                Text(
                    text = product.displayPrice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xCCE0EAE7),
                )
            }
            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(999.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFECD7AB),
                    contentColor = Color(0xFF153139),
                    disabledContainerColor = Color(0x18FFFFFF),
                    disabledContentColor = Color(0x88E7EFEC),
                ),
            ) {
                Text("Choose")
            }
        }
    }
}

@Composable
private fun CreatorSiteCard(
    onOpenWebsite: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0x1EFFF2D7),
                        Color(0x1457E1D4),
                    ),
                ),
            )
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Discover more from Pondering Silver",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF8F4EA),
            )
            Text(
                text = "Explore essays, transmissions, soul tools, reflections, and other projects from the creator of Breath Studio.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xDCE7EFEC),
                lineHeight = 21.sp,
            )
            OutlinedButton(
                onClick = onOpenWebsite,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFF1E0BA),
                ),
            ) {
                Text("Visit Pondering Silver")
            }
        }
    }
}

private const val PonderingSilverHomeUrl = "https://ponderingsilver.com"
