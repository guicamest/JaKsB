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
package io.github.guicamest.jaksb.processor

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("BuilderProcessor should")
class OmitParameterForSingleValueRequiredEnumsTest {
    @Test
    fun `omit parameter in builder for required enums that have only one value`() {
        assertGeneratedFile(
            testSourceFiles =
                arrayOf(
                    TestSourceFile(
                        filename = "Code.java",
                        content = """
                    package a.b.c;

                    import jakarta.xml.bind.annotation.XmlEnum;
                    import jakarta.xml.bind.annotation.XmlType;

                    @XmlType(name = "Code")
                    @XmlEnum
                    public enum Code {

                        SE_CRET;

                        public String value() {
                            return name();
                        }

                        public static Code fromValue(String v) {
                            return valueOf(v);
                        }
                     }
                """,
                    ),
                    TestSourceFile(
                        filename = "Document.java",
                        content = """
                    package a.b.c;
                    import jakarta.xml.bind.annotation.XmlAccessType;
                    import jakarta.xml.bind.annotation.XmlAccessorType;
                    import jakarta.xml.bind.annotation.XmlElement;
                    import jakarta.xml.bind.annotation.XmlType;

                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "Document", propOrder = {
                        "name",
                        "code",
                    })
                    public class Document {

                        @XmlElement(name = "Name")
                        protected String name;

                        @XmlElement(name = "Code", required = true)
                        protected Code code;
                    }
                """,
                    ),
                ),
            expectedGeneratedResultFileName = "a/b/c/Document.kt",
            expectedGeneratedSource = """
                package a.b.c

                import kotlin.Unit

                public fun Document(configure: Document.() -> Unit = {}): Document = Document().apply {
                    this.code = Code.SE_CRET
                    configure()
                }
            """,
        )
    }
}
