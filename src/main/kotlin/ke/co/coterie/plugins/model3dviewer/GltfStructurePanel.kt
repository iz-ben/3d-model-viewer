package ke.co.coterie.plugins.model3dviewer

import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

/**
 * Content of the "Model Explorer" tool window: a tree of the active `.glb` / `.gltf` file's
 * internal structure (asset, scenes, nodes, meshes, materials, accessors, images, …).
 *
 * - The tree is rebuilt off the EDT whenever the active editor switches to a different glTF
 *   model, so it always mirrors the focused file.
 * - Double-clicking a node jumps the model editor's glTF JSON to that element.
 * - Selecting a node that maps to materials highlights them in the 3D preview.
 */
class GltfStructurePanel(
    private val project: Project,
) : com.intellij.openapi.ui.SimpleToolWindowPanel(true, true), Disposable {

    private val treeModel = DefaultTreeModel(null)
    private val tree = Tree(treeModel)

    @Volatile
    private var currentModelPath: String? = null

    // Incremented on the EDT for every scheduled rebuild so a slower, earlier parse
    // can't overwrite the tree produced by a later one (e.g. Refresh clicked twice).
    private var refreshGeneration = 0

    init {
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.cellRenderer = StructureCellRenderer()
        tree.emptyText.text = "Open a .glb or .gltf file to inspect its structure"

        installToolbar()
        setContent(JBScrollPane(tree))

        tree.addTreeSelectionListener { highlightSelection() }
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean = navigateSelection()
        }.installOn(tree)

        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = refreshFromSelection()
            },
        )
        refreshFromSelection()
    }

    private fun installToolbar() {
        val actions = DefaultActionGroup().apply {
            add(object : AnAction("Refresh", "Rebuild the structure tree", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    currentModelPath = null
                    refreshFromSelection()
                }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(object : AnAction("Expand All", null, AllIcons.Actions.Expandall) {
                override fun actionPerformed(e: AnActionEvent) = TreeUtil.expandAll(tree)
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
            add(object : AnAction("Collapse All", null, AllIcons.Actions.Collapseall) {
                override fun actionPerformed(e: AnActionEvent) = TreeUtil.collapseAll(tree, 1)
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            })
        }
        val toolbar = ActionManager.getInstance().createActionToolbar("Model3DGltfStructure", actions, true)
        toolbar.targetComponent = tree
        setToolbar(toolbar.component)
    }

    private fun gltfModelFile(editor: FileEditor?): VirtualFile? =
        Model3DFileSupport.resolveModelFile(editor)?.takeIf { Model3DFileSupport.isGltfStructured(it) }

    private fun refreshFromSelection() {
        val modelFile = gltfModelFile(FileEditorManager.getInstance(project).selectedEditor)
        if (modelFile == null) {
            currentModelPath = null
            tree.emptyText.text = "Open a .glb or .gltf file to inspect its structure"
            treeModel.setRoot(null)
            return
        }
        if (modelFile.path == currentModelPath && treeModel.root != null) return

        currentModelPath = modelFile.path
        val path = modelFile.path
        val name = modelFile.name
        val generation = ++refreshGeneration
        tree.emptyText.text = "Loading $name…"
        treeModel.setRoot(null)

        ApplicationManager.getApplication().executeOnPooledThread {
            val json = runCatching { GltfAssetParser.extractGltfJson(File(path)) }.getOrNull()
            val root = json?.let { GltfStructureModel.build(it) }
            ApplicationManager.getApplication().invokeLater({
                if (project.isDisposed || generation != refreshGeneration || currentModelPath != path) return@invokeLater
                if (root == null) {
                    tree.emptyText.text = "Unable to read glTF structure for $name"
                    treeModel.setRoot(null)
                    return@invokeLater
                }
                treeModel.setRoot(toTreeNode(root))
                expandCategories()
            }, ModalityState.any())
        }
    }

    private fun toTreeNode(node: GltfStructureNode): DefaultMutableTreeNode {
        val treeNode = DefaultMutableTreeNode(node)
        node.children.forEach { treeNode.add(toTreeNode(it)) }
        return treeNode
    }

    private fun expandCategories() {
        val root = treeModel.root as? DefaultMutableTreeNode ?: return
        tree.expandPath(TreePath(root.path))
        for (i in 0 until root.childCount) {
            tree.expandPath(TreePath((root.getChildAt(i) as DefaultMutableTreeNode).path))
        }
    }

    private fun selectedNode(): GltfStructureNode? =
        (tree.lastSelectedPathComponent as? DefaultMutableTreeNode)?.userObject as? GltfStructureNode

    private fun highlightSelection() {
        val file = currentModelFile() ?: return
        val viewer = Model3DViewerService.getInstance(project).getViewerForFile(file) ?: return
        val node = selectedNode()
        when {
            node == null -> viewer.clearHighlight()
            node.meshIndex != null -> viewer.highlightMeshes(listOf(node.meshIndex))
            node.nodeIndex != null -> viewer.highlightNodes(listOf(node.nodeIndex))
            node.materialIndices.isNotEmpty() -> viewer.highlightMaterials(node.materialIndices)
            else -> viewer.clearHighlight()
        }
    }

    private fun navigateSelection(): Boolean {
        val node = selectedNode() ?: return false
        val jsonPath = node.path ?: return false
        val file = currentModelFile() ?: return false
        val editor = FileEditorManager.getInstance(project).getEditors(file)
            .filterIsInstance<Model3DTextEditorWithPreview>().firstOrNull() ?: return false

        val document = editor.jsonTextEditor.editor.document
        // Compute the target offset off the EDT (PSI traversal can be non-trivial
        // for large models), then navigate back on the UI thread. Blocking read
        // actions on the EDT are disallowed/can freeze the UI on recent platforms.
        ReadAction.nonBlocking<Int?> {
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? JsonFile
                ?: return@nonBlocking null
            GltfJsonPathLocator.offsetForPath(psiFile, jsonPath)
        }
            .expireWith(this)
            .finishOnUiThread(ModalityState.defaultModalityState()) { offset ->
                if (offset != null) {
                    FileEditorManager.getInstance(project).openFile(file, true)
                    editor.revealJsonLocation(offset)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
        // The node maps to a JSON element (node.path != null), so treat the
        // double-click as handled and consume it (prevents the tree's default
        // expand/collapse). The async lookup rarely fails; when it does the
        // navigation is simply skipped in finishOnUiThread above.
        return true
    }

    private fun currentModelFile(): VirtualFile? =
        gltfModelFile(FileEditorManager.getInstance(project).selectedEditor)

    override fun dispose() {}

    private class StructureCellRenderer : ColoredTreeCellRenderer() {
        override fun customizeCellRenderer(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ) {
            val node = (value as? DefaultMutableTreeNode)?.userObject as? GltfStructureNode ?: return
            append(node.label)
            node.secondary?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
        }
    }
}
