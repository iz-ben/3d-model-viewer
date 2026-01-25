package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.Icon

class GlbWireframeWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    companion object {
        var wireframeEnabled = false
        var currentViewer: GlbViewer? = null
    }

    override fun ID(): String = "GlbViewerWireframeWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String = if (wireframeEnabled) "[x] Wireframe" else "[  ] Wireframe"

    override fun getAlignment(): Float = 0f

    override fun getTooltipText(): String = "Toggle wireframe mode"

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        wireframeEnabled = !wireframeEnabled
        currentViewer?.toggleWireframe(wireframeEnabled)
        myStatusBar?.updateWidget(ID())
    }
}
