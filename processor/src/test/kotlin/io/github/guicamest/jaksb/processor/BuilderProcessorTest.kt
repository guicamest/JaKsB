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
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.math.BigDecimal
import java.math.BigInteger
import java.util.stream.Stream
import javax.xml.datatype.Duration
import javax.xml.datatype.XMLGregorianCalendar
import kotlin.reflect.KClass

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("BuilderProcessor should")
class BuilderProcessorTest {
    // https://en.wikipedia.org/wiki/Jakarta_XML_Binding#Default_data_type_bindings
    private fun propertyTypes() =
        Stream.of(
            String::class,
            BigDecimal::class,
            BigInteger::class,
            XMLGregorianCalendar::class,
            Duration::class,
        )

    @ParameterizedTest
    @MethodSource("propertyTypes")
    fun `generate builder with one required property and a configuration parameter`(propertyType: KClass<*>) {
        val propertyQualifiedName = propertyType.qualifiedName // kotlin.String
        val javaName =
            propertyType.simpleName.takeIf {
                propertyType.qualifiedName?.startsWith("kotlin") ?: false
            } ?: propertyQualifiedName // String

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
                    protected $javaName name;

                    @XmlElement(name = "Age")
                    protected Integer age;
                }
            """,
            expectedGeneratedResultFileName = "a/b/c/Document.kt",
            expectedGeneratedSource = """
                package a.b.c

                import $propertyQualifiedName
                import kotlin.Unit

                public fun Document(name: ${propertyType.simpleName}, configure: Document.() -> Unit = {}): Document = Document().apply {
                    this.name = name
                    configure()
                }
            """,
        )
    }

    @Test
    fun `generate builder with configuration parameter when there are no required fields`() {
        assertGeneratedFile(
            sourceFileName = "DocumentNoneRequired.java",
            source = """
                package a.b.c;
                import jakarta.xml.bind.annotation.XmlAccessType;
                import jakarta.xml.bind.annotation.XmlAccessorType;
                import jakarta.xml.bind.annotation.XmlElement;
                import jakarta.xml.bind.annotation.XmlType;

                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "DocumentNoneRequired", propOrder = {
                    "name",
                    "age"
                })
                public class DocumentNoneRequired {

                    @XmlElement(name = "Name")
                    protected String name;

                    @XmlElement(name = "Age")
                    protected Integer age;
                }
            """,
            expectedGeneratedResultFileName = "a/b/c/DocumentNoneRequired.kt",
            expectedGeneratedSource = """
                package a.b.c

                import kotlin.Unit

                public fun DocumentNoneRequired(configure: DocumentNoneRequired.() -> Unit = {}): DocumentNoneRequired = DocumentNoneRequired().apply {
                    configure()
                }
            """,
        )
    }
}

data class TestSourceFile(
    val filename: String,
    val content: String,
)

private fun assertGeneratedFile(
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
