package fi.clocklings.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import fi.clocklings.R

/** Fredoka (otsikot ja isot tekstit) ja Nunito (leipäteksti). Staattiset TTF:t, yksi per paino. */
val Fredoka =
    FontFamily(
        Font(R.font.fredoka_medium, FontWeight.Medium),
        Font(R.font.fredoka_semibold, FontWeight.SemiBold),
        Font(R.font.fredoka_bold, FontWeight.Bold),
    )

val Nunito =
    FontFamily(
        Font(R.font.nunito_semibold, FontWeight.SemiBold),
        Font(R.font.nunito_bold, FontWeight.Bold),
        Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
    )

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)

private fun display(
    size: Int,
    weight: FontWeight = FontWeight.SemiBold,
    lineHeight: Float = 1.15f,
) = TextStyle(
    fontFamily = Fredoka,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.em,
    lineHeightStyle = Trim,
    color = SkColors.Ink,
)

private fun body(
    size: Int,
    weight: FontWeight = FontWeight.Bold,
    lineHeight: Float = 1.45f,
) = TextStyle(
    fontFamily = Nunito,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.em,
    lineHeightStyle = Trim,
    color = SkColors.Ink,
)

/**
 * Shared text styles for compact screens.
 */
object SkType {
    val HomeLogo = display(30, FontWeight.Bold, 1.2f)
    val Body = body(18)
    val BodySoft = body(18).copy(color = SkColors.UiInkSoft)
    val Small = body(15)
    val SmallSoft = body(15).copy(color = SkColors.UiInkSoft)
    val Eyebrow = body(14, FontWeight.ExtraBold, 1.33f).copy(color = SkColors.UiInkSoft)

    val Logo = display(40, FontWeight.Bold, 1.1f)
    val Tagline = display(20).copy(color = SkColors.UiInkSoft)
    val Title = display(24, lineHeight = 1.17f)
    val TaskTitleLarge = display(20, lineHeight = 1.2f)
    val Words = display(30, lineHeight = 1.2f)
    val Question = display(26, lineHeight = 1.23f)
    val Digital = display(24, lineHeight = 1.2f)
    val DayPart = display(18)
    val OptWords = display(20, lineHeight = 1.2f)
    val OptDigital = display(18, lineHeight = 1.2f).copy(color = SkColors.UiInkSoft)

    val Button = display(20, lineHeight = 1.2f)
    val ButtonSmall = display(19, lineHeight = 1.2f)
    val Chip = body(15, FontWeight.ExtraBold, 1.2f)
    val Segment = body(15, FontWeight.ExtraBold, 1.2f)
    val Pill = body(13, FontWeight.ExtraBold, 1.2f)
    val LockPill = body(13, lineHeight = 1.2f).copy(color = SkColors.UiInkSoft)
    val LockPillTile = LockPill.copy(fontSize = 12.sp)
    val PillLarge = display(19, lineHeight = 1.1f)
    val NowWords = display(19, lineHeight = 1.26f)

    val BigButton = display(22, lineHeight = 1.18f)

    val FeedbackOk = display(26).copy(color = SkColors.Good)

    val LessonTitle = display(20, lineHeight = 1.2f)
    val LessonTitleCurrent = display(24, lineHeight = 1.17f)
    val LessonNumberCurrent = display(26, FontWeight.Bold, 1f)
    val PracticeTitle = display(18, lineHeight = 1.22f)
    val Counter = body(15, FontWeight.ExtraBold, 1.33f)
    val LessonNumber = display(22, FontWeight.Bold, 1f)
    val LessonText = body(19, lineHeight = 1.47f)

    val ResultTitle = display(34, lineHeight = 1.18f)
    val ResultText = body(18)
    val ScoreHuge = display(96, FontWeight.Bold).copy(lineHeight = 100.sp, color = SkColors.Primary)
    val ChallengeBadge = display(16)

    /** Smaller than [Title], so a screen's own title stays its largest heading. */
    val SectionTitle = display(20, lineHeight = 1.2f)
    val SettingsHeading = body(14, FontWeight.ExtraBold, 1.43f).copy(color = SkColors.UiInkSoft)
    val ItemNameMinSize = 10.sp
    val ItemNameStep = 0.5.sp
    val ItemName = body(14, lineHeight = 1.2f)
    val NameField = display(22, lineHeight = 1.2f)

    /** Same size as [NameField], so typing does not move the field, but lighter to read as a hint. */
    val NameFieldPlaceholder = NameField.copy(fontWeight = FontWeight.Medium, color = SkColors.UiInkSoft)
}
