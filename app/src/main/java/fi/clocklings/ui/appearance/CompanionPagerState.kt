package fi.clocklings.ui.appearance

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import kotlinx.coroutines.flow.drop

/** Keep the preview alive across tabs; an access fallback never overwrites the saved companion. */
@Composable
internal fun rememberCompanionPager(
    companion: CompanionKind,
    stars: Int,
    access: AccessPolicy,
    onSelect: (CompanionKind) -> Unit,
): PagerState {
    val pager =
        key(access.fullAccess, stars >= AccessPolicy.COMPANION_STARS) {
            rememberPagerState(initialPage = access.displayedCompanion(companion, stars).ordinal) {
                CompanionKind.entries.size
            }
        }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentAccess by rememberUpdatedState(access)
    val currentStars by rememberUpdatedState(stars)
    LaunchedEffect(pager) {
        val initial = currentAccess.displayedCompanion(companion, currentStars).ordinal
        if (pager.settledPage != initial) pager.scrollToPage(initial)
        snapshotFlow { pager.settledPage }.drop(1).collect { page ->
            val chosen = CompanionKind.entries[page]
            if (currentAccess.companion(chosen, currentStars)) currentOnSelect(chosen)
        }
    }
    return pager
}
