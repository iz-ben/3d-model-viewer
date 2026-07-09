package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.diagnostic.Logger
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * Serves a resource (a bundled app file or an on-disk model/asset) over the
 * plugin's synthetic viewer origin by streaming it in chunks, so large `.glb`
 * files and textures are never fully buffered in memory. A handler with no
 * content ([contentLength] < 0) responds with 404.
 */
class Model3DResourceHandler private constructor(
    private val mimeType: String,
    private val contentLength: Long,
    private val streamFactory: (() -> InputStream)?,
) : CefResourceHandler {

    private var stream: InputStream? = null

    override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
        val factory = streamFactory
        if (factory != null) {
            stream = try {
                BufferedInputStream(factory())
            } catch (e: Exception) {
                LOG.warn("Failed to open resource stream", e)
                null
            }
        }
        callback?.Continue()
        return true
    }

    override fun getResponseHeaders(
        response: CefResponse?,
        responseLength: IntRef?,
        redirectUrl: StringRef?,
    ) {
        if (response == null) return
        if (contentLength < 0 || stream == null) {
            response.status = 404
            responseLength?.set(0)
            return
        }
        response.mimeType = mimeType
        response.status = 200
        response.setHeaderByName("Cache-Control", "no-cache", true)
        // responseLength is a 32-bit int; for anything larger (or unknown), pass
        // -1 so CEF streams until readResponse signals completion.
        responseLength?.set(if (contentLength in 0..Int.MAX_VALUE.toLong()) contentLength.toInt() else -1)
    }

    override fun readResponse(
        dataOut: ByteArray?,
        bytesToRead: Int,
        bytesRead: IntRef?,
        callback: CefCallback?,
    ): Boolean {
        val s = stream
        if (s == null || dataOut == null) {
            bytesRead?.set(0)
            return false
        }
        return try {
            val n = s.read(dataOut, 0, bytesToRead)
            if (n <= 0) {
                closeStream()
                bytesRead?.set(0)
                false
            } else {
                bytesRead?.set(n)
                true
            }
        } catch (e: Exception) {
            LOG.warn("Failed while streaming resource", e)
            closeStream()
            bytesRead?.set(0)
            false
        }
    }

    override fun cancel() {
        closeStream()
    }

    private fun closeStream() {
        try {
            stream?.close()
        } catch (_: Exception) {
            // Ignore close failures.
        }
        stream = null
    }

    companion object {
        private val LOG = Logger.getInstance(Model3DResourceHandler::class.java)

        /** A 404 handler. */
        fun notFound(): Model3DResourceHandler = Model3DResourceHandler("text/plain", -1, null)

        /** Serve a small in-memory payload (bundled app resources). */
        fun bytes(data: ByteArray, mimeType: String): Model3DResourceHandler =
            Model3DResourceHandler(mimeType, data.size.toLong(), { ByteArrayInputStream(data) })

        /** Stream a file from disk without buffering it all in memory. */
        fun file(path: Path, mimeType: String): Model3DResourceHandler =
            Model3DResourceHandler(mimeType, Files.size(path), { Files.newInputStream(path) })
    }
}
