package fi.clocklings.ui.companion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import fi.clocklings.R
import fi.clocklings.domain.AccessPolicy
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.slots
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
internal fun wardrobeStarInfo(
    kind: CompanionKind,
    total: Int,
    access: AccessPolicy,
): String =
    if (Wardrobe.itemsFor(kind).any { !access.item(it, total) }) {
        pluralStringResource(R.plurals.doll_info, total, total)
    } else {
        stringResource(R.string.doll_all_open)
    }

@Composable
internal fun WardrobeHints(
    kind: CompanionKind,
    total: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen)) {
        if (Wardrobe.lockedCount(kind, total) > 0) {
            Text(stringResource(R.string.doll_stars_explained), style = SkType.SmallSoft)
        }
        // Said once, above the first section for every companion. The doll's required clothes cannot be
        // taken off, so its hint names only the optional ones.
        if (kind.slots.isNotEmpty()) {
            val hint =
                if (kind.slots.all {
                        it.optional
                    }
                ) {
                    R.string.doll_optional_hint
                } else {
                    R.string.doll_optional_hint_partial
                }
            Text(stringResource(hint), style = SkType.SmallSoft)
        }
    }
}
