package com.ciy.plugin

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ciy.plugin.modle.ApiBean
import com.ciy.plugin.modle.ApiInfoBean
import com.ciy.plugin.modle.JsonSchemaBean
import com.ciy.plugin.modle.ProjectInfoBean
import com.ciy.plugin.ui.AnalysisApiListProgressDialog
import com.ciy.plugin.ui.InputUrlDialog
import com.ciy.plugin.ui.SelectApiDialog
import com.ciy.plugin.utils.ApiServiceGenerate
import com.ciy.plugin.utils.URLConstantGenerate
import com.ciy.plugin.utils.URLConstantGenerate.propertyMap
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
        propertyMap.clear()
        // 生成 URLConstant.kt
        val urlConstantClassName = "URLConstant"
        val urlConstantFile = URLConstantGenerate.createURLConstant(urlConstantClassName, rootDir, packName, apiInfoList)
        urlConstantFile.writeTo(rootDir)
        // 生成ApiService.kt
        val apiSeriviceClassName = "ApiService"
        val apiServiceFile = ApiServiceGenerate.createApiService(apiSeriviceClassName, packName, urlConstantFile, apiInfoList)
        apiServiceFile.writeTo(rootDir)
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