package fi.clocklings.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.clocklings.R
import fi.clocklings.analytics.UsageAnalytics
import fi.clocklings.billing.PurchaseStatus
import fi.clocklings.billing.PurchaseUiState
import fi.clocklings.ui.components.ButtonKind
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkButton
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.settings.SettingsCard
import fi.clocklings.ui.settings.SettingsSwitch
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import kotlin.random.Random

/** A usability boundary, not age verification. A restored destination must pass the gate again. */
@Composable
fun ParentScreen(
    state: PurchaseUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    usage: UsageAnalytics = UsageAnalytics.Disabled,
) {
    var adult by remember { mutableStateOf(false) }
    ScreenScaffold(
        modifier,
        section = Section.HOME,
        topBar = { TopBar(stringResource(R.string.parent_title), onBack) },
    ) {
        if (adult) {
            PurchaseContent(state, onPurchase, onRestore, onRetry)
            if (usage.available) UsageSharing(usage)
        } else {
            AdultGate(onContinue = { adult = true }, onCancel = onBack)
        }
    }
}

@Composable
private fun UsageSharing(usage: UsageAnalytics) {
    val enabled by usage.enabled.collectAsStateWithLifecycle(initialValue = null)
    SettingsCard {
        SettingsSwitch(
            text = stringResource(R.string.usage_sharing_title),
            supporting = stringResource(R.string.usage_sharing_description),
            checked = enabled == true,
            enabled = enabled != null,
            onChange = usage::setEnabled,
        )
    }
}

@Composable
private fun AdultGate(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
) {
    val code = remember { Random.nextInt(GATE_CODE_MIN, GATE_CODE_LIMIT).toString() }
    var answer by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    val submit = {
        if (answer == code.reversed()) {
            onContinue()
        } else {
            answer = ""
            failed = true
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen)) {
        Text(stringResource(R.string.parent_gate_instruction, code.toList().joinToString(" ")), style = SkType.Body)
        val answerLabel = stringResource(R.string.parent_gate_answer)
        Text(answerLabel, style = SkType.Body)
        GateAnswerField(
            answer,
            answerLabel,
            failed,
            submit,
            onValueChange = {
                answer = it.filter(Char::isDigit).take(GATE_CODE_LENGTH)
                failed = false
            },
        )
        SkButton(
            stringResource(R.string.parent_continue),
            submit,
            Modifier.fillMaxWidth(),
            kind = ButtonKind.PRIMARY,
            enabled = answer.length == GATE_CODE_LENGTH,
        )
        SkButton(stringResource(R.string.parent_cancel), onCancel, Modifier.fillMaxWidth())
    }
}

@Composable
private fun GateAnswerField(
    answer: String,
    label: String,
    failed: Boolean,
    onSubmit: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    val errorMessage = stringResource(R.string.parent_gate_error)
    OutlinedTextField(
        value = answer,
        onValueChange = onValueChange,
        modifier =
            Modifier.fillMaxWidth().semantics {
                contentDescription = label
                if (failed) error(errorMessage)
            },
        isError = failed,
        shape = SkShapes.Field,
        supportingText =
            if (failed) {
                {
                    Text(
                        errorMessage,
                        style = SkType.Small,
                        color = SkColors.Danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            } else {
                null
            },
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SkColors.Surface,
                unfocusedContainerColor = SkColors.Surface,
                errorContainerColor = SkColors.Surface,
                focusedBorderColor = SkColors.Primary,
                unfocusedBorderColor = SkColors.FieldOutline,
                errorBorderColor = SkColors.Danger,
            ),
        textStyle = SkType.Body,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}

@Composable
private fun PurchaseContent(
    state: PurchaseUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SkDimens.GapScreen)) {
        Text(stringResource(R.string.full_unlock_name), style = SkType.Title)
        Text(stringResource(R.string.full_unlock_description), style = SkType.Body)
        Text(stringResource(R.string.full_unlock_rewards), style = SkType.Body)
        val unavailable = state.status == PurchaseStatus.UNAVAILABLE
        val status = stringResource(state.status.message())
        val statusSemantics = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        if (unavailable) {
            FlatPanel(color = SkColors.DangerContainer) {
                Text(status, style = SkType.Body, modifier = statusSemantics)
            }
        } else {
            Text(status, style = SkType.Body, modifier = statusSemantics)
        }
        val price = state.localizedPrice
        if (!unavailable && !state.fullAccess && price != null) {
            SkButton(
                stringResource(R.string.purchase_buy, price),
                onPurchase,
                Modifier.fillMaxWidth(),
                kind = ButtonKind.PRIMARY,
                enabled = state.canPurchase && !state.busy,
            )
        }
        if (state.status in
            setOf(PurchaseStatus.UNAVAILABLE, PurchaseStatus.FAILED, PurchaseStatus.ACKNOWLEDGEMENT_PENDING)
        ) {
            SkButton(
                stringResource(R.string.purchase_retry),
                onRetry,
                Modifier.fillMaxWidth(),
                kind = if (unavailable) ButtonKind.PRIMARY else ButtonKind.SECONDARY,
                enabled = !state.busy,
            )
        }
        SkButton(stringResource(R.string.purchase_restore), onRestore, Modifier.fillMaxWidth(), enabled = !state.busy)
        Text(stringResource(R.string.purchase_restore_explanation), style = SkType.SmallSoft)
    }
}

private fun PurchaseStatus.message(): Int =
    when (this) {
        PurchaseStatus.READY -> R.string.purchase_ready
        PurchaseStatus.UNAVAILABLE -> R.string.purchase_unavailable
        PurchaseStatus.PENDING -> R.string.purchase_pending
        PurchaseStatus.CANCELLED -> R.string.purchase_cancelled
        PurchaseStatus.VERIFYING -> R.string.purchase_verifying
        PurchaseStatus.OWNED -> R.string.purchase_owned
        PurchaseStatus.RESTORED -> R.string.purchase_restored
        PurchaseStatus.NOT_OWNED -> R.string.purchase_not_owned
        PurchaseStatus.FAILED -> R.string.purchase_failed
        PurchaseStatus.ACKNOWLEDGEMENT_PENDING -> R.string.purchase_acknowledgement_pending
    }

private const val GATE_CODE_MIN = 1000
private const val GATE_CODE_LIMIT = 10_000
private const val GATE_CODE_LENGTH = 4
