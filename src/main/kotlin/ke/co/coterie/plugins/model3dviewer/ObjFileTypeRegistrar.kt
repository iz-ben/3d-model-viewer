package ke.co.coterie.plugins.model3dviewer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.project.Project

/**
 * Dynamically registers/unregisters the OBJ file type based on user settings.
 * This component handles the dynamic extension point registration for OBJ support.
 */
@Service(Service.Level.APP)
class ObjFileTypeRegistrar {

    @Volatile
    private var isObjRegistered = false

    init {
        // Register OBJ file type on startup if enabled in settings
        val settings = Model3DSettings.getInstance()
        if (settings.state.objSupportEnabled) {
            registerObjFileType()
        }

        // Listen for settings changes
        settings.addSettingsChangeListener {
            handleSettingsChange()
        }
    }

    private fun handleSettingsChange() {
        val settings = Model3DSettings.getInstance()
        if (settings.state.objSupportEnabled && !isObjRegistered) {
            registerObjFileType()
        } else if (!settings.state.objSupportEnabled && isObjRegistered) {
            unregisterObjFileType()
        }
    }

    private fun registerObjFileType() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                try {
                    val fileTypeManager = FileTypeManager.getInstance()
                    // Associate .obj extension with our ObjFileType
                    fileTypeManager.associateExtension(ObjFileType.INSTANCE, "obj")
                    isObjRegistered = true
                    println("OBJ file type registered successfully")
                } catch (e: Exception) {
                    println("Failed to register OBJ file type: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun unregisterObjFileType() {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                try {
                    val fileTypeManager = FileTypeManager.getInstance()
                    // Remove .obj extension association
                    fileTypeManager.removeAssociatedExtension(ObjFileType.INSTANCE, "obj")
                    isObjRegistered = false
                    println("OBJ file type unregistered successfully")
                } catch (e: Exception) {
                    println("Failed to unregister OBJ file type: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        fun getInstance(): ObjFileTypeRegistrar {
            return ApplicationManager.getApplication().getService(ObjFileTypeRegistrar::class.java)
        }
    }
}

/**
 * Startup activity to ensure the ObjFileTypeRegistrar is initialized.
 */
class ObjFileTypeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Simply accessing the service will initialize it
        ObjFileTypeRegistrar.getInstance()
    }
}
