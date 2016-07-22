package com.github.jk1.ytplugin.search.rest

import com.github.jk1.ytplugin.common.rest.ResponseLoggerTrait
import com.github.jk1.ytplugin.common.rest.RestClientTrait
import com.github.jk1.ytplugin.search.model.Issue
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.tasks.impl.BaseRepository
import org.apache.commons.httpclient.methods.GetMethod
import java.io.InputStreamReader

class IssuesRestClient(override val project: Project, val repo: BaseRepository) : RestClientTrait, ResponseLoggerTrait {

    fun getIssues(query: String): List<Issue> {
        val method = GetMethod("${repo.url}/rest/issue?filter=${query.urlencoded}")
        method.setRequestHeader("Accept", "application/json")
        try {
            val status = createHttpClient(repo).executeMethod(method)
            if (status == 200) {
                val stream = InputStreamReader(method.responseBodyAsLoggedStream())
                val root = JsonParser().parse(stream).asJsonObject
                return root.getAsJsonArray("issue").map { Issue(it) }
            } else {
                throw RuntimeException(method.responseBodyAsLoggedString())
            }
        } finally {
            method.releaseConnection()
        }
    }
}