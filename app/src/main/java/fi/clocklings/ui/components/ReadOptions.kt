package fi.clocklings.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import fi.clocklings.R
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkType

@Composable
fun ReadOptions(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
        Text(stringResource(R.string.question_read), style = SkType.Question)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapOptions), content = content)
    }
}
