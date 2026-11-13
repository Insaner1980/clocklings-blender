package fi.clocklings.domain

enum class CompanionKind { HUMAN, CAT, DOG, ROBOT, DRAGON, DINOSAUR, BUNNY, UNICORN, CAR, NONE }

enum class CompanionColor { ORIGINAL, BLUE, GREEN, PURPLE, ORANGE }

/**
 * The colour options offered for this kind, in swatch order. The dragon and the dinosaur swap the option that
 * would look like their own Original colour (purple, green) for orange.
 */
fun CompanionKind.colorOptions(): List<CompanionColor> =
    when (this) {
        CompanionKind.DRAGON -> DragonColorOptions
        CompanionKind.DINOSAUR -> DinosaurColorOptions
        else -> DefaultColorOptions
    }

private val DefaultColorOptions =
    listOf(CompanionColor.ORIGINAL, CompanionColor.BLUE, CompanionColor.GREEN, CompanionColor.PURPLE)
private val DragonColorOptions =
    listOf(CompanionColor.ORIGINAL, CompanionColor.BLUE, CompanionColor.GREEN, CompanionColor.ORANGE)
private val DinosaurColorOptions =
    listOf(CompanionColor.ORIGINAL, CompanionColor.BLUE, CompanionColor.PURPLE, CompanionColor.ORANGE)

data class CompanionStyle(
    val color: CompanionColor = CompanionColor.ORIGINAL,
)

enum class ClockTheme { CLASSIC, RAINBOW, SPACE, FOREST, OCEAN, GARDEN, WORKSHOP, CUSTOM }

enum class ClockPalette { BLUE, GREEN, PURPLE, CORAL }

enum class ClockFrame { ROUND, SCALLOPED, BLOCKS }

enum class NumeralStyle { ROUNDED, PLAIN }

enum class HandStyle { ROUND, POINTER }

/** Only readable combinations are offered; all faces retain the same time geometry. */
data class CustomClock(
    val palette: ClockPalette = ClockPalette.BLUE,
    val frame: ClockFrame = ClockFrame.ROUND,
    val numerals: NumeralStyle = NumeralStyle.ROUNDED,
    val hands: HandStyle = HandStyle.ROUND,
)

data class ClockAppearance(
    val theme: ClockTheme = ClockTheme.RAINBOW,
    val custom: CustomClock = CustomClock(),
) {
    val numerals: NumeralStyle get() = if (theme == ClockTheme.CUSTOM) custom.numerals else NumeralStyle.ROUNDED
    val hands: HandStyle get() = if (theme == ClockTheme.CUSTOM) custom.hands else HandStyle.ROUND
}

data class Personalization(
    val companion: CompanionKind = CompanionKind.HUMAN,
    val styles: Map<CompanionKind, CompanionStyle> = emptyMap(),
    val clock: ClockAppearance = ClockAppearance(),
) {
    /** A stored colour the kind no longer offers (an old dragon purple or dinosaur green) reads as Original. */
    fun styleFor(kind: CompanionKind): CompanionStyle =
        styles[kind]?.takeIf { it.color in kind.colorOptions() } ?: CompanionStyle()

    fun withStyle(
        kind: CompanionKind,
        style: CompanionStyle,
    ): Personalization = copy(styles = if (style == CompanionStyle()) styles - kind else styles + (kind to style))
}
