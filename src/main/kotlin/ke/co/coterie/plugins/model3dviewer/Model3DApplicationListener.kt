package ke.co.coterie.plugins.model3dviewer

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.util.io.FileUtil
import org.apache.commons.io.FileUtils
import java.util.concurrent.TimeUnit

class Model3DApplicationListener : AppLifecycleListener {

    companion object {
        private const val SERVER_START_TIMEOUT_SECONDS = 60L

        var port = -1
        @Volatile
        var isServerReady = false
            private set

        @Volatile
        var serverError: String? = null
            private set

        private data class ReadyListener(val onReady: () -> Unit, val onError: (String) -> Unit)

        private val listeners = mutableListOf<ReadyListener>()
        private val lock = Any()

        /**
         * Register callbacks to be invoked when the server becomes ready or fails to
         * start. If the server is already ready (or already failed), the matching
         * callback is invoked immediately.
         */
        fun onServerReady(onReady: () -> Unit, onError: (String) -> Unit = {}) {
            val action: () -> Unit
            synchronized(lock) {
                action = when {
                    isServerReady -> onReady
                    serverError != null -> {
                        val error = serverError!!
                        ({ onError(error) })
                    }
                    else -> {
                        listeners.add(ReadyListener(onReady, onError))
                        return
                    }
                }
            }
            // Invoke outside the lock so a slow or re-entrant callback cannot deadlock
            // or block other registrations.
            action()
        }

        private fun notifyServerReady() {
            val toNotify: List<ReadyListener>
            synchronized(lock) {
                if (isServerReady || serverError != null) return
                isServerReady = true
                toNotify = listeners.toList()
                listeners.clear()
            }
            toNotify.forEach { listener ->
                try {
                    listener.onReady()
                } catch (e: Exception) {
                    // Best-effort: one failing listener must not stop the others.
                    e.printStackTrace()
                }
            }
        }

        private fun notifyServerFailed(message: String) {
            val toNotify: List<ReadyListener>
            synchronized(lock) {
                if (isServerReady || serverError != null) return
                serverError = message
                toNotify = listeners.toList()
                listeners.clear()
            }
            toNotify.forEach { listener ->
                try {
                    listener.onError(message)
                } catch (e: Exception) {
                    // Best-effort: one failing listener must not stop the others.
                    e.printStackTrace()
                }
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
                // The output stream closed, meaning the process exited. If it never
                // signalled readiness, report the failure so waiting editors can react
                // instead of showing "Loading..." forever.
                if (!isServerReady) {
                    notifyServerFailed("The 3D model viewer server stopped before it was ready.")
                }
            }.start()

            // Guard against a server that starts but never becomes ready (e.g. hangs),
            // so editors don't wait indefinitely.
            Thread {
                if (!process.waitFor(SERVER_START_TIMEOUT_SECONDS, TimeUnit.SECONDS) && !isServerReady) {
                    // Terminate the hung process so it doesn't leak or keep its port bound.
                    // destroy() is not guaranteed to work, so force-kill if it lingers.
                    process.destroy()
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        process.destroyForcibly()
                    }
                    notifyServerFailed("The 3D model viewer server did not start within ${SERVER_START_TIMEOUT_SECONDS}s.")
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
            notifyServerFailed("Could not start the 3D model viewer server: $e")
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
