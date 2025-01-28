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
@file:OptIn(KspExperimental::class)

package io.github.guicamest.jaksb.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.CodeBlock
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
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlEnum
import jakarta.xml.bind.annotation.XmlType

class BuilderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val (nonEnums, enums) = findClassesWithXmlType(resolver)

        nonEnums.forEach { type ->
            generateBuilderFileSpecs(type, SingleValueEnums(declarations = enums))
        }
        return emptyList()
    }

    private fun generateBuilderFileSpecs(
        classDeclaration: KSClassDeclaration,
        singleValuedEnums: SingleValueEnums,
    ) {
        val fqName = classDeclaration.toClassName()
        logger.info("Generating builder...", classDeclaration)
        val packageName = fqName.packageName
        val onlyName = fqName.simpleName

        val requiredFields =
            classDeclaration
                .getAllProperties()
                .filter(::isRequiredXmlElement)
                .map { property ->
                    ParameterSpec
                        .builder(
                            name = property.simpleName.asString(),
                            type = property.type.toTypeName(),
                        ).build()
                }

        val builderFunction =
            FunSpec
                .builder(onlyName)
                .addOriginatingKSFile(classDeclaration.containingFile!!)
        requiredFields.forEach {
            if (singleValuedEnums.find(it.type) !is SingleValueEnum) builderFunction.addParameter(it)
        }

        val funBody =
            CodeBlock.builder().run {
                beginControlFlow("return %T().apply", fqName)
                requiredFields.forEach { field ->
                    val name = field.name
                    when (val result = singleValuedEnums.find(field.type)) {
                        is SingleValueEnum -> addStatement("this.$name = %T", result.onlyValue)
                        else -> addStatement("this.$name = $name")
                    }
                }
                addStatement("configure()")
                endControlFlow()
                build()
            }

        FileSpec
            .builder(packageName, onlyName)
            .addFunction(
                builderFunction
                    .addParameter(parameterSpecForConfigure(classDeclaration))
                    .returns(fqName)
                    .addCode(funBody)
                    .build(),
            ).build()
            .writeTo(codeGenerator, Dependencies(true))
        logger.info("Generated builder", classDeclaration)
    }

    private fun findClassesWithXmlType(
        resolver: Resolver,
    ): Pair<Set<KSClassDeclaration>, List<KSClassDeclaration>> {
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
        return (nonEnums.toSet() to enums)
    }

    private fun parameterSpecForConfigure(classDeclaration: KSClassDeclaration): ParameterSpec {
        val fqType = classDeclaration.asType(emptyList()).toTypeName()
        val configureType: TypeName =
            LambdaTypeName.get(
                receiver = fqType,
                returnType = typeNameOf<Unit>(),
            )

        return ParameterSpec
            .builder("configure", configureType)
            .defaultValue("{}")
            .build()
    }

    private fun isRequiredXmlElement(ksPropertyDeclaration: KSPropertyDeclaration): Boolean {
        val annotationsByType = ksPropertyDeclaration.getAnnotationsByType(XmlElement::class)
        return annotationsByType.firstOrNull()?.required == true
    }
}

sealed interface FindSingleValueEnumResult

data object Other : FindSingleValueEnumResult

data class SingleValueEnum(
    val onlyValue: TypeName,
) : FindSingleValueEnumResult

class SingleValueEnums(
    private val known: Map<TypeName, SingleValueEnum>,
) {
    fun find(type: TypeName): FindSingleValueEnumResult = known[type] ?: Other

    companion object {
        operator fun invoke(declarations: List<KSClassDeclaration>): SingleValueEnums =
            SingleValueEnums(
                declarations.fold(emptyMap()) { current, enumType ->
                    val enumValues = enumType.declarations.filterIsInstance<KSClassDeclaration>()
                    if (enumValues.count() != 1) return@fold current

                    // .... to a.b.c.Code.SE_CRET
                    val entryToAdd =
                        enumType.asType(emptyList()).toTypeName() to
                            SingleValueEnum(enumValues.first().toClassName())
                    current + entryToAdd
                },
            )
    }
}
