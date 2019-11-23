package com.ciy.plugin

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ciy.plugin.modle.ApiBean
import com.ciy.plugin.modle.ApiInfoBean
import com.ciy.plugin.modle.JsonSchemaBean
import com.ciy.plugin.modle.ProjectInfoBean
import com.ciy.plugin.ui.AnalysisApiListProgressDialog
import com.ciy.plugin.ui.InputUrlDialog
import com.ciy.plugin.ui.SelectApiDialog
import com.google.gson.Gson
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import com.squareup.kotlinpoet.*
import java.io.File

/**
 * 显示输入框动作
 */
class ShowInputDialogAction : AnAction() {

    init {
        isEnabledInModalContext = true
    }

    private val urlConstantMap = HashMap<ApiInfoBean, PropertySpec>()

    /**
     * 点击图标
     */
    override fun actionPerformed(p0: AnActionEvent) {
        if (!ApplicationManager.getApplication().isHeadlessEnvironment && p0.project != null) {
            InputUrlDialog(p0.project, InputUrlDialog.InputUrlDialogListener {
                showSelectApiDialog(p0.project!!, it)
            }).isVisible = true
        }

    }

    /**
     * 选择Api
     */
    fun showSelectApiDialog(project: Project, p: ProjectInfoBean) {
        SelectApiDialog(project, p, SelectApiDialog.SelectApiDialogListener { module, packName, apiBeans ->
            analysisApiList(module, packName, apiBeans)
        }).isVisible = true
    }

    /**
     * 分析Api列表
     */
    fun analysisApiList(module: Module, packName: String, apiList: List<ApiBean>) {
        AnalysisApiListProgressDialog(apiList, AnalysisApiListProgressDialog.AnalysisApiListProgressDialogListener {
            // 找到模块源代码存放的根目录
            val javaSrc: VirtualDirectoryImpl? =
                module.rootManager.getSourceRoots(false).find { it2 -> it2.name == "java" } as? VirtualDirectoryImpl
            if (javaSrc != null) {
                val rootDir = File(javaSrc.path)
                if (!rootDir.exists()) {
                    rootDir.mkdirs()
                }
                generateSourceCode(rootDir, packName, it)
            }
        }).isVisible = true
    }

    /**
     * 生成源代码
     */
    fun generateSourceCode(rootDir: File, packName: String, apiInfoList: List<ApiInfoBean>) {
        urlConstantMap.clear()
        createURLConstant(rootDir, packName, apiInfoList)
    }

    /**
     * 创建URLConstant.kt，记录所有URL
     */
    private fun createURLConstant(rootDir: File, packName: String, apiInfoList: List<ApiInfoBean>) {
        val className = "URLConstant"
        val urlConstantBuild = TypeSpec.objectBuilder(className)

        val (prefixProperty, suffixProperty) = createPrefixAndSuffixProperty(
            rootDir,
            packName,
            className,
            urlConstantBuild
        )
        apiInfoList.forEach {
            // 添加字段名称
            var propertyName = it.path.replace("/", "_").toUpperCase()
            if (propertyName.startsWith("_")) {
                propertyName = propertyName.substring(1)
            }
            val property = PropertySpec.builder(propertyName, String::class.asTypeName(), KModifier.CONST)
                .initializer("\"$${prefixProperty.name}${it.path}$${suffixProperty.name}\"")
                .addKdoc(it.title).build()
            urlConstantMap[it] = property
            urlConstantBuild.addProperty(property)
        }
        urlConstantBuild.addKdoc("Url常量存放类")
        FileSpec.builder(packName, className).addType(urlConstantBuild.build()).build().writeTo(rootDir)
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

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
        }

        fun analysisJsonSchema(jsonSchema: JsonSchemaBean, name: String, cacheTypeList: ArrayList<TypeSpec>): Any? {
            when (jsonSchema.type) {
                // 对象
                "object" -> {
                    // 构造方法
                    val constructorFunBuilder = FunSpec.constructorBuilder()
                    // 所有字段
                    val propertyList = ArrayList<PropertySpec>()
                    // 循环所有字段
                    for ((key, value) in jsonSchema.properties) {
                        val result = analysisJsonSchema(value, key, cacheTypeList)
                        if (result is PropertySpec) {
                            constructorFunBuilder.addParameter(key, result.type).addKdoc(value.description ?: "")
                            propertyList.add(result)
                        } else if (result is TypeSpec) {
                            val className = ClassName("", result.name!!)
                            constructorFunBuilder.addParameter(key, className).addKdoc(value.description ?: "")
                            propertyList.add(
                                PropertySpec.builder(key, className).addKdoc(
                                    value.description ?: ""
                                ).initializer(key).build()
                            )
                        }
                    }
                    val typeBuilder = TypeSpec.classBuilder(captureName(name)).addModifiers(KModifier.DATA)
                        .primaryConstructor(constructorFunBuilder.build()).addKdoc(jsonSchema.description ?: "")
                    typeBuilder.addProperties(propertyList)
                    val type = typeBuilder.build()
                    cacheTypeList.add(0, type)
                    return type
                }
                // 列表
                "array" -> {
                    val result = analysisJsonSchema(jsonSchema.items, "${captureName(name)}List", cacheTypeList)
                    if (result is PropertySpec) {
                        val listProperty = LIST.parameterizedBy(result.type)
                        return PropertySpec.builder(name, listProperty).addKdoc(jsonSchema.description ?: "")
                            .initializer(name).build()
                    } else if (result is TypeSpec) {
                        val listProperty = LIST.parameterizedBy(ClassName("", result.name!!))
                        return PropertySpec.builder(name, listProperty).addKdoc(jsonSchema.description ?: "")
                            .initializer(name).build()
                    }
                }
                // 基本类型
                "string", "integer", "boolean", "number" -> {
                    return PropertySpec.builder(name, getType(jsonSchema.type)).initializer(name)
                        .addKdoc(jsonSchema.description ?: "")
                        .build()
                }
            }
            return null
        }

        //首字母大写
        fun captureName(name: String): String {
            if (name.isNotEmpty()) {
                return name.substring(0, 1).toUpperCase() + name.substring(1)
            }
            return name
        }

        fun getType(type: String): ClassName = when (type) {
            "string" -> String::class.asTypeName()
            "integer" -> Int::class.asTypeName()
            "boolean" -> Boolean::class.asTypeName()
            "number" -> Double::class.asTypeName()
            else -> Any::class.asTypeName()
        }
    }
}