package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Service that tracks Model3DViewer instances per file and manages
 * the currently active viewer based on editor selection.
 */
@Service(Service.Level.PROJECT)
class Model3DViewerService(project: Project) : Disposable {

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("glb", "gltf", "obj")

        fun getInstance(project: Project): Model3DViewerService {
            return project.getService(Model3DViewerService::class.java)
        }
    }

    private val viewersByFile = mutableMapOf<String, Model3DViewer>()
    private val viewerChangeListeners = mutableListOf<(VirtualFile?, Model3DViewer?) -> Unit>()

    private var currentFile: VirtualFile? = null
    private var messageBusConnection = project.messageBus.connect()

    init {
        // Initialize from the current selection. The service may be created after the
        // initial editor selection has already happened (e.g. status bar widgets are
        // created at startup with a 3D model already open), in which case we would
        // otherwise miss it and report no current file.
        FileEditorManager.getInstance(project).selectedEditor?.file?.let { file ->
            if (file.extension?.lowercase() in SUPPORTED_EXTENSIONS) {
                currentFile = file
            }
        }

        // Listen for file editor selection changes
        messageBusConnection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                    val newFile = event.newFile
                    if (newFile != null && newFile.extension?.lowercase() in SUPPORTED_EXTENSIONS) {
                        currentFile = newFile
                        val viewer = viewersByFile[newFile.path]
                        notifyViewerChanged(newFile, viewer)
                    } else if (newFile == null || newFile.extension?.lowercase() !in SUPPORTED_EXTENSIONS) {
                        // No 3D model file selected
                        currentFile = null
                        notifyViewerChanged(null, null)
                    }
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    if (file.extension?.lowercase() in SUPPORTED_EXTENSIONS) {
                        viewersByFile.remove(file.path)
                        if (currentFile?.path == file.path) {
                            currentFile = null
                            notifyViewerChanged(null, null)
                        }
                    }
                }
            }
        )
    }

    /**
     * Register a viewer for a specific file.
     */
    fun registerViewer(file: VirtualFile, viewer: Model3DViewer) {
        viewersByFile[file.path] = viewer
        // If this is the currently active file, notify listeners
        if (currentFile?.path == file.path) {
            notifyViewerChanged(file, viewer)
        }
    }

    /**
     * Get the viewer for the currently selected 3D model file.
     */
    fun getCurrentViewer(): Model3DViewer? {
        return currentFile?.let { viewersByFile[it.path] }
    }

    /**
     * Get the currently selected 3D model file.
     */
    fun getCurrentFile(): VirtualFile? = currentFile

    /**
     * Add a listener for viewer changes (when user switches between 3D model files).
     */
    fun addViewerChangeListener(listener: (VirtualFile?, Model3DViewer?) -> Unit) {
        viewerChangeListeners.add(listener)
    }

    /**
     * Remove a viewer change listener.
     */
    fun removeViewerChangeListener(listener: (VirtualFile?, Model3DViewer?) -> Unit) {
        viewerChangeListeners.remove(listener)
    }

    private fun notifyViewerChanged(file: VirtualFile?, viewer: Model3DViewer?) {
        viewerChangeListeners.forEach { it(file, viewer) }
    }

    override fun dispose() {
        messageBusConnection.disconnect()
        viewersByFile.clear()
        viewerChangeListeners.clear()
    }
}
