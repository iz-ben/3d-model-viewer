package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Service that tracks GlbViewer instances per file and manages
 * the currently active viewer based on editor selection.
 */
@Service(Service.Level.PROJECT)
class GlbViewerService(private val project: Project) : Disposable {

    private val viewersByFile = mutableMapOf<String, GlbViewer>()
    private val viewerChangeListeners = mutableListOf<(VirtualFile?, GlbViewer?) -> Unit>()

    private var currentFile: VirtualFile? = null
    private var messageBusConnection = project.messageBus.connect()

    init {
        // Listen for file editor selection changes
        messageBusConnection.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
                    val newFile = event.newFile
                    if (newFile != null && newFile.extension?.lowercase() == "glb") {
                        currentFile = newFile
                        val viewer = viewersByFile[newFile.path]
                        notifyViewerChanged(newFile, viewer)
                    } else if (newFile == null || newFile.extension?.lowercase() != "glb") {
                        // No GLB file selected
                        currentFile = null
                        notifyViewerChanged(null, null)
                    }
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    if (file.extension?.lowercase() == "glb") {
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
    fun registerViewer(file: VirtualFile, viewer: GlbViewer) {
        viewersByFile[file.path] = viewer
        // If this is the currently active file, notify listeners
        if (currentFile?.path == file.path) {
            notifyViewerChanged(file, viewer)
        }
    }

    /**
     * Unregister a viewer for a specific file.
     */
    fun unregisterViewer(file: VirtualFile) {
        viewersByFile.remove(file.path)
    }

    /**
     * Get the viewer for the currently selected GLB file.
     */
    fun getCurrentViewer(): GlbViewer? {
        return currentFile?.let { viewersByFile[it.path] }
    }

    /**
     * Get the currently selected GLB file.
     */
    fun getCurrentFile(): VirtualFile? = currentFile

    /**
     * Get the viewer for a specific file.
     */
    fun getViewerForFile(file: VirtualFile): GlbViewer? {
        return viewersByFile[file.path]
    }

    /**
     * Add a listener for viewer changes (when user switches between GLB files).
     */
    fun addViewerChangeListener(listener: (VirtualFile?, GlbViewer?) -> Unit) {
        viewerChangeListeners.add(listener)
    }

    /**
     * Remove a viewer change listener.
     */
    fun removeViewerChangeListener(listener: (VirtualFile?, GlbViewer?) -> Unit) {
        viewerChangeListeners.remove(listener)
    }

    private fun notifyViewerChanged(file: VirtualFile?, viewer: GlbViewer?) {
        viewerChangeListeners.forEach { it(file, viewer) }
    }

    override fun dispose() {
        messageBusConnection.disconnect()
        viewersByFile.clear()
        viewerChangeListeners.clear()
    }

    companion object {
        fun getInstance(project: Project): GlbViewerService {
            return project.getService(GlbViewerService::class.java)
        }
    }
}
