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
        return file.extension == "glb"
    }

    override fun createEditor(
        project: Project,
        file: VirtualFile
    ): FileEditor {
        return GlbEditor(project, file)
    }

    override fun getEditorTypeId(): @NonNls String {
        return "GLB_EDITOR"
    }

    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}