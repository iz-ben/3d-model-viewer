package ke.co.coterie.plugins.model3dviewer

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.SwingConstants

/**
 * A loading indicator shown at the top of the 3D editor while a model downloads
 * and parses. It uses the IntelliJ platform "Loader" (an animated icon plus a
 * progress-text label) and is driven by the loading events the viewer engine
 * streams over the JS bridge (see Model3DViewer + the renderer's LoadingStatus).
 *
 * The loader lives in its own layout region (not overlaid on the JCEF browser)
 * so it renders reliably regardless of the browser's windowed/off-screen mode,
 * and is removed as soon as loading completes, as the platform guidelines
 * require: https://plugins.jetbrains.com/docs/intellij/loader.html
 */
class Model3DLoadingBanner : JBPanel<Model3DLoadingBanner>(BorderLayout()) {

    private val label = JLabel(LOADING, AnimatedIcon.Default(), SwingConstants.LEFT)

    init {
        border = JBUI.Borders.empty(4, 8)
        add(label, BorderLayout.WEST)
    }

    /** Loading has begun; length is not yet known. */
    fun start() {
        show(LOADING)
    }

    /**
     * Download progress. Shows a percentage when the transfer length is known
     * (total > 0); otherwise just the generic loading text.
     */
    fun progress(percent: Int, total: Long) {
        show(if (total > 0) "$LOADING ${percent.coerceIn(0, 100)}%" else LOADING)
    }

    /** Bytes are in; the model is being parsed (duration unknown). */
    fun parsing() {
        show(PARSING)
    }

    /** The model finished loading; remove the loader. */
    fun done() {
        isVisible = false
    }

    /** Loading failed; keep the message visible with an error icon (no spinner). */
    fun error(message: String?) {
        label.icon = AllIcons.General.Error
        label.text = if (message.isNullOrBlank()) FAILED else "$FAILED: $message"
        isVisible = true
    }

    private fun show(text: String) {
        label.icon = AnimatedIcon.Default()
        label.text = text
        isVisible = true
    }

    companion object {
        private const val LOADING = "Loading model"
        private const val PARSING = "Parsing model"
        private const val FAILED = "Failed to load model"
    }
}
