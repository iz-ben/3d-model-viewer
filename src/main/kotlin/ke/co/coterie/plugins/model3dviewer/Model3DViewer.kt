package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import org.cef.callback.CefRunContextMenuCallback
import org.cef.handler.CefContextMenuHandler
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import java.io.File

class Model3DViewer(val project: Project, val file: VirtualFile) : JBCefBrowser() {

    private val animationListQuery = JBCefJSQuery.create(this as JBCefBrowserBase)
    @Volatile
    private var isPageReady = false
    private val pendingCommands = mutableListOf<String>()

    private val viewerService = Model3DViewerService.getInstance(project)
    private val animationStateService = Model3DAnimationStateService.getInstance(project)

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

        uploadFile()

        val url: String = file.url
        val port = Model3DApplicationListener.port
        val wireframe = if (Model3DWireframeWidget.wireframeEnabled) "true" else "false"
        val serverUrl = "http://localhost:${port}/viewer.html?model=${file.name}&wireframe=${wireframe}"
        println("url: $url port: $port serverUrl: $serverUrl")
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

    fun toggleAnimation(enabled: Boolean) {
        println("toggleAnimation enabled: $enabled")
        executeJavaScript("if (window.toggleAnimation) { window.toggleAnimation($enabled); }")
    }

    fun selectAnimation(animationName: String) {
        println("selecting animation: $animationName")
        executeJavaScript("if (window.selectAnimation) { window.selectAnimation('$animationName'); }")
    }

    private fun uploadFile() {
        val client = OkHttpClient()
        println("Virtual File path : ${file.path}")

        val fileExtension = file.extension?.lowercase()

        if (fileExtension == "gltf") {
            // GLTF files may reference external assets - upload as a bundle
            uploadGltfBundle(client)
        } else {
            // GLB and OBJ files are self-contained - single file upload
            uploadSingleFile(client)
        }
    }

    private fun uploadSingleFile(client: OkHttpClient) {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
            "file", file.name, File(file.path).asRequestBody("application/octet-stream".toMediaType())
        ).build()
        val request = Request.Builder().url("http://localhost:${Model3DApplicationListener.port}/api/models/upload")
            .post(body).build()

        val response = client.newCall(request).execute()
        println(response.body?.string())
    }

    private fun uploadGltfBundle(client: OkHttpClient) {
        val gltfFile = File(file.path)
        val referencedAssets = GltfAssetParser.parseReferencedAssets(gltfFile)

        println("GLTF Bundle: Main file + ${referencedAssets.size} referenced assets")

        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

        // Add the main GLTF file
        bodyBuilder.addFormDataPart(
            "files", file.name, gltfFile.asRequestBody("application/octet-stream".toMediaType())
        )

        // Add all referenced assets with their relative paths
        referencedAssets.forEach { (assetFile, relativePath) ->
            println("GLTF Bundle: Adding asset $relativePath")
            bodyBuilder.addFormDataPart(
                "files", relativePath, assetFile.asRequestBody("application/octet-stream".toMediaType())
            )
        }

        val body = bodyBuilder.build()
        val request = Request.Builder()
            .url("http://localhost:${Model3DApplicationListener.port}/api/models/upload/bulk")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        println(response.body?.string())
    }

}
