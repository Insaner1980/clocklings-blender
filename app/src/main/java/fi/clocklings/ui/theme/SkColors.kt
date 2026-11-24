package fi.clocklings.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Kaikki sovelluksen värit. Shared tokens for activity surfaces and artwork.
 * Näkymät eivät koskaan kirjoita värejä suoraan, vaan käyttävät näitä.
 */
object SkColors {
    val UiInkSoft = Color(0xFF655279)
    val Primary = Color(0xFF6741D9)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFE5DBFF)
    val PrimarySoft = Color(0xFFB197FC)
    val TonalFill = Color(0xFFF1E9FA)
    val SurfaceContainer = Color(0xFFF4EEFB)
    val LearnSurface = Color(0xFFFFF6D6)
    val ExploreSurface = Color(0xFFE6F3FF)
    val PlaySurface = Color(0xFFFFEAF2)
    val TogetherSurface = Color(0xFFE7F8EA)
    val CompanionSurface = Color(0xFFF2ECFF)
    val LearnTrack = Color(0xFFF0DB94)
    val PlayTrack = Color(0xFFF5C2D6)
    val FieldOutline = Color(0xFFD9C8EA)
    val OptCorrectFill = Color(0xFFD3F9D8)
    val DangerContainer = Color(0xFFFFE3E3)

    // Pinnat ja teksti
    val Bg = Color(0xFFFFF5FA)
    val Surface = Color(0xFFFFFFFF)
    val Line = Color(0xFFF1DDEA)
    val Ink = Color(0xFF3A2453)
    val PrimaryDisabledContent = Ink
    val InkSoft = Color(0xFF625075)

    /** Hairline around a colour swatch that is too close to the page background to read on its own. */
    val SwatchOutline = InkSoft.copy(alpha = 0.35f)

    val Shadow = Color(0xFF3A2453)
    val FlatSurface = Surface
    val IconSurface = Surface
    val IconFill = Color(0xFFE9DDF7)
    val IconBookCover = Color(0xFFFFA94D)
    val IconClockRing = Color(0xFF4DABF7)

    /** A faint lilac so the bubble reads against both white panels and the coloured screens. */
    val Bubble = SurfaceContainer
    val Badge = Color(0xFF2B8A3E)
    val Danger = Color(0xFFC92A2A)
    val StageShadow = Shadow.copy(alpha = 0.1f)
    val HomeSky = Color(0xFFDCEEFF)
    val HomeGround = Color(0xFFCDEFD3)
    val Cloud = Surface.copy(alpha = 0.75f)
    val SettingsBackground = Color(0xFFF7F3FA)
    val FaceEdge = Color(0xFFFAF7FC)
    val FaceShadow = Shadow.copy(alpha = 0.06f)
    val Glass = Surface.copy(alpha = 0.55f)
    val HandShadow = Shadow.copy(alpha = 0.14f)
    const val DISABLED_CONTROL_ALPHA = 0.45f
    const val LOCKED_BADGE_ALPHA = 0.5f

    // Viisarit
    val Hour = Color(0xFFE64980)
    val Minute = Color(0xFF228BE6)

    // Kulta
    val Gold = Color(0xFFFCC419)
    val StarTotalCompleteFill = Gold
    val HelpOnFill = Color(0xFFFFE066)
    val GoldDeep = Color(0xFFF59F00)

    // Palaute
    val Good = Color(0xFF2B8A3E)
    val GoodBg = Color(0xFFE3F8E7)
    val Try = Color(0xFFB35000)
    val TryBg = Color(0xFFFFF0E0)
    val HintFill = Color(0xFFFFF5CC)

    // Kellon puoliskot
    val Yli = Color(0xFFFFF3BF)
    val Vaille = Color(0xFFDCEEFF)

    /** Kellon reunan 12 väriä. */
    val Rainbow =
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFF922B),
            Color(0xFFFCC419),
            Color(0xFF94D82D),
            Color(0xFF51CF66),
            Color(0xFF20C997),
            Color(0xFF22B8CF),
            Color(0xFF339AF0),
            Color(0xFF5C7CFA),
            Color(0xFF845EF7),
            Color(0xFFCC5DE8),
            Color(0xFFF06595),
        )

    /** Sateenkaaren 7 väriä: konfetti, edistymispallot, ajastin, hiukset ja mekko. */
    val Rainbow7 =
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFA94D),
            Color(0xFFFCC419),
            Color(0xFF51CF66),
            Color(0xFF339AF0),
            Color(0xFF845EF7),
            Color(0xFFDA77F2),
        )

    /** Otsikon kirjainten värit järjestyksessä. */
    val Logo =
        listOf(
            Color(0xFFF03E3E),
            Color(0xFFF76707),
            Color(0xFFE8A200),
            Color(0xFF2F9E44),
            Color(0xFF1C7ED6),
            Color(0xFF7048E8),
            Color(0xFFC2255C),
        )

    /** Otsikon kirjaimen [index] väri: värit kiertävät järjestyksessä. */
    fun logoColor(index: Int): Color = Logo[index % Logo.size]

    // Menu tile fills
    val KouluFill = Color(0xFFFFE8A3)
    val VapaaFill = Color(0xFFBFE3FF)
    val PelaaFill = Color(0xFFFFD1E3)
    val KysyFill = Color(0xFFC3F2CB)
    val NukkeFill = Color(0xFFE5DBFF)

    /** Tasokorttien värit (Tasatunnit … Minuutit). */
    val LevelCards =
        listOf(
            Color(0xFFFFE3E3),
            Color(0xFFFFE8CC),
            Color(0xFFFFF3BF),
            Color(0xFFD3F9D8),
            Color(0xFFD0EBFF),
        )
    val ChallengeCard = Color(0xFFE5DBFF)

    // Sirut, pillerit ja pienet pinnat
    val Pressed = Color(0xFFFFF3BF)
    val PillMuted = Color(0xFFF1EAF5)
    val Legend = Color(0xFFFFF9E6)
    val DotEmpty = Color(0xFFEADDF0)

    // Vastausvaihtoehdot
    val OptCorrectBorder = Good

    // Tähdet ja lukko
    val StarOn = Color(0xFFFCC419)
    val StarOnStroke = Color(0xFFE67700)
    val StarOff = Color(0xFFE9DFF0)
    val StarOffStroke = Color(0xFFD6C8E0)
    val Lock = Color(0xFFB9A8C9)

    // Nukke ja vaatekaappi
    val ItemWindow = Color(0xFFFFF4F9)
    val ItemLockedBg = Color(0xFFF7F2FA)

    // Kellotaulu
    val Face = Color(0xFFFFFFFF)
    val Tick = Color(0xFFD3C6DE)
    val Hour24 = Color(0xFF6741C7)
    val Sun = Color(0xFFFCC419)
    val SunRay = Color(0xFFF59F00)
    val Moon = Color(0xFF5C7CFA)

    // Opetuksen merkinnät kellon päällä
    val MarkNumber = Color(0xFFFFE066)
    val MarkTick = Color(0xFFFF8FB8)
    val MarkHalf = Color(0xFFFFE8A3)
    val MarkRed = Color(0xFFFFC9C9)
    val MarkGreen = Color(0xFFB2F2BB)
    val MarkBlue = Color(0xFFA5D8FF)
    val ZoneToBorder = MarkBlue
    val ZonePastBorder = HelpOnFill
}
