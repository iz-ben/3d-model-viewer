package ke.co.coterie.plugins.glbviewer

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.io.FileUtils

class GlbApplicationListener: AppLifecycleListener {

    companion object {
        var port = -1
        @Volatile
        var isServerReady = false
            private set

        private val readyCallbacks = mutableListOf<() -> Unit>()
        private val lock = Any()

        /**
         * Register a callback to be invoked when the server is ready.
         * If the server is already ready, the callback is invoked immediately.
         */
        fun onServerReady(callback: () -> Unit) {
            synchronized(lock) {
                if (isServerReady) {
                    callback()
                } else {
                    readyCallbacks.add(callback)
                }
            }
        }

        private fun notifyServerReady() {
            synchronized(lock) {
                isServerReady = true
                readyCallbacks.forEach { it() }
                readyCallbacks.clear()
            }
        }
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
                    // Detect when Spring Boot server is ready
                    if (line.contains("Started") || line.contains("Tomcat started on port")) {
                        notifyServerReady()
                    }
                }
            }.start()
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