package com.github.jk1.ytplugin.search.components

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.common.logger
import com.github.jk1.ytplugin.search.model.Issue
import com.github.jk1.ytplugin.search.rest.IssuesRestClient
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.ActionCallback

class IssueStore(val repo: YouTrackServer, private var issues: List<Issue> = listOf()) : Iterable<Issue> {
    private val client = IssuesRestClient(repo)
    private var currentCallback: ActionCallback = ActionCallback.Done()
    private val listeners = mutableSetOf({
        /** todo: fileStore().save() */
    })

    fun update(): ActionCallback {
        if (isUpdating()) {
            return currentCallback
        }
        currentCallback = refresh()
        return currentCallback
    }

    private fun refresh(): ActionCallback {
        logger.debug("Issue store refresh started for project ${repo.project.name} and YouTrack server ${repo.url}")
        val future = ActionCallback()
        object : Task.Backgroundable(repo.project, "Updating issues from server", true, ALWAYS_BACKGROUND) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    logger.debug("Fetching issues for search query: ${repo.defaultSearch}")
                    repo.login()
                    issues = client.getIssues(repo.defaultSearch)
                } catch (e: Exception) {
                    // todo: notification?
                    logger.error("YouTrack issues refresh failed", e)
                }
            }

            override fun onCancel() {
                future.setDone()
                logger.debug("Issue store refresh cancelled for YouTrack server ${repo.url}")
            }

            override fun onSuccess() {
                future.setDone()
                logger.debug("Issue store has been updated for YouTrack server ${repo.url}")
                listeners.forEach { it.invoke() }
            }
        }.queue()
        return future
    }


    fun isUpdating() = !currentCallback.isDone

    fun getAllIssues(): Collection<Issue> = issues

    fun getIssue(index: Int) = issues[index]

    override fun iterator() = issues.iterator()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }
}