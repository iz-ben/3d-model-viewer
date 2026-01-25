package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

class Model3DAnimationSelectorWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "Model3DViewerAnimationSelectorWidget"

    override fun getDisplayName(): String = "Animation Selector"

    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget {
        return Model3DAnimationSelectorWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {}

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
