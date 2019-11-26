package com.ciy.plugin.utils

import com.ciy.plugin.modle.ApiInfoBean
import com.squareup.kotlinpoet.*
import java.io.File

object URLConstantGenerate {

    val propertyMap = HashMap<ApiInfoBean, PropertySpec>()

    /**
     * 创建URLConstant.kt，记录所有URL
     */
    fun createURLConstant(className:String, rootDir: File, packName: String, apiInfoList: List<ApiInfoBean>): FileSpec {
        propertyMap.clear()
        val urlConstantBuild = TypeSpec.objectBuilder(className)
        val (prefixProperty, suffixProperty) = createPrefixAndSuffixProperty(
            rootDir,
            packName,
            className,
            urlConstantBuild
        )
        apiInfoList.forEach {
            // 添加字段名称
            var propertyName = it.path.replace("/", "_").replace(".", "_").toUpperCase()
            if (propertyName.startsWith("_")) {
                propertyName = propertyName.substring(1)
            }
            val property = PropertySpec.builder(propertyName, String::class.asTypeName(), KModifier.CONST)
                .initializer("\"$${prefixProperty.name}${it.path}$${suffixProperty.name}\"")
                .addKdoc(it.title).build()
            propertyMap[it] = property
            urlConstantBuild.addProperty(property)
        }
        urlConstantBuild.addKdoc("Url常量存放类")
        return FileSpec.builder(packName, className).addType(urlConstantBuild.build()).build()
    }

    /**
     * 创建URLConstant.kt中的前缀和后缀字段
     */
    private fun createPrefixAndSuffixProperty(
        rootDir: File,
        packName: String,
        className: String,
        urlConstantBuild: TypeSpec.Builder
    ): Pair<PropertySpec, PropertySpec> {
        val prefixPropertyName = "PREFIX"
        val suffixPropertyName = "SUFFIX"
        val prefixPropertyValue =
            getClassPropertyValue(getClassFile(rootDir, packName, "$className.kt"), prefixPropertyName, "\"\"")
        val suffixPropertyValue =
            getClassPropertyValue(getClassFile(rootDir, packName, "$className.kt"), suffixPropertyName, "\"\"")
        // 前缀字段
        val prefixProperty =
            PropertySpec.builder(prefixPropertyName, String::class.asTypeName(), KModifier.CONST, KModifier.PRIVATE)
                .addKdoc("URL前缀").initializer(prefixPropertyValue).build()
        // 后缀字段
        val suffixProperty =
            PropertySpec.builder(suffixPropertyName, String::class.asTypeName(), KModifier.CONST, KModifier.PRIVATE)
                .addKdoc("URL后缀").initializer(suffixPropertyValue).build()
        urlConstantBuild.addProperty(prefixProperty)
        urlConstantBuild.addProperty(suffixProperty)
        return Pair(prefixProperty, suffixProperty)
    }

    /**
     * 根据root目录和包名、class名获取class的绝对路径
     */
    private fun getClassFile(rootDir: File, packName: String, className: String): File {
        return File(
            rootDir.absolutePath + File.separator + packName.replace(
                ".",
                File.separator
            ) + File.separator + className
        )
    }

    /**
     * 根据文件路径和字段，获取里面字段value
     */
    private fun getClassPropertyValue(classFile: File, property: String, defaultValue: String): String {
        if (classFile.exists()) {
            val classText = classFile.readText()
            for (matchResult in "$property.*?=(.*)".toRegex().findAll(classText)) {
                if (matchResult.groupValues.size == 2) {
                    return matchResult.groupValues[1].trim()
                }
            }
        }
        return defaultValue
    }
}