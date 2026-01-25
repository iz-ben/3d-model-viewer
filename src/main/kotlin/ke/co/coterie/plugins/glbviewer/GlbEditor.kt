package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class GlbEditor(private val project: Project, private val file: VirtualFile) : FileEditor, UserDataHolderBase() {

    private var glbViewer: GlbViewer? = null

    private val mainPanel = JPanel(BorderLayout()).apply {
        // Show loading message initially
        add(JLabel("Loading GLB Viewer...", SwingConstants.CENTER), BorderLayout.CENTER)
    }

    init {
        // Wait for server to be ready before creating the viewer
        GlbApplicationListener.onServerReady {
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed && file.isValid) {
                    glbViewer = GlbViewer(project, file)
                    mainPanel.removeAll()
                    mainPanel.add(glbViewer!!.component, BorderLayout.CENTER)
                    mainPanel.revalidate()
                    mainPanel.repaint()
                }
            }
        }
    }

    override fun dispose() {
        glbViewer?.dispose()
    }

    override fun getFile(): VirtualFile {
        return file
    }

    override fun getComponent(): JComponent {
        return mainPanel
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return glbViewer?.component
    }

    override fun getName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "Glb Editor"
    }

    override fun setState(p0: FileEditorState) {

    }

    override fun isModified(): Boolean {
        return false
    }

    override fun isValid(): Boolean {
        return file.isValid
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {

    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {

    }
}