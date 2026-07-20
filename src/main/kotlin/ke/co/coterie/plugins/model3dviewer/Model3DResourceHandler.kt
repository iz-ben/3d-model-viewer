package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.diagnostic.Logger
import org.cef.callback.CefCallback
import org.cef.callback.CefResourceReadCallback
import org.cef.callback.CefResourceSkipCallback
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.misc.IntRef
import org.cef.misc.LongRef
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
) : CefResourceHandlerAdapter() {

    private var stream: InputStream? = null

    private fun openStream() {
        val factory = streamFactory ?: return
        // Guard against a repeated open()/processRequest() on the same handler
        // leaking the previous stream.
        closeStream()
        stream = try {
            BufferedInputStream(factory())
        } catch (e: Exception) {
            LOG.warn("Failed to open resource stream", e)
            null
        }
    }

    /**
     * Old JCEF API. Still required at runtime: the 2026.2+ out-of-process JCEF
     * ("cef_server") bridges resource handlers over Thrift using processRequest/
     * readResponse, not open/read. Deprecated, but not optional.
     */
    override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
        openStream()
        callback?.Continue()
        return true
    }

    /** New JCEF API (in-process path): open the resource and handle synchronously. */
    override fun open(request: CefRequest?, handleRequest: BoolRef?, callback: CefCallback?): Boolean {
        openStream()
        // Handle the request immediately; getResponseHeaders is called next.
        handleRequest?.set(true)
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
        // -1 so CEF streams until read() signals completion.
        responseLength?.set(if (contentLength in 0..Int.MAX_VALUE.toLong()) contentLength.toInt() else -1)
    }

    /** New JCEF API (in-process path). */
    override fun read(
        dataOut: ByteArray?,
        bytesToRead: Int,
        bytesRead: IntRef?,
        callback: CefResourceReadCallback?,
    ): Boolean = readInto(dataOut, bytesToRead, bytesRead)

    /** Old JCEF API. Required by the 2026.2+ out-of-process JCEF bridge (see [processRequest]). */
    override fun readResponse(
        dataOut: ByteArray?,
        bytesToRead: Int,
        bytesRead: IntRef?,
        callback: CefCallback?,
    ): Boolean = readInto(dataOut, bytesToRead, bytesRead)

    private fun readInto(dataOut: ByteArray?, bytesToRead: Int, bytesRead: IntRef?): Boolean {
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

    /** New JCEF API (2026.2+): skip forward in the stream (e.g. for range requests). */
    override fun skip(bytesToSkip: Long, bytesSkipped: LongRef?, callback: CefResourceSkipCallback?): Boolean {
        val s = stream
        if (s == null || bytesToSkip < 0) {
            bytesSkipped?.set(ERR_FAILED)
            return false
        }
        return try {
            var remaining = bytesToSkip
            val scratch = ByteArray(8 * 1024)
            while (remaining > 0) {
                val skipped = s.skip(remaining)
                if (skipped > 0) {
                    remaining -= skipped
                    continue
                }
                // skip() may return 0 without being at EOF; fall back to reading.
                val toRead = minOf(remaining, scratch.size.toLong()).toInt()
                val n = s.read(scratch, 0, toRead)
                if (n <= 0) break
                remaining -= n
            }
            bytesSkipped?.set(bytesToSkip - remaining)
            true
        } catch (e: Exception) {
            LOG.warn("Failed while skipping resource", e)
            bytesSkipped?.set(ERR_FAILED)
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

        // CEF net error code set on the bytesSkipped LongRef to signal skip() failure.
        private const val ERR_FAILED = -2L

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
