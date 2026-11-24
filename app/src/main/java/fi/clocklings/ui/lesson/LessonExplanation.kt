package fi.clocklings.ui.lesson

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import fi.clocklings.domain.DemoStep
import fi.clocklings.domain.LessonPage
import fi.clocklings.ui.SkMotion
import fi.clocklings.ui.components.CompanionTip
import fi.clocklings.ui.resolve
import fi.clocklings.ui.resources
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
internal fun LessonExplanation(
    page: LessonPage,
    visibleCount: Int,
    animate: Boolean,
    compact: Boolean,
) {
    val res = resources()
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) SkDimens.GapS else SkDimens.GapM)) {
        page.text.forEachIndexed { index, paragraph ->
            val visible = index < visibleCount
            val opacity by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = if (animate && visible) tween(SkMotion.REDUCED_FADE) else snap(),
            )
            Text(
                paragraph.resolve(res),
                modifier =
                    Modifier.alpha(opacity).then(
                        if (visible) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() },
                    ),
                style = SkType.LessonText,
            )
        }
    }
}

@Composable
internal fun LessonCaption(
    page: LessonPage,
    caption: String,
    animate: Boolean,
) {
    val res = resources()
    val reserved =
        if (animate) {
            page.demo
                .filterIsInstance<DemoStep.Say>()
                .mapNotNull { it.text?.resolve(res) }
                .distinct()
        } else {
            emptyList()
        }
    if (reserved.isNotEmpty() || caption.isNotEmpty()) {
        Box {
            for (text in reserved) {
                if (text != caption) {
                    CompanionTip(
                        text,
                        Modifier.alpha(0f).clearAndSetSemantics { hideFromAccessibility() },
                        animated = false,
                    )
                }
            }
            if (caption.isNotEmpty()) CompanionTip(caption)
        }
    }
}
