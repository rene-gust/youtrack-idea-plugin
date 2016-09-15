package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.common.components.TaskManagerProxyComponent.Companion.CONFIGURE_SERVERS_ACTION_ID
import com.github.jk1.ytplugin.common.runAction
import com.github.jk1.ytplugin.search.actions.*
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
import com.intellij.ui.components.JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
import com.intellij.util.Function
import com.intellij.util.containers.Convertor
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractListModel
import javax.swing.JComponent
import javax.swing.KeyStroke

class IssueListToolWindowContent(override val project: Project, val repo: YouTrackServer, parent: Disposable) :
        JBLoadingPanel(BorderLayout(), parent), ComponentAware {

    private val issueList: JBList = JBList()
    private val splitter = EditorSplitter()
    private val viewer = IssueViewer(project)
    private val issueListModel: IssueListModel = IssueListModel()
    private lateinit var issueCellRenderer: IssueListCellRenderer

    init {
        val issueListScrollPane = JBScrollPane(issueList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER)
        issueCellRenderer = IssueListCellRenderer({issueListScrollPane.viewport.width})
        issueList.cellRenderer = issueCellRenderer
        splitter.firstComponent = issueListScrollPane
        splitter.secondComponent = viewer
        add(splitter, BorderLayout.CENTER)
        add(createActionPanel(), BorderLayout.WEST)
        setupIssueListActionListeners()
        initIssueListModel()
        ListSpeedSearch(issueList)
    }

    private fun createActionPanel(): JComponent {
        val group = DefaultActionGroup()
        val selectedIssue = {
            when {
                issueList.selectedIndex == -1 -> null
                else -> issueListModel.getElementAt(issueList.selectedIndex)
            }
        }
        group.add(RefreshIssuesAction(repo))
        //group.add(CreateIssueAction()) todo: implement me
        group.add(SetAsActiveTaskAction(selectedIssue))
        group.add(BrowseIssueAction(selectedIssue))
        group.add(AnalyzeStacktraceAction(selectedIssue))
        group.add(ToggleIssueViewAction(project, issueCellRenderer, issueListModel))
        group.add(ActionManager.getInstance().getAction(CONFIGURE_SERVERS_ACTION_ID))
        return ActionManager.getInstance()
                .createActionToolbar("Actions", group, false)
                .component
    }

    private fun setupIssueListActionListeners(){
        // update preview contents upon selection
        issueList.addListSelectionListener {
            val issue = issueListModel.getElementAt(issueList.selectedIndex)
            if (!issue.equals(viewer.currentIssue)) {
                viewer.showIssue(issue)
            }
        }
        // keystrokes to expand/collapse issue preview
        issueList.registerKeyboardAction({ splitter.collapse() },
                KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), JComponent.WHEN_FOCUSED)
        issueList.registerKeyboardAction({ splitter.expand() },
                KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), JComponent.WHEN_FOCUSED)
        issueList.registerKeyboardAction({ splitter.expand() },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED)
        // expand issue preview on click
        issueList.addMouseListener(object: MouseAdapter(){
            override fun mousePressed(e: MouseEvent) {
                if (issueList.model.size > 0) {
                    splitter.expand()
                }
            }
        })
    }

    private fun initIssueListModel() {
        issueList.emptyText.clear()
        startLoading()
        issueList.model = issueListModel
        issueStoreComponent[repo].update().doWhenDone { stopLoading() }
        issueStoreComponent[repo].addListener {
            val placeholder = issueList.emptyText
            placeholder.clear()
            if (issueStoreComponent[repo].getAllIssues().isEmpty()) {
                placeholder.appendText("No issues found ")
                placeholder.appendText("Edit search request", SimpleTextAttributes.LINK_ATTRIBUTES,
                        { CONFIGURE_SERVERS_ACTION_ID.runAction() })
            }
        }
    }

    inner class IssueListModel: AbstractListModel<Issue>() {
        init {
            issueStoreComponent[repo].addListener { update() }
        }

        override fun getElementAt(index: Int) = issueStoreComponent[repo].getIssue(index)

        override fun getSize() = issueStoreComponent[repo].getAllIssues().size

        fun update() { fireContentsChanged(IssueListPanel@this, 0, size) }
    }
}