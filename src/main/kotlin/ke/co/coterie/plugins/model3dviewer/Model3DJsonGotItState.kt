package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager

/**
 * Tracks whether a project has finished opening, so the 3D-preview Got It tooltip
 * is not shown (and its once-per-user flag not consumed) during the bulk editor
 * restore that happens while the project is still opening. See
 * [Model3DJsonGotItListener].
 */
@Service(Service.Level.PROJECT)
class Model3DJsonGotItState(project: Project) {

    @Volatile
    var isReady: Boolean = false
        private set

    init {
        StartupManager.getInstance(project).runAfterOpened { isReady = true }
    }
}
