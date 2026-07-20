package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Marks [Model3DJsonGotItState] ready once the project has finished opening, so the
 * 3D-preview Got It tooltip is suppressed during the bulk editor restore on startup.
 *
 * Replaces the previously used `StartupManager.runAfterOpened(...)` method, which
 * is marked `@ApiStatus.Internal` and must not be called from plugin code.
 */
class Model3DJsonGotItStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<Model3DJsonGotItState>().markReady()
    }
}
