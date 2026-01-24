package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.Nls
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class GlbEditor(private val project: Project, private val file: VirtualFile) : FileEditor, UserDataHolderBase() {

    private val glbViewer = GlbViewer(project, file)

    override fun dispose() {

    }

    override fun getFile(): VirtualFile {
        return file
    }

    override fun getComponent(): JComponent {
        return glbViewer.component
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "Glb Editor"
    }

    override fun setState(p0: FileEditorState) {

    }

    override fun isModified(): Boolean {
        return false;
    }

    override fun isValid(): Boolean {
        return file.isValid;
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {

    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {

    }
}