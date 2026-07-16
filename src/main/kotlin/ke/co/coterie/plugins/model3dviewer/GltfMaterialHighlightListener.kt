package ke.co.coterie.plugins.model3dviewer

import com.intellij.json.psi.JsonFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.SelectionEvent
import com.intellij.openapi.editor.event.SelectionListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager

/**
 * Listens to caret and selection changes in the glTF JSON editor and highlights the
 * corresponding materials in the 3D preview. The mapping is resolved via
 * [GltfJsonMaterialLocator]; the material index is built off-EDT once the JSON is loaded
 * (see [Model3DTextEditorWithPreview.createJsonTextEditor]).
 */
class GltfMaterialHighlightListener(
    private val project: Project,
    private val modelFile: VirtualFile,
    private val editor: Editor
) : CaretListener, SelectionListener {

    @Volatile
    var materialIndex: GltfMaterialIndex? = null

    private var lastHighlight: Set<Int> = emptySet()

    override fun caretPositionChanged(event: CaretEvent) = update()

    override fun selectionChanged(event: SelectionEvent) = update()

    private fun update() {
        val index = materialIndex ?: return
        val materials = ApplicationManager.getApplication().runReadAction(
            ThrowableComputable<Set<Int>, RuntimeException> {
                val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? JsonFile
                    ?: return@ThrowableComputable emptySet()
                GltfJsonMaterialLocator.materialsForRanges(psiFile, index, currentRanges())
            }
        )

        if (materials == lastHighlight) return
        lastHighlight = materials

        val viewer = Model3DViewerService.getInstance(project).getViewerForFile(modelFile) ?: return
        if (materials.isEmpty()) {
            viewer.clearHighlight()
        } else {
            viewer.highlightMaterials(materials.sorted())
        }
    }

    private fun currentRanges(): List<TextRange> {
        val carets = editor.caretModel.allCarets
        val selections = carets.mapNotNull { caret ->
            if (caret.hasSelection()) TextRange(caret.selectionStart, caret.selectionEnd) else null
        }
        if (selections.isNotEmpty()) {
            return selections
        }
        // No selection: use the caret position(s) as empty ranges.
        return carets.map { TextRange(it.offset, it.offset) }
    }
}
