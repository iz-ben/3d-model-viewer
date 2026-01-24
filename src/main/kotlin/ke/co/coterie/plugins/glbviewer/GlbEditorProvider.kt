package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls

class GlbEditorProvider: FileEditorProvider, DumbAware {
    override fun accept(
        project: Project,
        file: VirtualFile
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun createEditor(
        project: Project,
        file: VirtualFile
    ): FileEditor {
        TODO("Not yet implemented")
    }

    override fun getEditorTypeId(): @NonNls String {
        TODO("Not yet implemented")
    }

    override fun getPolicy(): FileEditorPolicy {
        TODO("Not yet implemented")
    }
}