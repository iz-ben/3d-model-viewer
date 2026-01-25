package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager

/**
 * Action to restart the IDE.
 * Used in notifications to prompt users to restart after changing OBJ support settings.
 */
class RestartIdeAction : AnAction("Restart IDE") {
    override fun actionPerformed(e: AnActionEvent) {
        ApplicationManager.getApplication().restart()
    }
}
