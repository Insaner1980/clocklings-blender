package fi.clocklings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performScrollTo

/**
 * Reveal outer scroll containers before inner ones, such as the companion row below its preview.
 * Pinned bars and content without a scroll parent need no scrolling.
 */
fun SemanticsNodeInteraction.scrollToIfScrollable(): SemanticsNodeInteraction =
    apply {
        var ancestor = fetchSemanticsNode().parent ?: return@apply
        var parent = onParent()
        while (SemanticsActions.ScrollBy !in ancestor.config) {
            ancestor = ancestor.parent ?: return@apply
            parent = parent.onParent()
        }
        parent.scrollToIfScrollable()
        performScrollTo()
    }
