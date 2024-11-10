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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.typeNameOf
import jakarta.xml.bind.annotation.XmlEnum
import jakarta.xml.bind.annotation.XmlType

class BuilderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val xmlTypes = findClassesWithXmlType(resolver)

        xmlTypes.forEach { type ->
            generateBuilderFileSpecs(type)
        }
        return emptyList()
    }

    private fun generateBuilderFileSpecs(type: KSClassDeclaration) {
        val fqName = type.toClassName()
        val fqType = type.asType(emptyList()).toTypeName()
        val packageName = fqName.packageName
        val onlyName = fqName.simpleName

        val configureType: TypeName =
            LambdaTypeName.get(
                receiver = fqType,
                returnType = typeNameOf<Unit>(),
            )

        val configureLambdaParameter =
            ParameterSpec
                .builder("configure", configureType)
                .defaultValue("{}")
                .build()

        FileSpec
            .builder(packageName, onlyName)
            .addFunction(
                FunSpec
                    .builder(onlyName)
                    .addOriginatingKSFile(type.containingFile!!)
                    .addParameter("name", String::class)
                    .addParameter(configureLambdaParameter)
                    .returns(fqType)
                    .addStatement(
                        """
                        return %T().apply {
                            this.name = name
                            configure()
                        }
                        """.trimIndent(),
                        fqName,
                    ).build(),
            ).build()
            .writeTo(codeGenerator, Dependencies(true))
    }

    @OptIn(KspExperimental::class)
    private fun findClassesWithXmlType(resolver: Resolver): Set<KSClassDeclaration> {
        val xmlTypes =
            resolver
                .getSymbolsWithAnnotation(XmlType::class.qualifiedName.orEmpty())
                .filterIsInstance<KSClassDeclaration>()
//                .filter(KSNode::validate) // because it is java ?
        val (enums, nonEnums) =
            xmlTypes.partition {
                it.isAnnotationPresent(XmlEnum::class)
            }
        logger.logging("Found ${enums.size} classes @ with XmlEnum")
        logger.logging("Found ${nonEnums.size} classes @ with XmlType (non-enum)")
        return nonEnums.toSet()
    }
}
