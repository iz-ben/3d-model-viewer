package ke.co.coterie.plugins.glbviewer

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rd.util.string.print
import org.apache.commons.io.FileUtils

class GlbApplicationListener: AppLifecycleListener {

    companion object {
        var port = -1
    }

    override fun appFrameCreated(commandLineArgs: List<String?>) {
        super.appFrameCreated(commandLineArgs)
        serve3dServer()
    }

    private fun serve3dServer() {
        val serverWar = javaClass.getResourceAsStream("/glbupload-0.0.1-SNAPSHOT.war")
        val serverTempDir = FileUtil.createTempDirectory("glb-upload-server", null, true)
        val serverWarFile = FileUtil.createTempFile(serverTempDir, "glbupload", ".war", true)
        FileUtils.copyInputStreamToFile(serverWar, serverWarFile)
        try {

            // Use ServerSocket to find an available port
            port = java.net.ServerSocket(0).use { it.localPort }

            // Serve the war file
            val processBuilder = ProcessBuilder(
                "java", "-jar", serverWarFile.absolutePath, "--server.port=$port"
            )
            println("Starting GLB upload server...")

            val process = processBuilder.start()
            // Print out output stream for debugging in a non-blocking way
            Thread {
                process.inputStream.bufferedReader().lines().forEach { line ->
                    println("GLB Server: $line")
                }
            }.start();
            println("GLB upload server started.")
            // close the process when the application exits
            Runtime.getRuntime().addShutdownHook(Thread {
                println("GLB upload server shut down")
                process.destroy()
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}