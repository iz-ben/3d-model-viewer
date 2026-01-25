package ke.co.coterie.plugins.model3dviewer

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableProvider
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Settings configurable for the 3D Model Viewer plugin.
 * Appears under Settings > Tools > 3D Model Viewer
 */
class Model3DSettingsConfigurable : Configurable {

    private var objSupportCheckbox: JBCheckBox? = null
    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "3D Model Viewer"

    override fun createComponent(): JComponent {
        objSupportCheckbox = JBCheckBox("Enable OBJ file support").apply {
            toolTipText = "When enabled, the 3D Model Viewer will also handle .obj files. Requires IDE restart to take full effect."
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(objSupportCheckbox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return mainPanel!!
    }

    override fun isModified(): Boolean {
        val settings = Model3DSettings.getInstance()
        return objSupportCheckbox?.isSelected != settings.state.objSupportEnabled
    }

    override fun apply() {
        val settings = Model3DSettings.getInstance()
        val wasEnabled = settings.state.objSupportEnabled
        val isEnabled = objSupportCheckbox?.isSelected ?: false

        if (wasEnabled != isEnabled) {
            settings.setObjSupportEnabled(isEnabled)

            // Show restart notification
            showRestartNotification(isEnabled)
        }
    }

    override fun reset() {
        val settings = Model3DSettings.getInstance()
        objSupportCheckbox?.isSelected = settings.state.objSupportEnabled
    }

    override fun disposeUIResources() {
        objSupportCheckbox = null
        mainPanel = null
    }

    private fun showRestartNotification(objEnabled: Boolean) {
        val message = if (objEnabled) {
            "OBJ file support has been enabled. Please restart the IDE for changes to take full effect."
        } else {
            "OBJ file support has been disabled. Please restart the IDE for changes to take full effect."
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("Model3DViewer.Notifications")
            .createNotification(
                "3D Model Viewer",
                message,
                NotificationType.INFORMATION
            )
            .addAction(RestartIdeAction())
            .notify(null)
    }
}

/**
 * Provider for the settings configurable.
 * This is used by IntelliJ to discover and instantiate the configurable.
 */
class Model3DSettingsConfigurableProvider : ConfigurableProvider() {
    override fun createConfigurable(): Configurable = Model3DSettingsConfigurable()
}
