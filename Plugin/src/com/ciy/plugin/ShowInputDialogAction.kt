package com.ciy.plugin

import com.ciy.plugin.modle.ApiBean
import com.ciy.plugin.modle.ApiInfoBean
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
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
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
        val apiServiceFile = ApiServiceGenerate.createApiService(apiSeriviceClassName, rootDir, packName, urlConstantFile, apiInfoList)
        apiServiceFile.writeTo(rootDir)
    }



    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TypeSpec.classBuilder("T").addModifiers(KModifier.DATA)
        }
    }
}