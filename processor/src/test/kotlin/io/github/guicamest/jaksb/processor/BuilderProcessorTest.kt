/*
 * Copyright 2024 guicamest
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

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.kspSourcesDir
import com.tschuchort.compiletesting.kspWithCompilation
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.io.File

@DisplayName("BuilderProcessor should")
class BuilderProcessorTest {
    @Test
    fun `generate builder with one required property and a configuration parameter`() {
        assertGeneratedFile(
            sourceFileName = "Document.java",
            source = """
                package a.b.c;
                import jakarta.xml.bind.annotation.XmlAccessType;
                import jakarta.xml.bind.annotation.XmlAccessorType;
                import jakarta.xml.bind.annotation.XmlElement;
                import jakarta.xml.bind.annotation.XmlType;

                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "Document", propOrder = {
                    "name"
                })
                public class Document {

                    @XmlElement(name = "Name", required = true)
                    protected String name;

                    @XmlElement(name = "Age")
                    protected Integer age;
                }
            """,
            expectedGeneratedResultFileName = "Document.kt",
            expectedGeneratedSource = """
                package a.b.c

                fun Document(
                    name: String,
                    configure: Document.() -> Unit = {}
                ) = Document().apply {
                    this.name = name
                    configure()
                }
            """,
        )
    }
}

private fun assertGeneratedFile(
    sourceFileName: String,
    @Language("java") source: String,
    expectedGeneratedResultFileName: String,
    @Language("kotlin") expectedGeneratedSource: String,
) {
    val compilation =
        KotlinCompilation().apply {
            inheritClassPath = true
            kspWithCompilation = true

            sources =
                listOf(
                    SourceFile.java(sourceFileName, source),
                )
            symbolProcessorProviders =
                mutableListOf(
                    BuilderProcessorProvider(),
                )
        }
    assertThat(compilation.compile().exitCode).isEqualTo(OK)

    val generated =
        File(
            compilation.kspSourcesDir,
            "kotlin/$expectedGeneratedResultFileName",
        )
    assertThat(generated).exists()
    assertThat(
        generated.readText(),
    ).isEqualTo(expectedGeneratedSource.trimIndent())
}
