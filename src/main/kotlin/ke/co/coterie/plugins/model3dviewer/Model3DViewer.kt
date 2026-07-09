package ke.co.coterie.plugins.model3dviewer

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.callback.CefRunContextMenuCallback
import org.cef.handler.CefContextMenuHandler
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.nio.file.Paths

class Model3DViewer(val project: Project, val file: VirtualFile) : JBCefBrowser() {

    private val animationListQuery = JBCefJSQuery.create(this as JBCefBrowserBase)
    @Volatile
    private var isPageReady = false
    private val pendingCommands = mutableListOf<String>()

    private val viewerService = Model3DViewerService.getInstance(project)
    private val animationStateService = Model3DAnimationStateService.getInstance(project)

    // Token that grants the bundled viewer app access to this model's directory
    // (for the model file and its relative assets). Released on dispose.
    private val assetToken: String

    init {
        // Register this viewer with the service
        viewerService.registerViewer(file, this)

        // Set up the animation list callback
        animationListQuery.addHandler { animationsJson ->
            // Parse the JSON array of animation names
            val animations = animationsJson
                .trim()
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }

            // Update animations for this specific file
            animationStateService.updateAvailableAnimations(file, animations)

            // The model (and its animations) are now loaded in the viewer, so this is
            // the correct point to apply the persisted play/pause and selected animation
            // state. Applying it earlier (on page load) has no effect because the model
            // hasn't finished loading yet, which left animations always paused on open.
            val state = animationStateService.getState(file)
            state.selectedAnimation?.let { selectAnimation(it) }
            toggleAnimation(state.isPlaying)
            null
        }

        // Add console message listener to capture JavaScript output
        jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onConsoleMessage(
                browser: CefBrowser?,
                level: org.cef.CefSettings.LogSeverity?,
                message: String?,
                source: String?,
                line: Int
            ): Boolean {
                println("JS Console [$level] $source:$line - $message")
                return false
            }
        }, cefBrowser)

        // Add load handler to inject the callback function after page loads
        jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                // Inject the callback function for sending animation list to Kotlin
                val jsCallback = animationListQuery.inject("animationsJson")
                browser?.executeJavaScript(
                    """
                    window.sendAnimationListToKotlin = function(animations) {
                        var animationsJson = JSON.stringify(animations);
                        $jsCallback
                    };
                    // The model may have finished loading (and published its animation
                    // list) before this bridge was injected. The viewer app stashes the
                    // latest list on window.__model3dAnimations, so re-deliver it now.
                    if (window.__model3dAnimations) {
                        window.sendAnimationListToKotlin(window.__model3dAnimations);
                    }
                    """.trimIndent(),
                    browser.url,
                    0
                )

                // Mark page as ready and execute any pending commands
                isPageReady = true
                synchronized(pendingCommands) {
                    pendingCommands.forEach { js ->
                        browser?.executeJavaScript(js, browser.url, 0)
                    }
                    pendingCommands.clear()
                }
            }
        }, cefBrowser)

        // Add context menu handler to remove the default context menu and provide custom options like zoom options and pause/play
        jbCefClient.addContextMenuHandler(object : CefContextMenuHandler {

            override fun onBeforeContextMenu(
                p0: CefBrowser?,
                p1: CefFrame?,
                p2: CefContextMenuParams?,
                p3: CefMenuModel?
            ) {

            }

            override fun runContextMenu(
                p0: CefBrowser?,
                p1: CefFrame?,
                p2: CefContextMenuParams?,
                p3: CefMenuModel?,
                p4: CefRunContextMenuCallback?
            ): Boolean {
                return false
            }

            override fun onContextMenuCommand(
                p0: CefBrowser?,
                p1: CefFrame?,
                p2: CefContextMenuParams?,
                p3: Int,
                p4: Int
            ): Boolean {
                return false
            }

            override fun onContextMenuDismissed(p0: CefBrowser?, p1: CefFrame?) {
                //TODO("Not yet implemented")
            }
        }, cefBrowser)

        // Serve the bundled viewer app + this model (and its relative assets) over
        // the plugin's synthetic origin — no external server or upload required.
        Model3DSchemeHandlerFactory.ensureRegistered()

        val modelPath = Paths.get(file.path)
        assetToken = Model3DAssetRegistry.register(modelPath)
        val wireframe = Model3DWireframeWidget.wireframeEnabled
        val serverUrl = Model3DSchemeHandlerFactory.viewerUrl(
            assetToken,
            modelPath.fileName.toString(),
            wireframe,
            Model3DAutoRotateWidget.autoRotateEnabled
        )
        println("Loading 3D viewer: $serverUrl (file: ${file.path})")
        loadURL(serverUrl)
    }

    private fun executeJavaScript(js: String) {
        if (isPageReady) {
            cefBrowser.executeJavaScript(js, cefBrowser.url, 0)
        } else {
            synchronized(pendingCommands) {
                pendingCommands.add(js)
            }
        }
    }

    fun toggleWireframe(enabled: Boolean) {
        println("toggleWireframe enabled: $enabled")
        executeJavaScript("if (window.toggleWireframe) { window.toggleWireframe($enabled); }")
    }

    fun toggleAutoRotate(enabled: Boolean) {
        println("toggleAutoRotate enabled: $enabled")
        executeJavaScript("if (window.toggleAutoRotate) { window.toggleAutoRotate($enabled); }")
    }

    fun toggleAnimation(enabled: Boolean) {
        println("toggleAnimation enabled: $enabled")
        executeJavaScript("if (window.toggleAnimation) { window.toggleAnimation($enabled); }")
    }

    fun selectAnimation(animationName: String) {
        println("selecting animation: $animationName")
        // JSON-encode the name so quotes/backslashes in animation names produce a valid,
        // properly escaped JS string literal (prevents broken JS and script injection).
        val encodedName = Gson().toJson(animationName)
        executeJavaScript("if (window.selectAnimation) { window.selectAnimation($encodedName); }")
    }

    fun highlightMaterials(indices: List<Int>) {
        // JSON-encode the index array so it is always a valid JS literal (e.g. [0,2,5]).
        val encoded = Gson().toJson(indices)
        executeJavaScript("if (window.highlightMaterials) { window.highlightMaterials($encoded); }")
    }

    fun clearHighlight() {
        executeJavaScript("if (window.clearMaterialHighlight) { window.clearMaterialHighlight(); }")
    }

    override fun dispose() {
        Model3DAssetRegistry.unregister(assetToken)
        super.dispose()
    }
}
