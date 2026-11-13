package fi.clocklings

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Hand
import fi.clocklings.domain.Item
import fi.clocklings.domain.Mark
import fi.clocklings.domain.MarkColor
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Slot
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.colorOptions
import fi.clocklings.ui.clock.ClockCommand
import fi.clocklings.ui.clock.ClockFace
import fi.clocklings.ui.companion.CompanionPose
import fi.clocklings.ui.companion.CompanionRig
import fi.clocklings.ui.companion.CompanionView
import fi.clocklings.ui.companion.rememberCompanionRig
import fi.clocklings.ui.itemName
import fi.clocklings.ui.theme.ClocklingsTheme
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Visual gallery of the production Blender companions, wardrobe thumbnails and clock styles.
 * Images are saved in the test application's files/gallery directory.
 */
@RunWith(AndroidJUnit4::class)
class GalleryTest {
    @get:Rule val compose = createComposeRule()

    private fun save(name: String) {
        val bitmap = capture().asAndroidBitmap()
        val dir =
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
                "gallery",
            ).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun dollInEveryItem() {
        val outfits =
            Wardrobe
                .itemsFor(CompanionKind.HUMAN)
                .filter { it.star > 0 }
                .map { Outfit.Default.with(it.slot, it.id) } + Outfit.Default
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    outfits.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach {
                                CompanionView(
                                    CompanionKind.HUMAN,
                                    it,
                                    Modifier.size(width = 90.dp, height = 153.dp),
                                    animated = false,
                                )
                            }
                        }
                    }
                }
            }
        }
        save("nukke")
    }

    /**
     * Saves `bare-large`: the companion named by the `kind` instrumentation argument, drawn bare and
     * still through its rig at 3 px per design unit, for close comparison against its reference.
     */
    @Test
    fun bareLarge() {
        val name = InstrumentationRegistry.getArguments().getString("kind") ?: CompanionKind.BUNNY.name
        compose.setContent {
            val rig = rememberCompanionRig(CompanionKind.valueOf(name), CompanionColor.ORIGINAL)!!
            ClocklingsTheme {
                GalleryColumn {
                    androidx.compose.foundation.Canvas(
                        Modifier.requiredSize(
                            with(LocalDensity.current) { (rig.width * BARE_SCALE).toDp() },
                            with(LocalDensity.current) { (rig.height * BARE_SCALE).toDp() },
                        ),
                    ) {
                        drawContext.transform.scale(BARE_SCALE, BARE_SCALE, androidx.compose.ui.geometry.Offset.Zero)
                        with(rig) { draw(CompanionColor.ORIGINAL, Outfit(), CompanionPose()) }
                    }
                }
            }
        }
        save("bare-large")
    }

    /**
     * Saves `moving-parts`: every companion drawn straight through its rig at the far ends of its
     * motion, first with the arm raised and the tail or ears swung one way, then swung the other way.
     */
    @Test
    fun movingParts() {
        val poses =
            listOf(
                CompanionPose(armDeg = 95f, partDeg = 18f),
                CompanionPose(armDeg = 120f, partDeg = -18f),
            )
        val kinds = CompanionKind.entries.filter { it != CompanionKind.NONE }
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    kinds.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { kind ->
                                val rig = rememberCompanionRig(kind, CompanionColor.ORIGINAL)!!
                                val outfit = if (kind == CompanionKind.HUMAN) Outfit.Default else Outfit()
                                poses.forEach { pose ->
                                    RigCanvas(rig, outfit, pose, Modifier.size(width = 62.dp, height = 106.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        save("moving-parts")
    }

    /** Saves `closed-eyes`: every companion drawn straight through its rig at blink 1, the left arm raised. */
    @Test
    fun closedEyes() {
        val closed = CompanionPose(blink = 1f)
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    CompanionKind.entries.filter { it != CompanionKind.NONE }.chunked(GALLERY_COLUMNS).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { kind ->
                                val rig = rememberCompanionRig(kind, CompanionColor.ORIGINAL)!!
                                val outfit = if (kind == CompanionKind.HUMAN) Outfit.Default else Outfit()
                                RigCanvas(
                                    rig,
                                    outfit,
                                    closed,
                                    Modifier.size(width = TILE_WIDTH.dp, height = TILE_HEIGHT.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        save("closed-eyes")
    }

    /** Saves `doll-crops`: the doll's items as wardrobe thumbnails, cropped to the item in the wardrobe's art size. */
    @Test
    fun dollCrops() {
        val items = Wardrobe.itemsFor(CompanionKind.HUMAN)
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    items.chunked(GALLERY_COLUMNS).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { ItemTile(CompanionKind.HUMAN, it, cropped = true) }
                        }
                    }
                }
            }
        }
        save("doll-crops")
    }

    /**
     * Saves `doll-large`: the default doll, a princess look (crown, princess dress, boots, bag) and a
     * long-hair look with wings, next to the approved cat sample at the same height.
     */
    @Test
    fun dollSampleLooks() {
        val princess =
            Outfit.Default
                .with(Slot.HEAD, "head-kruunu")
                .with(Slot.DRESS, "dress-overalls")
                .with(Slot.SHOES, "shoes-saappaat")
                .with(Slot.EXTRA, "extra-strawberry-bag")
        val fairy =
            Outfit.Default
                .with(Slot.HAIR, "hair-pitkat")
                .with(Slot.DRESS, "dress-tahti")
                .with(Slot.EXTRA, "extra-siivet")
        val cat = Outfit().with(Slot.HEAD, "cat-head-bobble-hat").with(Slot.NECK, "cat-neck-scarf")
        compose.setContent {
            ClocklingsTheme {
                val doll = Modifier.size(width = 140.dp, height = 238.dp)
                GalleryColumn {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompanionView(CompanionKind.HUMAN, Outfit.Default, doll, animated = false)
                        CompanionView(CompanionKind.HUMAN, princess, doll, animated = false)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        CompanionView(CompanionKind.HUMAN, fairy, doll, animated = false)
                        val catSize = Modifier.size(width = 190.dp, height = 238.dp)
                        CompanionView(CompanionKind.CAT, cat, catSize, animated = false)
                    }
                }
            }
        }
        save("doll-large")
    }

    @Test
    fun catInEveryItem() = companionInEveryItem(CompanionKind.CAT)

    /**
     * The two approved sample looks at a large size: Original with bobble hat and scarf, Blue with crown
     * and bell collar.
     */
    @Test
    fun catSampleLooks() =
        companionLarge(
            CompanionKind.CAT,
            CompanionColor.ORIGINAL to
                Outfit().with(Slot.HEAD, "cat-head-bobble-hat").with(Slot.NECK, "cat-neck-scarf"),
            CompanionColor.BLUE to
                Outfit().with(Slot.HEAD, "cat-head-crown").with(Slot.NECK, "cat-neck-bell-collar"),
        )

    @Test
    fun dogInEveryItem() = companionInEveryItem(CompanionKind.DOG)

    /** Two large dog looks: Original with sun hat and bandana, Blue with aviator cap and medal. */
    @Test
    fun dogSampleLooks() =
        companionLarge(
            CompanionKind.DOG,
            CompanionColor.ORIGINAL to Outfit().with(Slot.HEAD, "dog-head-sun-hat").with(Slot.NECK, "dog-neck-bandana"),
            CompanionColor.BLUE to Outfit().with(Slot.HEAD, "dog-head-aviator").with(Slot.NECK, "dog-neck-medal"),
        )

    @Test
    fun robotInEveryItem() = companionInEveryItem(CompanionKind.ROBOT)

    /**
     * Saves `robot-large`: Original with antenna and bow tie, and Blue with headphones, the light-up
     * necklace and the heart-eyes screen (a hat worn with a screen face), each about 300 dp wide. Under
     * them, one row drawn straight through the rig at blink 1 (the bare robot, and the propeller cap with
     * the heart-eyes screen) shows the Blender screen-eye blink on both eye styles.
     */
    @Test
    fun robotSampleLooks() {
        val looks =
            listOf(
                CompanionColor.ORIGINAL to
                    Outfit().with(Slot.HEAD, "robot-head-antenna").with(Slot.NECK, "robot-neck-bow-tie"),
                CompanionColor.BLUE to
                    Outfit()
                        .with(Slot.HEAD, "robot-head-headphones")
                        .with(Slot.NECK, "robot-neck-cape")
                        .with(Slot.EXTRA, "robot-extra-heart-eyes"),
            )
        val blinking =
            listOf(
                Outfit(),
                Outfit().with(Slot.HEAD, "robot-head-propeller").with(Slot.EXTRA, "robot-extra-heart-eyes"),
            )
        val closed = CompanionPose(blink = 1f)
        compose.setContent {
            val rig = rememberCompanionRig(CompanionKind.ROBOT, CompanionColor.ORIGINAL)!!
            ClocklingsTheme {
                GalleryColumn {
                    looks.forEach { (color, outfit) ->
                        CompanionView(
                            CompanionKind.ROBOT,
                            outfit,
                            Modifier.size(width = LARGE_WIDTH.dp, height = (LARGE_WIDTH * TILE_HEIGHT / TILE_WIDTH).dp),
                            color = color,
                            animated = false,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        blinking.forEach { outfit ->
                            RigCanvas(
                                rig,
                                outfit,
                                closed,
                                Modifier.size(
                                    width = (LARGE_WIDTH / 2).dp,
                                    height = (LARGE_WIDTH * TILE_HEIGHT / TILE_WIDTH / 2).dp,
                                ),
                            )
                        }
                    }
                }
            }
        }
        save("robot-large")
    }

    @Test
    fun dragonInEveryItem() = companionInEveryItem(CompanionKind.DRAGON)

    /** Two large dragon looks: Original with wizard hat and pendant, Blue with crown and sparkle necklace. */
    @Test
    fun dragonSampleLooks() =
        companionLarge(
            CompanionKind.DRAGON,
            CompanionColor.ORIGINAL to
                Outfit().with(Slot.HEAD, "dragon-head-wizard-hat").with(Slot.NECK, "dragon-neck-pendant"),
            CompanionColor.BLUE to
                Outfit().with(Slot.HEAD, "dragon-head-crown").with(Slot.NECK, "dragon-neck-sparkles"),
        )

    @Test
    fun dinosaurInEveryItem() = companionInEveryItem(CompanionKind.DINOSAUR)

    /** Two large dinosaur looks: Original with the ice cream and binoculars, Blue with party hat and the swim ring. */
    @Test
    fun dinosaurSampleLooks() =
        companionLarge(
            CompanionKind.DINOSAUR,
            CompanionColor.ORIGINAL to
                Outfit().with(Slot.EXTRA, "dinosaur-extra-ice-cream").with(Slot.NECK, "dinosaur-neck-binoculars"),
            CompanionColor.BLUE to
                Outfit().with(Slot.HEAD, "dinosaur-head-party-hat").with(Slot.EXTRA, "dinosaur-extra-swim-ring"),
        )

    @Test
    fun bunnyInEveryItem() = companionInEveryItem(CompanionKind.BUNNY)

    /** Two large bunny looks: Original with sun hat and ribbon, Blue with top hat and bow tie. */
    @Test
    fun bunnySampleLooks() =
        companionLarge(
            CompanionKind.BUNNY,
            CompanionColor.ORIGINAL to
                Outfit().with(Slot.HEAD, "bunny-head-sun-hat").with(Slot.NECK, "bunny-neck-ribbon"),
            CompanionColor.BLUE to
                Outfit().with(Slot.HEAD, "bunny-head-top-hat").with(Slot.NECK, "bunny-neck-bow-tie"),
        )

    @Test
    fun unicornInEveryItem() = companionInEveryItem(CompanionKind.UNICORN)

    /** Two large unicorn looks: Original with crown and royal cape, Purple with flower crown and garland. */
    @Test
    fun unicornSampleLooks() =
        companionLarge(
            CompanionKind.UNICORN,
            CompanionColor.ORIGINAL to
                Outfit().with(Slot.HEAD, "unicorn-head-crown").with(Slot.NECK, "unicorn-neck-cape"),
            CompanionColor.PURPLE to
                Outfit().with(Slot.HEAD, "unicorn-head-flower-crown").with(Slot.NECK, "unicorn-neck-lei"),
        )

    @Test
    fun carInEveryItem() = companionInEveryItem(CompanionKind.CAR)

    /** Two large car looks: Original race car with surfboard and sport rims, Blue fire engine with a flag. */
    @Test
    fun carSampleLooks() =
        companionLarge(
            CompanionKind.CAR,
            CompanionColor.ORIGINAL to
                Outfit()
                    .with(Slot.MODEL, "car-model-race")
                    .with(Slot.ROOF, "car-roof-surfboard")
                    .with(Slot.WHEELS, "car-wheels-sport"),
            CompanionColor.BLUE to
                Outfit()
                    .with(Slot.MODEL, "car-model-fire-engine")
                    .with(Slot.ROOF, "car-roof-flag")
                    .with(Slot.FRONT, "car-front-plate"),
        )

    /**
     * Saves `<kind>-colors` (the bare companion in every color), `<kind>-items` (the companion in the
     * Original color wearing exactly one item per tile, with the item name underneath) and `<kind>-crops`
     * (the same items as wardrobe thumbnails, cropped to the item in the wardrobe's art size).
     */
    private fun companionInEveryItem(kind: CompanionKind) {
        val page = mutableStateOf(Page.COLORS)
        val items = Wardrobe.itemsFor(kind)
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    if (page.value != Page.COLORS) {
                        val cropped = page.value == Page.CROPS
                        items.chunked(GALLERY_COLUMNS).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { ItemTile(kind, it, cropped) }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            kind.colorOptions().forEach {
                                CompanionView(
                                    kind,
                                    Outfit(),
                                    Modifier.size(width = TILE_WIDTH.dp, height = TILE_HEIGHT.dp),
                                    color = it,
                                    animated = false,
                                )
                            }
                        }
                    }
                }
            }
        }
        val name = kind.name.lowercase()
        save("$name-colors")
        page.value = Page.ITEMS
        compose.waitForIdle()
        save("$name-items")
        page.value = Page.CROPS
        compose.waitForIdle()
        save("$name-crops")
    }

    /** One gallery tile: [kind] wearing only [item], cropped to the item when [cropped], and its name. */
    @Composable
    private fun ItemTile(
        kind: CompanionKind,
        item: Item,
        cropped: Boolean,
    ) {
        Column(Modifier.width(TILE_WIDTH.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CompanionView(
                kind,
                Outfit().previewFor(item),
                if (cropped) {
                    Modifier.size(SkDimens.ItemWindow)
                } else {
                    Modifier.size(width = TILE_WIDTH.dp, height = TILE_HEIGHT.dp)
                },
                color = CompanionColor.ORIGINAL,
                animated = false,
                cropItem = item.takeIf { cropped },
            )
            Text(
                stringResource(itemName(item.id)),
                color = SkColors.Ink,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }

    /** Saves `<kind>-large`: each look about 300 dp wide, one under the other. */
    private fun companionLarge(
        kind: CompanionKind,
        vararg looks: Pair<CompanionColor, Outfit>,
    ) {
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    looks.forEach { (color, outfit) ->
                        CompanionView(
                            kind,
                            outfit,
                            Modifier.size(width = LARGE_WIDTH.dp, height = (LARGE_WIDTH * TILE_HEIGHT / TILE_WIDTH).dp),
                            color = color,
                            animated = false,
                        )
                    }
                }
            }
        }
        save("${kind.name.lowercase()}-large")
    }

    @Test
    fun clockVariants() {
        compose.setContent {
            ClocklingsTheme {
                GalleryColumn {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFace(ClockCommand(205), Modifier.size(190.dp), help = false)
                        ClockFace(ClockCommand(205), Modifier.size(190.dp), help = true, interactive = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFace(ClockCommand(930), Modifier.size(220.dp), period = 1440, help = true)
                        ClockFace(ClockCommand(1290), Modifier.size(220.dp), period = 1440, help = false)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClockFace(
                            ClockCommand(195),
                            Modifier.size(220.dp),
                            highlight = Hand.HOUR,
                            under = listOf(Mark.Sector(0, 15, MarkColor.HALF), Mark.NumberGlow(3), Mark.TickGlow(7)),
                            over = listOf(Mark.Bubble(5, "5"), Mark.Bubble(10, "10")),
                        )
                        ClockFace(ClockCommand(555), Modifier.size(120.dp), mini = true)
                    }
                }
            }
        }
        save("kello")
    }

    /**
     * Window capture waits only briefly for a redraw and for PixelCopy; a loaded emulator can miss that
     * (ComposeTimeoutException, or an AssertionError from PixelCopy), so try a few times.
     */
    private fun capture(): ImageBitmap {
        repeat(CAPTURE_ATTEMPTS - 1) {
            try {
                return compose.onNodeWithTag("galleria").captureToImage()
            } catch (ignored: ComposeTimeoutException) {
                compose.waitForIdle()
            } catch (ignored: AssertionError) {
                compose.waitForIdle()
            }
        }
        return compose.onNodeWithTag("galleria").captureToImage()
    }

    @Composable
    private fun RigCanvas(
        rig: CompanionRig,
        outfit: Outfit,
        pose: CompanionPose,
        modifier: Modifier = Modifier,
    ) {
        Canvas(modifier) {
            val scale = minOf(size.width / rig.width, size.height / rig.height)
            drawContext.transform.scale(scale, scale, Offset.Zero)
            with(rig) { draw(CompanionColor.ORIGINAL, outfit, pose) }
        }
    }

    @Composable
    private fun GalleryColumn(content: @Composable ColumnScope.() -> Unit) {
        Column(
            Modifier.testTag("galleria").background(SkColors.Bg).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }

    private enum class Page { COLORS, ITEMS, CROPS }

    private companion object {
        const val CAPTURE_ATTEMPTS = 4
        const val GALLERY_COLUMNS = 4
        const val TILE_WIDTH = 90
        const val TILE_HEIGHT = 112
        const val LARGE_WIDTH = 300
        const val BARE_SCALE = 3f
    }
}
