package fi.clocklings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fi.clocklings.data.ProgressRepository
import fi.clocklings.domain.ClockAppearance
import fi.clocklings.domain.ClockFrame
import fi.clocklings.domain.ClockPalette
import fi.clocklings.domain.ClockTheme
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.CompanionStyle
import fi.clocklings.domain.CustomClock
import fi.clocklings.domain.HandStyle
import fi.clocklings.domain.NumeralStyle
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Personalization
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Settings
import fi.clocklings.domain.Slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class PersonalizationStorageTest {
    @get:Rule val files = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    @Test
    fun corruptOwnedStoreFallsBackOnReadButCannotClaimSuccessfulWrite() =
        runBlocking {
            val file = files.newFolder().resolve("corrupt.preferences_pb")
            file.writeBytes(byteArrayOf(0x0a, 0x7f, 0x01))
            val job = SupervisorJob()
            try {
                val repository =
                    ProgressRepository(
                        PreferenceDataStoreFactory.create(
                            scope = CoroutineScope(job + Dispatchers.IO),
                            produceFile = { file },
                        ),
                    )
                assertEquals(Progress(), repository.progress.first())
                assertEquals(Settings(), repository.settings.first())
                var failed = false
                try {
                    repository.updateProgress { it.withLevelResult(0, 3) }
                } catch (_: IOException) {
                    failed = true
                }
                assertTrue("ST-03: corrupt store must not report a committed write", failed)
            } finally {
                job.cancelAndJoin()
            }
        }

    @Test
    fun selectionsAndIndividualStylesSurviveClosingAndReopeningRealStore() =
        runBlocking {
            val file = files.newFolder().resolve("preferences.preferences_pb")
            val expected =
                Personalization(
                    companion = CompanionKind.DRAGON,
                    styles =
                        mapOf(
                            CompanionKind.CAT to CompanionStyle(CompanionColor.BLUE),
                            CompanionKind.DRAGON to CompanionStyle(CompanionColor.GREEN),
                        ),
                    clock =
                        ClockAppearance(
                            ClockTheme.CUSTOM,
                            CustomClock(
                                ClockPalette.CORAL,
                                ClockFrame.BLOCKS,
                                NumeralStyle.PLAIN,
                                HandStyle.POINTER,
                            ),
                        ),
                )
            val firstJob = SupervisorJob()
            try {
                val first =
                    ProgressRepository(
                        PreferenceDataStoreFactory.create(
                            scope = CoroutineScope(firstJob + Dispatchers.IO),
                            produceFile = { file },
                        ),
                    )
                first.updateSettings { it.copy(personalization = expected) }
                first.updateProgress {
                    it
                        .withLevelResult(0, 3)
                        .withName(CompanionKind.HUMAN, "Friend")
                        .withName(CompanionKind.CAT, "Miuku")
                        .withOutfit(CompanionKind.CAT, Outfit().with(Slot.HEAD, "cat-head-crown"))
                }
            } finally {
                firstJob.cancelAndJoin()
            }
            val secondJob = SupervisorJob()
            try {
                val second =
                    ProgressRepository(
                        PreferenceDataStoreFactory.create(
                            scope = CoroutineScope(secondJob + Dispatchers.IO),
                            produceFile = { file },
                        ),
                    )
                assertEquals(expected, second.settings.first().personalization)
                assertEquals(3, second.progress.first().totalStars)
                assertEquals("Friend", second.progress.first().nameFor(CompanionKind.HUMAN))
                assertEquals("Miuku", second.progress.first().nameFor(CompanionKind.CAT))
                assertEquals("cat-head-crown", second.progress.first().outfitFor(CompanionKind.CAT)[Slot.HEAD])
            } finally {
                secondJob.cancelAndJoin()
            }
        }
}
