package uz.jurabekov.guard.presentation.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.jurabekov.guard.R
import uz.jurabekov.guard.domain.model.User
import uz.jurabekov.guard.presentation.main.MainSection
import uz.jurabekov.guard.ui.theme.Dimens
import uz.jurabekov.guard.ui.theme.Error500

/**
 * Navigation drawer'ning to'liq kontenti.
 *
 * **Tuzilishi:**
 *  - **Header** (logo + loyiha nomi)  — `statusBarsPadding`
 *  - **Menu** (permission-filtered MainSection ro'yxati)  — flex grow
 *  - **Footer** (user full_name + logout button)  — bottom-anchored
 *
 * **Layout strategy:**
 *  - `Column` + `Modifier.weight(1f)` o'rtasidagi menu uchun → header
 *    tepada, footer pastda, menu sig'masa scroll bo'ladi.
 *  - `ModalDrawerSheet` Material3 default width (~360dp on phones).
 *
 * **Visual hierarchy:**
 *  - Active item — primary tint background + primary text
 *  - Inactive — transparent + onSurface
 *  - Logout — error tint (qizilroq, vizual ajratish)
 */
@Composable
fun AppDrawerContent(
    user: User?,
    availableSections: List<MainSection>,
    currentRoute: String?,
    onSectionClick: (MainSection) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {

            DrawerHeader()

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )

            // === Menu ro'yxati (flex grow) ===
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.SpaceS,
                        vertical = Dimens.SpaceS
                    )
            ) {
                availableSections.forEach { section ->
                    DrawerMenuItem(
                        section = section,
                        isSelected = currentRoute == section.route,
                        onClick = { onSectionClick(section) }
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )

            // === Footer: user + logout ===
            DrawerFooter(
                user = user,
                onLogoutClick = onLogoutClick
            )
        }
    }
}

/* ============================================================
 * Header — Logo + loyiha nomi
 * ============================================================ */
@Composable
private fun DrawerHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                horizontal = Dimens.SpaceM,
                vertical = Dimens.SpaceM
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Loyiha logosi",
            modifier = Modifier.size(44.dp)
        )

        Column {
            Text(
                text = "UMK-Navbat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.2.sp
            )
            Text(
                text = "Boshqaruv paneli",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* ============================================================
 * Menu item — bitta drawer satri
 * ============================================================ */
@Composable
private fun DrawerMenuItem(
    section: MainSection,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val fg = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.RadiusM))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceM, vertical = 12.dp)
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = section.title,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = section.title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg
        )
    }
}

/* ============================================================
 * Footer — user info + logout
 * ============================================================ */
@Composable
private fun DrawerFooter(
    user: User?,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceM)
    ) {
        // User info row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar surrogate — initials yoki person icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                val initial = user?.fullName
                    ?.firstOrNull { it.isLetter() }
                    ?.uppercase()

                if (initial != null) {
                    Text(
                        text = initial,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.fullName ?: "—",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = user?.roleCode?.replaceFirstChar { it.uppercase() } ?: "—",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceS))

        // Logout button
        LogoutButton(onClick = onLogoutClick)
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.RadiusM),
        color = Error500.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            modifier = Modifier.padding(
                horizontal = Dimens.SpaceM,
                vertical = 10.dp
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Tizimdan chiqish",
                tint = Error500,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Tizimdan chiqish",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Error500
            )
        }
    }
}
