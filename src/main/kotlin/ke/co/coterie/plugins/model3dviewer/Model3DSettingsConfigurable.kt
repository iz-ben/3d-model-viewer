package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings configurable for the 3D Model Viewer plugin.
 * Appears under Settings > Tools > 3D Model Viewer
 */
class Model3DSettingsConfigurable : Configurable {

    private var objSupportCheckbox: JBCheckBox? = null
    private var backgroundCombo: ComboBox<Model3DSettings.BackgroundMode>? = null
    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "3D Model Viewer"

    override fun createComponent(): JComponent {
        objSupportCheckbox = JBCheckBox("Enable OBJ file support").apply {
            toolTipText = "When enabled, the 3D Model Viewer will also handle .obj files."
        }

        backgroundCombo = ComboBox(DefaultComboBoxModel(Model3DSettings.BackgroundMode.entries.toTypedArray())).apply {
            toolTipText = "Backdrop for the 3D preview. \"Follow IDE theme\" matches the IDE's light/dark theme."
            renderer = com.intellij.ui.SimpleListCellRenderer.create("") { it.label }
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(objSupportCheckbox!!)
            .addLabeledComponent("Renderer background:", backgroundCombo!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = Model3DSettings.getInstance()
        return objSupportCheckbox?.isSelected != settings.state.objSupportEnabled ||
            selectedBackgroundMode()?.id != settings.state.backgroundMode
    }

    override fun apply() {
        val settings = Model3DSettings.getInstance()
        val wasEnabled = settings.state.objSupportEnabled
        val isEnabled = objSupportCheckbox?.isSelected ?: false

        if (wasEnabled != isEnabled) {
            settings.setObjSupportEnabled(isEnabled)
        }

        selectedBackgroundMode()?.let { settings.setBackgroundMode(it.id) }
    }

    override fun reset() {
        val settings = Model3DSettings.getInstance()
        objSupportCheckbox?.isSelected = settings.state.objSupportEnabled
        backgroundCombo?.selectedItem = Model3DSettings.BackgroundMode.fromId(settings.state.backgroundMode)
    }

    override fun disposeUIResources() {
        objSupportCheckbox = null
        backgroundCombo = null
        mainPanel = null
    }

    private fun selectedBackgroundMode(): Model3DSettings.BackgroundMode? =
        backgroundCombo?.selectedItem as? Model3DSettings.BackgroundMode
}

/**
 * Provider for the settings configurable.
 * This is used by IntelliJ to discover and instantiate the configurable.
 */
class Model3DSettingsConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable = Model3DSettingsConfigurable()
}
