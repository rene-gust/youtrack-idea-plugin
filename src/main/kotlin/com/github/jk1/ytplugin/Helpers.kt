package com.github.jk1.ytplugin

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.text.SimpleDateFormat
import java.util.*

val logger: Logger
    get() = Logger.getInstance("com.github.jk1.ytplugin")

fun String.runAction() {
    val action = ActionManager.getInstance().getAction(this)
    DataManager.getInstance().dataContextFromFocusAsync.onSuccess { context ->
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context)
        action.actionPerformed(event)
    }
}

fun AnActionEvent.whenActive(closure: (project: Project) -> Unit) {
    val project = project
    if (project != null && project.isInitialized && !project.isDisposed) {
        closure.invoke(project)
    }
}

fun Date.format(): String = SimpleDateFormat("dd MMM yyyy HH:mm").format(this)
