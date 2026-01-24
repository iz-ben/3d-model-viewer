package icons

import com.intellij.openapi.util.IconLoader

object GlbIcon {
    @JvmField
    val icon_darkMode = IconLoader.getIcon("icons/pluginIcon_dark.svg", javaClass)

    @JvmField
    val icon_lightMode = IconLoader.getIcon("icons/pluginIcon_light", javaClass)
}