/*
 * This file is part of "SAP Commerce Developers Toolset" plugin for IntelliJ IDEA.
 * Copyright (C) 2019-2024 EPAM Systems <hybrisideaplugin@epam.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.intellij.idea.plugin.hybris.tools.ccv2.actions

import com.intellij.idea.plugin.hybris.common.utils.HybrisIcons
import com.intellij.idea.plugin.hybris.common.utils.HybrisIcons.CCv2.Actions.FETCH
import com.intellij.idea.plugin.hybris.common.utils.HybrisIcons.CCv2.Build.Actions.SHOW_DETAILS
import com.intellij.idea.plugin.hybris.settings.CCv2Settings
import com.intellij.idea.plugin.hybris.settings.CCv2Subscription
import com.intellij.idea.plugin.hybris.settings.components.ApplicationSettingsComponent
import com.intellij.idea.plugin.hybris.tools.ccv2.CCv2Service
import com.intellij.idea.plugin.hybris.tools.ccv2.dto.CCv2BuildDto
import com.intellij.idea.plugin.hybris.tools.ccv2.dto.CCv2BuildStatus
import com.intellij.idea.plugin.hybris.tools.ccv2.ui.CCv2CreateBuildDialog
import com.intellij.idea.plugin.hybris.tools.ccv2.ui.CCv2DeployBuildDialog
import com.intellij.idea.plugin.hybris.toolwindow.ccv2.CCv2Tab
import com.intellij.idea.plugin.hybris.toolwindow.ccv2.CCv2View
import com.intellij.idea.plugin.hybris.toolwindow.ccv2.CCv2ViewUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

val subscriptionKey = DataKey.create<CCv2Subscription>("subscription")
val buildKey = DataKey.create<CCv2BuildDto>("build")

class CCv2CreateBuildAction : AbstractCCv2Action(
    tab = CCv2Tab.BUILDS,
    text = "Schedule Build",
    icon = HybrisIcons.CCv2.Build.Actions.CREATE
) {
    override fun actionPerformed(e: AnActionEvent) {
        val subscription = e.dataContext.getData(subscriptionKey)
        val build = e.dataContext.getData(buildKey)
        val project = e.project ?: return

        CCv2CreateBuildDialog(project, subscription, build).showAndGet()
    }
}

class CCv2RedoBuildAction(
    private val subscription: CCv2Subscription,
    private val build: CCv2BuildDto
) : AbstractCCv2Action(
    tab = CCv2Tab.BUILDS,
    text = "Redo Build",
    icon = HybrisIcons.CCv2.Build.Actions.REDO
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        CCv2CreateBuildDialog(project, subscription, build).showAndGet()
    }
}

class CCv2DeployBuildAction(
    private val subscription: CCv2Subscription,
    private val build: CCv2BuildDto
) : AbstractCCv2Action(
    tab = CCv2Tab.BUILDS,
    text = "Deploy Build",
    icon = HybrisIcons.CCv2.Build.Actions.DEPLOY
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        CCv2DeployBuildDialog(project, subscription, build).showAndGet()
    }
}

class CCv2DeleteBuildAction(
    private val subscription: CCv2Subscription,
    private val build: CCv2BuildDto
) : AbstractCCv2Action(
    tab = CCv2Tab.BUILDS,
    text = "Delete Build",
    icon = HybrisIcons.CCv2.Build.Actions.DELETE
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete '${build.code}' build within the '$subscription' subscription?",
                "Delete CCv2 Build",
                HybrisIcons.CCv2.Build.Actions.DELETE
            ) != Messages.YES
        ) return

        CCv2Service.getInstance(project).deleteBuild(project, subscription, build)
    }
}

class CCv2FetchBuildsAction : AbstractCCv2FetchAction<CCv2BuildDto>(
    tab = CCv2Tab.BUILDS,
    text = "Fetch Builds",
    icon = FETCH,
    fetch = { project, subscriptions, onStartCallback, onCompleteCallback ->
        CCv2Service.getInstance(project).fetchBuilds(subscriptions, onStartCallback, onCompleteCallback)
    }
)

class CCv2FetchBuildDetailsAction(
    private val subscription: CCv2Subscription,
    private val build: CCv2BuildDto,
    private val onStartCallback: () -> Unit,
    private val onCompleteCallback: (CCv2BuildDto) -> Unit
) : DumbAwareAction("Fetch Build", null, FETCH) {

    private var fetching = false

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        CCv2Service.getInstance(project).fetchBuildWithCode(
            subscription, build.code,
            {
                fetching = true
                e.presentation.text = "Fetching..."

                onStartCallback.invoke()
            },
            { response ->
                invokeLater {
                    fetching = false
                    e.presentation.text = "Fetch Build"

                    onCompleteCallback.invoke(response)
                }
            }
        )
    }

    override fun update(e: AnActionEvent) {
        val isRightPlace = "GoToAction" != e.place
        e.presentation.isEnabled = isRightPlace && isEnabled()
        e.presentation.isVisible = isRightPlace && e.project
            ?.let { CCv2View.getActiveTab(it) == CCv2Tab.BUILDS }
            ?: false
        e.presentation.isEnabled = !fetching && ApplicationSettingsComponent.getInstance().state.ccv2Subscriptions.isNotEmpty()
    }

    fun isEnabled() = ApplicationSettingsComponent.getInstance().state.ccv2Subscriptions.isNotEmpty()
}

class CCv2DownloadBuildLogsAction(
    private val subscription: CCv2Subscription,
    private val build: CCv2BuildDto
) : AbstractCCv2Action(
    tab = CCv2Tab.BUILDS,
    text = "Download Build Logs",
    icon = HybrisIcons.CCv2.Build.Actions.DOWNLOAD_LOGS
) {
    private var processing = false

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        CCv2Service.getInstance(project).downloadBuildLogs(project, subscription, build, onStartCallback(), onCompleteCallback(project))
    }

    private fun onCompleteCallback(project: Project): (Collection<VirtualFile>) -> Unit = {
        invokeLater {
            processing = false

            it.forEach {
                FileEditorManager.getInstance(project).openFile(it, true)
            }
        }
    }

    private fun onStartCallback(): () -> Unit = {
        processing = true
    }

    override fun isEnabled() = !processing && super.isEnabled()
}

class CCv2ShowBuildDetailsAction(
    private val subscription: CCv2Subscription,
    private val build: CCv2BuildDto
) : DumbAwareAction("Show Build Details", null, SHOW_DETAILS) {

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        CCv2ViewUtil.showBuildDetailsTab(project, subscription, build)
    }

}

abstract class AbstractCCv2ShowBuildWithStatusAction(status: CCv2BuildStatus) : AbstractCCv2ShowWithStatusAction<CCv2BuildStatus>(
    CCv2Tab.BUILDS,
    status,
    status.title,
    status.icon
) {

    override fun getStatuses(settings: CCv2Settings) = settings.showBuildStatuses
}

class CCv2ShowDeletedBuildsAction : AbstractCCv2ShowBuildWithStatusAction(CCv2BuildStatus.DELETED)
class CCv2ShowFailedBuildsAction : AbstractCCv2ShowBuildWithStatusAction(CCv2BuildStatus.FAIL)
class CCv2ShowUnknownBuildsAction : AbstractCCv2ShowBuildWithStatusAction(CCv2BuildStatus.UNKNOWN)
class CCv2ShowScheduledBuildsAction : AbstractCCv2ShowBuildWithStatusAction(CCv2BuildStatus.SCHEDULED)
class CCv2ShowBuildingBuildsAction : AbstractCCv2ShowBuildWithStatusAction(CCv2BuildStatus.BUILDING)
class CCv2ShowSuccessBuildsAction : AbstractCCv2ShowBuildWithStatusAction(CCv2BuildStatus.SUCCESS)