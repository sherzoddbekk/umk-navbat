package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.domain.model.QueueItem
import uz.jurabekov.guard.ui.theme.Accent500
import uz.jurabekov.guard.ui.theme.Accent600
import uz.jurabekov.guard.ui.theme.Dimens

@Composable
fun NowEnteringBanner(
    item: QueueItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Accent500, Accent600)
                )
            )
            .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS + 2.dp)
    ) {
        // ===== Qator 1: HOZIR (chap) | full_name · #N (o'ng) =====
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.size(Dimens.SpaceXS))
                Text(
                    text = "Navbatdagi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = Color.White
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.fullName,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 1
                )
                Text(
                    text = " · #${item.queueNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ===== Qator 2: katta plate =====
        Text(
            text = item.plate,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.2.sp,
            color = Color.White
        )
    }
}
