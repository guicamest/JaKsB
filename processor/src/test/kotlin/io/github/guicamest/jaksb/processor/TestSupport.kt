@file:OptIn(ExperimentalCompilerApi::class)

package io.github.guicamest.jaksb.processor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspLoggingLevels
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File

data class TestSourceFile(
    val filename: String,
    val content: String,
)

fun assertGeneratedFile(
    sourceFileName: String,
    @Language("java") source: String,
    expectedGeneratedResultFileName: String,
    @Language("kotlin") expectedGeneratedSource: String,
) {
    val (result, kspSourcesDir) =
        compile(
            TestSourceFile(sourceFileName, source),
        )
    assertThat(result.exitCode).isEqualTo(OK)

    val generated =
        File(
            kspSourcesDir,
            "kotlin/$expectedGeneratedResultFileName",
        )
    assertThat(generated).exists()
    assertThat(generated.readText().trim()).isEqualTo(expectedGeneratedSource.trimIndent())
}

private fun compile(testSourceFile: TestSourceFile): Pair<JvmCompilationResult, File> {
    val compilation =
        KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true
            kspLoggingLevels = setOf(CompilerMessageSeverity.INFO)
            useKsp2()

            sources =
                listOf(
                    SourceFile.java(testSourceFile.filename, testSourceFile.content),
                )
            symbolProcessorProviders =
                mutableListOf(
                    BuilderProcessorProvider(),
                )
        }
    return compilation.compile() to compilation.kspSourcesDir
}
