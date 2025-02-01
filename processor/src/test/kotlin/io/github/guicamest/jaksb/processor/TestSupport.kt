/*
 * Copyright 2024-2025 guicamest
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    @Language("java") val content: String,
)

fun assertGeneratedFile(
    sourceFileName: String,
    @Language("java") source: String,
    expectedGeneratedResultFileName: String,
    @Language("kotlin") expectedGeneratedSource: String,
) = assertGeneratedFile(
    testSourceFiles = arrayOf(TestSourceFile(sourceFileName, source)),
    expectedGeneratedResultFileName = expectedGeneratedResultFileName,
    expectedGeneratedSource = expectedGeneratedSource,
)

fun assertGeneratedFile(
    vararg testSourceFiles: TestSourceFile,
    expectedGeneratedResultFileName: String,
    @Language("kotlin") expectedGeneratedSource: String,
) {
    val (result, kspSourcesDir) = compile(*testSourceFiles)
    assertThat(result.exitCode).isEqualTo(OK)

    val generated =
        File(
            kspSourcesDir,
            "kotlin/$expectedGeneratedResultFileName",
        )
    assertThat(generated).exists()
    assertThat(generated.readText().trim()).isEqualTo(expectedGeneratedSource.trimIndent())
}

private fun compile(vararg testSourceFiles: TestSourceFile): Pair<JvmCompilationResult, File> {
    val compilation =
        KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true
            kspLoggingLevels = setOf(CompilerMessageSeverity.INFO)
            useKsp2()

            sources =
                testSourceFiles.map { testSourceFile ->
                    SourceFile.java(testSourceFile.filename, testSourceFile.content)
                }
            symbolProcessorProviders =
                mutableListOf(
                    BuilderProcessorProvider(),
                )
        }
    return compilation.compile() to compilation.kspSourcesDir
}
