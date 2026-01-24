package ke.co.coterie.plugins.glbviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser

class GlbViewer(val project: Project, val file: VirtualFile): JBCefBrowser() {

    init {
        val url: String = file.url
        println("url: $url")
        val port = GlbApplicationListener.port
        loadURL("http://localhost:${port}/viewer.html?model=${file.name}")
    }

}