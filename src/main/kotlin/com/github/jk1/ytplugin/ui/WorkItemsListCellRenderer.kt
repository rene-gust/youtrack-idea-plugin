package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.intellij.icons.AllIcons
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.SimpleTextAttributes.STYLE_BOLD
import com.intellij.ui.border.CustomLineBorder
import com.intellij.util.ui.UIUtil
import java.awt.*
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer


class WorkItemsListCellRenderer(
        private val viewportWidthProvider: () -> Int,  private val viewportHeightProvider: () -> Int, repo: YouTrackRepository) : JPanel(BorderLayout()), ListCellRenderer<IssueWorkItem> {

    private val myRepository = repo
    private val topPanel = JPanel(BorderLayout())
    private val summaryPanel = JPanel(BorderLayout())
    private val trackingComments = SimpleColoredComponent()
    private lateinit var issueLink: HyperlinkLabel

    private val complimentaryColor = Color(123, 123, 127)
    private val fgColor = Color(75, 107, 244)
    private val idStyle = STYLE_BOLD


    init {
        summaryPanel.isOpaque = false
        border = CustomLineBorder(JBColor(Gray._220, Gray._85), 0, 0, 1, 0)
        topPanel.isOpaque = false
        topPanel.add(summaryPanel, BorderLayout.WEST)
        add(topPanel, BorderLayout.NORTH)
    }

    override fun getListCellRendererComponent(list: JList<out IssueWorkItem>,
                                              issueWorkItem: IssueWorkItem, index: Int,
                                              isSelected: Boolean, cellHasFocus: Boolean): Component {

        background = UIUtil.getListBackground(false, false)
        fillTrackingInfoLine(issueWorkItem, fgColor)
        return this
    }

    private fun fillTrackingInfoLine(issueWorkItem: IssueWorkItem, fgColor: Color) {
        summaryPanel.removeAll()

        val date = SimpleColoredComponent()
        date.isOpaque = false
        date.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        date.append(issueWorkItem.date.format().substring(0, issueWorkItem.date.format().length - 5), SimpleTextAttributes(idStyle, complimentaryColor))

        val value = SimpleColoredComponent()
        value.isOpaque = false
        value.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        value.icon = AllIcons.Vcs.History
        value.append(issueWorkItem.value, SimpleTextAttributes(idStyle, fgColor))

        val type = SimpleColoredComponent()
        type.isOpaque = false
        type.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        type.append(issueWorkItem.type, SimpleTextAttributes(idStyle, complimentaryColor))

        issueLink = HyperlinkLabel(issueWorkItem.issueId,
                "${myRepository.url}/issue/${issueWorkItem.issueId}")
        issueLink.icon = AllIcons.Actions.MoveTo2

        prepareCommentsForDisplaying(issueWorkItem)

        val panel = JPanel(FlowLayout(FlowLayout.LEFT))
        panel.isOpaque = false
        val panelWidth = 9 * viewportWidthProvider.invoke() / 10
        val panelHeight = viewportHeightProvider.invoke() / 13

        panel.preferredSize = Dimension(panelWidth, panelHeight)
        val datePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        datePanel.preferredSize = Dimension((0.156 * panelWidth).toInt(), panelHeight + 10)
        datePanel.add(date)

        value.alignmentX = Component.RIGHT_ALIGNMENT
        val valuePanel = JPanel(FlowLayout(FlowLayout.LEFT))

        valuePanel.preferredSize = Dimension((0.313 * panelWidth).toInt(), panelHeight + 10)
        valuePanel.alignmentX = Component.RIGHT_ALIGNMENT

        val issueLinkPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        issueLinkPanel.add(issueLink)
        issueLinkPanel.preferredSize = Dimension((0.078 * panelWidth).toInt(), panelHeight + 10)


        if (panelWidth > 1000) {
            datePanel.preferredSize = Dimension((0.156 * panelWidth).toInt(), panelHeight + 10)
            valuePanel.preferredSize = Dimension((0.313 * panelWidth).toInt(), panelHeight + 10)
            issueLinkPanel.preferredSize = Dimension((0.078 * panelWidth).toInt(), panelHeight + 10)

        } else {
            datePanel.preferredSize = Dimension((0.3 * panelWidth).toInt(), panelHeight)
            valuePanel.preferredSize = Dimension((0.4 * panelWidth).toInt(), panelHeight)
            issueLinkPanel.preferredSize = Dimension((0.2 * panelWidth).toInt(), panelHeight)

        }

        datePanel.add(date)
        valuePanel.add(value)
        panel.add(datePanel)
        panel.add(valuePanel)

        if (panelWidth > 500) {

            panel.add(issueLinkPanel)

            if (panelWidth > 1000) {
                val typePanel = JPanel(FlowLayout(FlowLayout.LEFT))
                typePanel.add(type)
                typePanel.preferredSize = Dimension((0.156 * panelWidth).toInt(), panelHeight + 10)
                panel.add(typePanel)

                val trackingCommentsPanel = JPanel(FlowLayout(FlowLayout.LEFT))
                trackingCommentsPanel.add(trackingComments)
                trackingCommentsPanel.preferredSize = Dimension((0.274 * panelWidth).toInt(), panelHeight + 10)
                panel.add(trackingCommentsPanel)
            }
        }

        summaryPanel.add(panel, BorderLayout.CENTER)
    }


    private fun prepareCommentsForDisplaying(issueWorkItem: IssueWorkItem) {
        val viewportWidth = viewportWidthProvider.invoke() / 8

        trackingComments.clear()
        trackingComments.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        trackingComments.isOpaque = false

        val comments = issueWorkItem.comment?.split(" ")?.iterator()
        if (comments != null && issueWorkItem.comment != "") {
            trackingComments.icon = AllIcons.General.Balloon

            while (comments.hasNext() && (viewportWidth > trackingComments.computePreferredSize(false).width)) {
                trackingComments.append(" ${comments.next()}", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
        if (comments != null && issueWorkItem.comment != "") {
            if (comments.hasNext()) {
                trackingComments.append(" …", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
    }

}
