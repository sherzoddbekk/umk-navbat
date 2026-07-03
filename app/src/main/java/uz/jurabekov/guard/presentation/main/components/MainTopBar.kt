package uz.jurabekov.guard.presentation.main.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Main screen top bar — QueueScreen TopBar bilan vizual konsistent.
 *
 * **Tuzilishi:**
 *  - Chap: menu icon button (drawer trigger)
 *  - Markaz/chap: section title
 *
 * **Scroll-aware elevation** (optional):
 *  `isScrolled = true` bo'lganda subtle border + tonal elevation. Default
 *  `false` — har screen LazyListState'iga ulansa qo'shimcha komponent
 *  yozish kerak; hozircha optional parameter sifatida qoldiramiz.
 *
 * **statusBarsPadding** — edge-to-edge UI da status bar ostiga kontent
 * o'tib ketmasin uchun. MainActivity'da `enableEdgeToEdge()` yoqilgan.
 */
@Composable
fun MainTopBar(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    isScrolled: Boolean = false
) {
    val elevation by animateDpAsState(
        targetValue = if (isScrolled) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "main-topbar-elevation"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 0.4f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "main-topbar-border"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = elevation,
        tonalElevation = if (isScrolled) 2.dp else 0.dp
    ) {
        Box(modifier = Modifier.statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS)
            ) {
                MenuIconButton(onClick = onMenuClick)

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha)
                    )
            )
        }
    }
}

/**
 * Menu icon button — dumaloq, QueueScreen'dagi InfoIconButton/LoginIconButton
 * dizayni bilan uyg'un.
 */
@Composable
private fun MenuIconButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menyu",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
