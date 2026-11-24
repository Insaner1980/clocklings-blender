package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import fi.clocklings.R
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
fun ReadOptions(
    modifier: Modifier = Modifier,
    promptGap: Dp = SkDimens.GapM,
    prompt: @Composable () -> Unit = { Text(stringResource(R.string.question_read), style = SkType.Question) },
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(promptGap)) {
        prompt()
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions), content = content)
    }
}
