package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class Model3DAnimationWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "Model3DViewerAnimationWidget"

    override fun getDisplayName(): String = "Animation Toggle"

    override fun isAvailable(project: Project): Boolean =
        Model3DFileSupport.isSupportedFileInFocus(project)

    override fun createWidget(project: Project): StatusBarWidget {
        return Model3DAnimationWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {}

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
