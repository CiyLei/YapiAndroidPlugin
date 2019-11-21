package com.ciy.plugin

import com.ciy.plugin.modle.ApiBean
import com.ciy.plugin.modle.ProjectInfoBean
import com.ciy.plugin.ui.AnalysisApiListProgressDialog
import com.ciy.plugin.ui.InputUrlDialog
import com.ciy.plugin.ui.SelectApiDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import sun.security.pkcs11.Secmod

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
            InputUrlDialog(InputUrlDialog.InputUrlDialogListener {
                showSelectApiDialog(p0.project!!, it)
            }).isVisible = true
        }

    }

    /**
     * 选择Api
     */
    fun showSelectApiDialog(project: Project, p: ProjectInfoBean) {
        SelectApiDialog(project, p, SelectApiDialog.SelectApiDialogListener { module, apiBeans ->
            analysisApiList(module, apiBeans)
        }).isVisible = true
    }

    /**
     * 分析Api列表
     */
    fun analysisApiList(module: Module, apiList: List<ApiBean>) {
        AnalysisApiListProgressDialog(apiList).isVisible = true
    }
}