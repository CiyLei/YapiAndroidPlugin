package com.ciy.plugin

import com.ciy.plugin.modle.ApiBean
import com.ciy.plugin.modle.ApiInfoBean
import com.ciy.plugin.modle.ProjectInfoBean
import com.ciy.plugin.ui.AnalysisApiListProgressDialog
import com.ciy.plugin.ui.InputUrlDialog
import com.ciy.plugin.ui.SelectApiDialog
import com.ciy.plugin.ui.ShowErrorListDialog
import com.ciy.plugin.utils.ApiServiceGenerate
import com.ciy.plugin.utils.BaseResponseGenerate
import com.ciy.plugin.utils.URLConstantGenerate
import com.ciy.plugin.utils.URLConstantGenerate.propertyMap
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.newvfs.impl.VirtualDirectoryImpl
import java.io.File

/**
 * 显示输入框动作
 */
class ShowInputDialogAction : AnAction() {

    init {
        isEnabledInModalContext = true
    }

    companion object {
        /**
         * 生成代码的错误列表
         */
        @JvmStatic
        val generateSourceCodeErrorList = ArrayList<Throwable>()

        @JvmStatic
        val urlConstantClassName = "URLConstant"

        @JvmStatic
        val apiServiceClassName = "ApiService"

        @JvmStatic
        val baseResponseClassName = "BaseResponse"
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
            val javaSrc: VirtualDirectoryImpl? = ModuleRootManager.getInstance(module).sourceRoots.find { it2 -> it2.path.endsWith("src/main/java") } as? VirtualDirectoryImpl
            if (javaSrc != null) {
                val rootDir = File(javaSrc.path)
                if (!rootDir.exists()) {
                    rootDir.mkdirs()
                }
                generateSourceCodeErrorList.clear()
                try {
                    generateSourceCode(rootDir, packName, it)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    generateSourceCodeErrorList.add(e)
                }
                if (generateSourceCodeErrorList.isNotEmpty()) {
                    ShowErrorListDialog(generateSourceCodeErrorList).isVisible = true
                }
            }
        }).isVisible = true
    }

    /**
     * 生成源代码
     */
    fun generateSourceCode(rootDir: File, packName: String, apiInfoList: List<ApiInfoBean>) {
        propertyMap.clear()
        // 生成 URLConstant.kt
        val urlConstantFile = URLConstantGenerate.createURLConstant(urlConstantClassName, rootDir, packName, apiInfoList)
        urlConstantFile.writeTo(rootDir)
        // 生成 BaseResponse.kt
        BaseResponseGenerate.createBaseResponse(baseResponseClassName, rootDir, packName)
        // 生成ApiService.kt
        val apiServiceFile = ApiServiceGenerate.createApiService(apiServiceClassName, rootDir, packName, urlConstantFile, apiInfoList)
        apiServiceFile.writeTo(rootDir)
    }

}