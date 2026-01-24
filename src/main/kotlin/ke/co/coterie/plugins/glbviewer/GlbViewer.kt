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

        uploadFile()

        val url: String = file.url
        val port = GlbApplicationListener.port
        val serverUrl = "http://localhost:${port}/viewer.html?model=${file.name}"
        println("url: $url port: $port serverUrl: $serverUrl")
        loadURL(serverUrl)
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