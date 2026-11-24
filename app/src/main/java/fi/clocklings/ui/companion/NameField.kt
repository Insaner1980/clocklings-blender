package fi.clocklings.ui.companion

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.domain.CompanionKind
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.RetryableSave
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** Name field: saved while typing, at most 16 characters. Each companion keeps its own name. */
@Composable
internal fun NameField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.doll_name_label)
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        state = state,
        interactionSource = interaction,
        inputTransformation = NameLengthLimit,
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = SkType.NameField.copy(textAlign = TextAlign.Center),
        cursorBrush = SolidColor(SkColors.Primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier =
            modifier
                .keepEditorVisible()
                .onPreInterceptKeyBeforeSoftKeyboard { event ->
                    val key = event.nativeKeyEvent
                    val historyModifiers =
                        key.hasModifiers(KeyEvent.META_CTRL_ON) ||
                            key.hasModifiers(KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON)
                    if (key.keyCode == KeyEvent.KEYCODE_Z && historyModifiers) {
                        // Keep undo and redo in Compose's history instead of splitting them with the IME.
                        view.dispatchKeyEvent(key)
                        true
                    } else {
                        false
                    }
                }.widthIn(max = SkDimens.NameFieldMaxWidth)
                .fillMaxWidth()
                .semantics { contentDescription = label },
        decorator = { inner -> NameEditorContent(state.text.isEmpty() && !focused, inner) },
    )
}

@Composable
private fun NameEditorContent(
    showPlaceholder: Boolean,
    inner: @Composable () -> Unit,
) {
    Row(
        Modifier
            .background(SkColors.Surface, SkShapes.Pill)
            .border(SkDimens.BorderThin, SkColors.FieldOutline, SkShapes.Pill)
            .heightIn(min = SkDimens.IconButton)
            .padding(
                start = SkDimens.PanelPadding,
                end = SkDimens.FieldPaddingHorizontal,
                top = SkDimens.FieldPaddingVertical,
                bottom = SkDimens.FieldPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (showPlaceholder) {
                Text(
                    stringResource(R.string.doll_name_placeholder),
                    style = SkType.NameFieldPlaceholder.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            inner()
        }
        // Shows that the name can be typed in; the field itself carries the label.
        SkIcon(SkIcons.Pencil, SkDimens.ActionIcon, Modifier.clearAndSetSemantics { })
    }
}

/** Re-request visibility after the IME changes the short window's scrolling container. */
@Composable
private fun Modifier.keepEditorVisible(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeHeight, focused) {
        if (focused && imeHeight > 0) {
            withFrameNanos { }
            requester.bringIntoView()
        }
    }
    return bringIntoViewRequester(requester).onFocusChanged { focused = it.isFocused }
}

private const val MAX_NAME = 16

private val NameLengthLimit =
    InputTransformation {
        if (length > MAX_NAME) {
            val text = asCharSequence()
            val splitsSurrogate = text[MAX_NAME - 1].isHighSurrogate() && text[MAX_NAME].isLowSurrogate()
            delete(if (splitsSurrogate) MAX_NAME - 1 else MAX_NAME, length)
        }
    }

@Composable
internal fun rememberNameSave(
    kind: CompanionKind,
    editor: TextFieldState,
): (() -> Unit)? {
    val app = LocalAppViewModel.current
    var failed by rememberSaveable(kind) { mutableStateOf(false) }
    val request = remember(app, kind) { RetryableSave() }
    val save: (String) -> Unit = { requestedName ->
        failed = false
        request.run { result ->
            app.setName(kind, requestedName.trim()) { saved ->
                result(saved)
                failed = request.retry != null
            }
        }
    }
    val initialDraft = remember(editor) { editor.text.toString() }
    LaunchedEffect(editor, request) {
        var previous = initialDraft
        snapshotFlow { editor.text.toString() }.collect { draft ->
            if (draft != previous) {
                previous = draft
                save(draft)
            }
        }
    }
    return if (failed) ({ save(editor.text.toString()) }) else null
}
