package uz.jurabekov.guard.presentation.queue.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import uz.jurabekov.guard.R
import uz.jurabekov.guard.ui.theme.Dimens

/**
 * Bitta info page — title, description, image.
 * Onboarding'dan farqli — alohida data class
 * (onboarding kelajakda boshqacha o'zgarishi mumkin).
 */
data class AppInfoPage(
    val imageRes: Int,
    val title: String,
    val description: String
)

/**
 * Ilova haqida ma'lumot dialog'i — 4 page, swipe + buttonlar bilan.
 *
 * Onboarding'dan farqli:
 *  - DataStore'ga hech nima yozmaydi
 *  - Yopilganda navigation o'zgarmaydi (modal)
 *  - "Roziman" tugmasi onDismiss() chaqiradi
 *
 * @param onDismiss Yopish callback (back button yoki "Roziman" bosilganda)
 */
@Composable
fun AppInfoDialog(onDismiss: () -> Unit) {
    val pages = remember { appInfoPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val isLastPage = pagerState.currentPage == pages.lastIndex

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false  // full-screen
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // ===== Top bar - back button =====
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.SpaceXS, vertical = Dimens.SpaceXS)
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // ===== Pager =====
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    AppInfoPageContent(page = pages[pageIndex])
                }

                // ===== Indicator (markazda) =====
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.SpaceM)
                ) {
                    DotIndicator(
                        pageCount = pages.size,
                        currentPage = pagerState.currentPage
                    )
                }

                // ===== Next / Roziman button (o'ng pastda) =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Dimens.SpaceL,
                            vertical = Dimens.SpaceL
                        )
                ) {
                    NextButton(
                        isLastPage = isLastPage,
                        onClick = {
                            if (isLastPage) {
                                onDismiss()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }
    }
}

/* ============================================================
 * Bitta page content (rasm + title + description)
 * ============================================================ */
@Composable
private fun AppInfoPageContent(page: AppInfoPage) {
    val configuration = LocalConfiguration.current
    val imageHeight = (configuration.screenHeightDp * 0.40f).dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.SpaceL)
    ) {
        Spacer(Modifier.height(Dimens.SpaceS))

        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight)
        )

        Spacer(Modifier.height(Dimens.SpaceXL))

        Text(
            text = page.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Dimens.SpaceM))

        Text(
            text = page.description,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/* ============================================================
 * Pixar-style dot indicator
 * ============================================================ */
@Composable
private fun DotIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val dotSize by animateDpAsState(
                targetValue = if (isActive) 12.dp else 8.dp,
                animationSpec = spring(stiffness = 300f),
                label = "dot-size-$index"
            )
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

/* ============================================================
 * Animated next button
 * ============================================================ */
@Composable
private fun NextButton(
    isLastPage: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .heightIn(min = 52.dp)
    ) {
        AnimatedContent(
            targetState = isLastPage,
            transitionSpec = {
                (slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)))
                    .togetherWith(
                        slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "next-button"
        ) { lastPage ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                if (lastPage) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Roziman",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "Keyingisi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/* ============================================================
 * Pages content
 * ============================================================ */
private fun appInfoPages(): List<AppInfoPage> = listOf(
    AppInfoPage(
        imageRes = R.drawable.onboarding_1,
        title = "Navbatni oson oling",
        description = "Mashina raqamingizni va ism familiyangizni kiriting va zavodga kirish uchun navbatga yoziling."
    ),
    AppInfoPage(
        imageRes = R.drawable.onboarding_2,
        title = "Navbatingizni kuzating",
        description = "Navbatingiz qachon kelishini telefoningiz orqali real vaqtda kuzating."
    ),
    AppInfoPage(
        imageRes = R.drawable.onboarding_3,
        title = "Navbat bilan kirish",
        description = "Navbatingiz kelganda zavodga muammosiz kirib, yukni topshiring."
    ),
    AppInfoPage(
        imageRes = R.drawable.onboarding_4,
        title = "Ma'lumotlar xavfsiz qayta ishlanadi",
        description = "Haydovchi va mashina ma'lumotlari tekshirilib, tizimga ishonchli saqlanadi."
    )
)
