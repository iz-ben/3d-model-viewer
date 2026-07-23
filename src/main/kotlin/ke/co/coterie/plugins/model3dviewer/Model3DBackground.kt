package ke.co.coterie.plugins.model3dviewer

import com.intellij.util.ui.StartupUiUtil
import java.awt.Color

/**
 * Resolves the renderer-background preference into a concrete backdrop for the
 * 3D preview.
 *
 * The user setting has three modes (see [Model3DSettings.BackgroundMode]):
 * `follow` (match the IDE theme), `light` and `dark`. "follow" is resolved to a
 * concrete `light`/`dark` value here against the current IDE theme, so the
 * bundled viewer only ever needs to handle two states.
 */
object Model3DBackground {

    const val LIGHT = "light"
    const val DARK = "dark"

    // Swing fill colours that match the viewer's stage gradient edges, used to
    // paint the JCEF surface so a resize doesn't flash a mismatched background
    // before the page repaints.
    private val DARK_COLOR = Color(0x14, 0x14, 0x18)
    private val LIGHT_COLOR = Color(0xE9, 0xEA, 0xEE)

    /** The IDE's current theme expressed as a backdrop value. */
    fun ideTheme(): String = if (StartupUiUtil.isDarkTheme) DARK else LIGHT

    /** Resolve a [Model3DSettings.BackgroundMode] id into a concrete `light`/`dark`. */
    fun resolve(mode: String?): String = when (mode) {
        LIGHT -> LIGHT
        DARK -> DARK
        else -> ideTheme() // BackgroundMode.FOLLOW_IDE and any unknown value
    }

    /** The Swing fill colour for an already-resolved backdrop value. */
    fun swingColor(resolved: String?): Color = if (resolved == LIGHT) LIGHT_COLOR else DARK_COLOR
}
