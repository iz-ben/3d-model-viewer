package ke.co.coterie.plugins.model3dviewer

import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import kotlin.math.min

/**
 * Serves a fixed byte payload (an app resource or a model/asset file) over the
 * plugin's synthetic viewer origin. A null [data] responds with 404.
 */
class Model3DResourceHandler(
    private val data: ByteArray?,
    private val mimeType: String,
) : CefResourceHandler {

    private var offset = 0

    override fun processRequest(request: CefRequest?, callback: CefCallback?): Boolean {
        callback?.Continue()
        return true
    }

    override fun getResponseHeaders(
        response: CefResponse?,
        responseLength: IntRef?,
        redirectUrl: StringRef?,
    ) {
        if (response == null) return
        val bytes = data
        if (bytes == null) {
            response.status = 404
            responseLength?.set(0)
            return
        }
        response.mimeType = mimeType
        response.status = 200
        // Same-origin app, but be explicit and permissive for module/worker fetches.
        response.setHeaderByName("Access-Control-Allow-Origin", "*", true)
        response.setHeaderByName("Cache-Control", "no-cache", true)
        responseLength?.set(bytes.size)
    }

    override fun readResponse(
        dataOut: ByteArray?,
        bytesToRead: Int,
        bytesRead: IntRef?,
        callback: CefCallback?,
    ): Boolean {
        val bytes = data
        if (bytes == null || dataOut == null || offset >= bytes.size) {
            bytesRead?.set(0)
            return false
        }
        val n = min(bytesToRead, bytes.size - offset)
        System.arraycopy(bytes, offset, dataOut, 0, n)
        offset += n
        bytesRead?.set(n)
        return true
    }

    override fun cancel() {
        // Nothing to release.
    }
}
