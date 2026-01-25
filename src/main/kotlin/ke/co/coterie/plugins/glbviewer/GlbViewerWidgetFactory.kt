package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class GlbViewerWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "GlbViewerWireframeWidget"

    override fun getDisplayName(): String = "Wireframe Toggle"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return GlbWireframeWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {}

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
