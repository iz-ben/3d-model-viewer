package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Icon definitions for 3D model file types.
 * GLB: Default gray/white color scheme
 * GLTF: Blue accent color
 * OBJ: Orange accent color
 */
object Model3DIcon {
    @JvmField
    val glbIcon: Icon = IconLoader.getIcon("/icons/glb_dark.svg", Model3DIcon::class.java)

    @JvmField
    val glbIconLight: Icon = IconLoader.getIcon("/icons/glb_light.svg", Model3DIcon::class.java)

    @JvmField
    val gltfIcon: Icon = IconLoader.getIcon("/icons/gltf_dark.svg", Model3DIcon::class.java)

    @JvmField
    val gltfIconLight: Icon = IconLoader.getIcon("/icons/gltf_light.svg", Model3DIcon::class.java)

    @JvmField
    val objIcon: Icon = IconLoader.getIcon("/icons/obj_dark.svg", Model3DIcon::class.java)

    @JvmField
    val objIconLight: Icon = IconLoader.getIcon("/icons/obj_light.svg", Model3DIcon::class.java)

    @JvmField
    val stlIcon: Icon = IconLoader.getIcon("/icons/stl_dark.svg", Model3DIcon::class.java)

    @JvmField
    val stlIconLight: Icon = IconLoader.getIcon("/icons/stl_light.svg", Model3DIcon::class.java)

    // Legacy aliases for backwards compatibility
    @JvmField
    @Deprecated("Use glbIcon instead", ReplaceWith("glbIcon"))
    val icon_darkMode: Icon = glbIcon

    @JvmField
    @Deprecated("Use glbIconLight instead", ReplaceWith("glbIconLight"))
    val icon_lightMode: Icon = glbIconLight
}
