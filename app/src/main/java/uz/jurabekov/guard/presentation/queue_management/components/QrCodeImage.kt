package uz.jurabekov.guard.presentation.queue_management.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR kod renderer — pure Java ZXing core'dan foydalanadi (native code yo'q).
 *
 * **Performance:**
 *  - `remember(content, sizePx)` — bitmap content yoki size o'zgarganda
 *    qayta yaratiladi. Aks holda recomposition'da har gal CPU-intensive
 *    encode + bitmap allocation yuz beradi.
 *  - `FilterQuality.None` — QR pixel-perfect rendering, blur yo'q.
 *  - Bitmap size CAP: `MAX_QR_PX` (576px = 192dp @ xxhdpi). Bundan kattaroq
 *    memory isrof (ARGB_8888'da 576² × 4 = ~1.3 MB). Scanning sifati uchun
 *    yetarli.
 *
 * **Memory note:**
 *  Bitmap dialog dismiss bo'lganda Compose tomonidan release qilinadi
 *  (remember scope tugagach). Manual `recycle()` Compose'da kerak emas.
 *
 * **Error correction:**
 *  `M` (Medium, ~15% recovery) — kichik dirt/glare bilan ham scan qilinadi.
 *  `L` (~7%) tezroq lekin noyob holatlarda fail. `H` (~30%) — overkill.
 *
 * @param content QR ichiga yoziladigan matn (URL, UUID, ...). UTF-8 sifatida
 *                encode qilinadi.
 * @param size    Compose Dp — pixel'ga aylantirildi screen density bo'yicha.
 */
@Composable
fun QrCodeImage(
    content: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val targetPx = with(density) { size.toPx().toInt().coerceAtMost(MAX_QR_PX) }

    val bitmap: ImageBitmap? = remember(content, targetPx) {
        encodeQrSafely(content, targetPx)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(androidx.compose.ui.graphics.Color.White)
            .padding(6.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "QR kod: $content",
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = "QR yaratib bo'lmadi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * QR encoding — exception'larni yutib oladi va `null` qaytaradi.
 * UI komponenti shu null'ni fallback bilan tutadi.
 */
private fun encodeQrSafely(content: String, sizePx: Int): ImageBitmap? =
    runCatching { encodeQrToBitmap(content, sizePx).asImageBitmap() }.getOrNull()

private fun encodeQrToBitmap(content: String, sizePx: Int): Bitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 1,                            // tight quiet zone
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )

    val matrix = QRCodeWriter().encode(
        content,
        BarcodeFormat.QR_CODE,
        sizePx,
        sizePx,
        hints
    )

    val w = matrix.width
    val h = matrix.height
    // Single allocation — ARGB int array. setPixels bir marotaba bitmap'ga
    // yoziladi (per-pixel setPixel mingllab JNI hop bo'lardi).
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val rowOffset = y * w
        for (x in 0 until w) {
            pixels[rowOffset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }

    return Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, w, 0, 0, w, h)
    }
}

private const val MAX_QR_PX = 576
