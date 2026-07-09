package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefApp
import org.cef.CefApp
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.network.CefRequest
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Serves the bundled Svelte/Threlte viewer app and the opened model (plus its
 * relative assets) over a synthetic origin, replacing the previous bundled
 * Spring Boot server. Registered once against JCEF's built-in `http` scheme for
 * a private host, so only requests to that host are intercepted.
 *
 * URL layout (all under `http://<VIEWER_HOST>/`):
 *  - `/`, `/index.html`, `/assets/...` -> app files from plugin resources.
 *  - `/m/<token>/<relPath>`          -> model + assets from disk (registry).
 */
class Model3DSchemeHandlerFactory : CefSchemeHandlerFactory {

    override fun create(
        browser: CefBrowser?,
        frame: CefFrame?,
        schemeName: String?,
        request: CefRequest?,
    ): CefResourceHandler? {
        val url = request?.url ?: return notFound()
        val path = try {
            URI(url).path ?: "/"
        } catch (e: Exception) {
            return notFound()
        }

        // Model / asset files served from disk via the traversal-safe registry.
        if (path.startsWith(MODEL_PREFIX)) {
            val rest = path.removePrefix(MODEL_PREFIX)
            val slash = rest.indexOf('/')
            if (slash <= 0) return notFound()
            val token = rest.substring(0, slash)
            val relPath = rest.substring(slash + 1)
            val file = Model3DAssetRegistry.resolve(token, relPath)
            if (file == null || !Files.isRegularFile(file)) return notFound()
            return try {
                Model3DResourceHandler(Files.readAllBytes(file), mimeFor(relPath))
            } catch (e: Exception) {
                LOG.warn("Failed to read model asset: $file", e)
                notFound()
            }
        }

        // Everything else is an app resource from the bundled build output.
        val resourcePath = when {
            path == "/" || path.isEmpty() -> "/viewer/index.html"
            else -> "/viewer$path"
        }
        val bytes = javaClass.getResourceAsStream(resourcePath)?.use { it.readBytes() }
            ?: return notFound()
        return Model3DResourceHandler(bytes, mimeFor(resourcePath))
    }

    private fun notFound(): CefResourceHandler =
        Model3DResourceHandler(null, "text/plain")

    companion object {
        const val VIEWER_HOST = "model3dviewer.localhost"
        private const val MODEL_PREFIX = "/m/"
        private val LOG = Logger.getInstance(Model3DSchemeHandlerFactory::class.java)
        private val registered = AtomicBoolean(false)

        /** Register the scheme handler exactly once (after JCEF is initialized). */
        fun ensureRegistered() {
            if (!registered.compareAndSet(false, true)) return
            try {
                // Force JCEF init so the shared CefApp exists, then register.
                JBCefApp.getInstance()
                val ok = CefApp.getInstance()
                    .registerSchemeHandlerFactory("http", VIEWER_HOST, Model3DSchemeHandlerFactory())
                if (!ok) {
                    registered.set(false)
                    LOG.warn("Failed to register the 3D viewer scheme handler factory.")
                }
            } catch (e: Exception) {
                registered.set(false)
                LOG.warn("Error registering the 3D viewer scheme handler factory", e)
            }
        }

        /**
         * Build the viewer URL for an already-registered model token. Encodes
         * the model path + type + wireframe + auto-rotate flags.
         */
        fun viewerUrl(token: String, mainName: String, wireframe: Boolean, autoRotate: Boolean): String {
            val type = mainName.substringAfterLast('.', "").lowercase()
            val modelParam = enc("m/$token/$mainName")
            return "http://$VIEWER_HOST/index.html?model=$modelParam&type=$type" +
                "&wireframe=$wireframe&autorotate=$autoRotate"
        }

        private fun enc(value: String): String =
            URLEncoder.encode(value, StandardCharsets.UTF_8)

        private fun mimeFor(path: String): String =
            when (path.substringAfterLast('.', "").lowercase()) {
                "html" -> "text/html"
                "js", "mjs" -> "text/javascript"
                "css" -> "text/css"
                "json" -> "application/json"
                "wasm" -> "application/wasm"
                "svg" -> "image/svg+xml"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "ktx2" -> "image/ktx2"
                "glb" -> "model/gltf-binary"
                "gltf" -> "model/gltf+json"
                "obj", "bin" -> "application/octet-stream"
                else -> "application/octet-stream"
            }
    }
}
