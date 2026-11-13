package fi.clocklings.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val benchmark = MacrobenchmarkRule()

    @Test
    fun coldWithoutPrecompilation() = startup(CompilationMode.None())

    @Test
    fun coldWithPackagedProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilation: CompilationMode) {
        benchmark.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = compilation,
            startupMode = StartupMode.COLD,
            iterations = 7,
            setupBlock = { pressHome() },
            measureBlock = { openHome() },
        )
    }
}
