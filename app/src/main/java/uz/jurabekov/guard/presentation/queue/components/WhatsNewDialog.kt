package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import uz.jurabekov.guard.R
import uz.jurabekov.guard.ui.theme.Accent500
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.PrimaryGradBot
import uz.jurabekov.guard.ui.theme.PrimaryGradTop

/**
 * "Yangilik!" e'loni — bir martalik to'liq ekran dialog.
 *
 *  - Yuqorida progress indicator (5 soniyada to'ladi).
 *  - 5 soniyadan so'ng avtomatik yopiladi.
 *  - "Tushunarli" tugmasi yoki X — darhol yopadi.
 *
 * Visibility va "faqat bir marta" mantiqi ViewModel'da (DataStore flag).
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // 5 soniyalik progress: 1f → 0f. `startProgress` birinchi kadrdan keyin
        // false bo'ladi va animatsiya ishga tushadi.
        var started by remember { mutableStateOf(false) }
        val progress by animateFloatAsState(
            targetValue = if (started) 0f else 1f,
            animationSpec = tween(durationMillis = AUTO_DISMISS_MS.toInt(), easing = LinearEasing),
            label = "whatsnew-progress"
        )

        LaunchedEffect(Unit) {
            started = true
            delay(AUTO_DISMISS_MS)
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(PrimaryGradTop, PrimaryGradBot))
                )
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {

                // ===== Yuqori progress indicator =====
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = Warning,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )

                // ===== Yopish (X) =====
                Box(modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceS)) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Yopish",
                            tint = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // ===== Markaziy kontent =====
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpaceXL)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(Dimens.RadiusXL))
                            .background(Color.White)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "UMK-Navbat",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(Modifier.height(Dimens.SpaceM))

                    Text(
                        text = "UMK-Navbat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(Modifier.height(Dimens.SpaceXL))

                    Text(
                        text = "🎉 Yangilik!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(Dimens.SpaceM))

                    Text(
                        text = buildAnnotatedString {
                            append("Endilikda ilovamizda navbat olish jarayonida xatolikka yo'l qo'ygan bo'lsangiz, olgan navbatingizni ")
                            withStyle(
                                SpanStyle(color = Warning, fontWeight = FontWeight.SemiBold)
                            ) {
                                append("bekor qilishingiz")
                            }
                            append(" mumkin.")
                        },
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                }

                // ===== "Tushunarli" tugmasi =====
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpaceL)
                        .clip(RoundedCornerShape(Dimens.RadiusM))
                        .background(Color.White)
                        .height(Dimens.ButtonHeight)
                        .clickable(onClick = onDismiss)
                ) {
                    Text(
                        text = "Tushunarli",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGradBot
                    )
                }

                Spacer(Modifier.height(Dimens.SpaceM))

                Text(
                    text = "12 soniyadan so'ng avtomatik yopiladi",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.SpaceL)
                )
            }
        }
    }
}

/** Accent orange highlight (matn urg'usi va progress). */
private val Warning = Accent500

private const val AUTO_DISMISS_MS = 12_000L
