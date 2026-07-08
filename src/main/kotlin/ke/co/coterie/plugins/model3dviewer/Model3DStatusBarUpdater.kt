package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager

/**
 * Re-evaluates the availability of the 3D model status bar widgets whenever the
 * selected editor changes, so the widgets only appear when a supported 3D model
 * file is in focus.
 */
class Model3DStatusBarUpdater : FileEditorManagerListener {

    override fun selectionChanged(event: FileEditorManagerEvent) {
        val project = event.manager.project
        val manager = project.getService(StatusBarWidgetsManager::class.java) ?: return
        manager.updateWidget(Model3DViewerWidgetFactory::class.java)
        manager.updateWidget(Model3DAnimationWidgetFactory::class.java)
        manager.updateWidget(Model3DAnimationSelectorWidgetFactory::class.java)
    }
}
