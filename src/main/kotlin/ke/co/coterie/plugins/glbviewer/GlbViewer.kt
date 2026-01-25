package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
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
import java.io.File

class GlbViewer(val project: Project, val file: VirtualFile): JBCefBrowser() {

    init {
        // Add console message listener to capture JavaScript output
        jbCefClient.addDisplayHandler(object : CefDisplayHandlerAdapter() {
            override fun onConsoleMessage(
                browser: CefBrowser?,
                level: org.cef.CefSettings.LogSeverity?,
                message: String?,
                source: String?,
                line: Int): Boolean {
                println("JS Console [$level] $source:$line - $message")
                return false
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
        val port = GlbApplicationListener.port
        val serverUrl = "http://localhost:${port}/viewer.html?model=${file.name}"
        println("url: $url port: $port serverUrl: $serverUrl")
        loadURL(serverUrl)
    }

    fun toggleWireframe(enabled: Boolean) {
        cefBrowser.executeJavaScript(
            "if (window.toggleWireframe) { window.toggleWireframe($enabled); }",
            cefBrowser.url,
            0
        )
    }

    private fun uploadFile() {
        val client = OkHttpClient()
        println("Virtual File path : ${file.path}")
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart(
            "file", file.name, File(file.path).asRequestBody("application/octet-stream".toMediaType())
        ).build()
        val request = Request.Builder().url("http://localhost:${GlbApplicationListener.port}/api/models/upload")
            .post(body).build()

        val response = client.newCall(request).execute()

        println(response.body?.string())
    }

}