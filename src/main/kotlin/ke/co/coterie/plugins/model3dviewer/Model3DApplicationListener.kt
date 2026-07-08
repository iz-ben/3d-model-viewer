package ke.co.coterie.plugins.model3dviewer

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.io.FileUtils

class Model3DApplicationListener : AppLifecycleListener {

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
        val systemTempDir = java.io.File(System.getProperty("java.io.tmpdir"))
        val serverTempDir = FileUtil.createTempDirectory(systemTempDir, "glb-upload-server", null, true)
        val serverWarFile = FileUtil.createTempFile(serverTempDir, "glbupload", ".war", true)
        FileUtils.copyInputStreamToFile(serverWar, serverWarFile)
        try {

            // Use ServerSocket to find an available port
            port = java.net.ServerSocket(0).use { it.localPort }

            // Launch the bundled server with the IDE's own JVM. Using a bare "java"
            // command relies on PATH, which fails for GUI apps on macOS (they are
            // launched without the user's shell PATH), leaving the viewer stuck on
            // "Loading 3D Model Viewer...". java.home always points at the running
            // JetBrains Runtime and matches the current architecture (e.g. aarch64).
            val javaExecutable = resolveJavaExecutable()

            // Serve the war file
            val processBuilder = ProcessBuilder(
                javaExecutable, "-jar", serverWarFile.absolutePath,
                "--server.port=$port",
                "--spring.servlet.multipart.location=${serverTempDir.absolutePath}"
            )
            processBuilder.directory(serverTempDir)
            // Merge stderr into stdout so startup failures surface in the reader below.
            processBuilder.redirectErrorStream(true)
            println("Starting 3D Model upload server with $javaExecutable ...")

            val process = processBuilder.start()
            // Print out output stream for debugging in a non-blocking way
            Thread {
                process.inputStream.bufferedReader().lines().forEach { line ->
                    println("3D Model Server: $line")
                    // Detect when Spring Boot server is ready
                    if (line.contains("Started") || line.contains("Tomcat started on port")) {
                        notifyServerReady()
                    }
                }
            }.start()
            println("3D Model upload server started.")
            // close the process when the application exits
            Runtime.getRuntime().addShutdownHook(Thread {
                println("3D Model upload server shut down")
                process.destroy()
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Resolves the `java` executable of the currently running IDE JVM (the JetBrains
     * Runtime). Falls back to a bare "java" (PATH lookup) only if the expected binary
     * cannot be found. This avoids depending on the user's shell PATH, which is not
     * available to GUI applications on macOS.
     */
    private fun resolveJavaExecutable(): String {
        val javaHome = System.getProperty("java.home")
        if (!javaHome.isNullOrBlank()) {
            val binName = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                "java.exe"
            } else {
                "java"
            }
            val candidate = java.io.File(javaHome, "bin${java.io.File.separator}$binName")
            if (candidate.canExecute()) {
                return candidate.absolutePath
            }
        }
        return "java"
    }
}
