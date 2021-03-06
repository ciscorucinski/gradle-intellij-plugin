package org.jetbrains.intellij.dependency

import com.intellij.structure.plugin.PluginCreationSuccess
import com.intellij.structure.plugin.PluginManager
import groovy.transform.ToString
import org.gradle.api.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.intellij.IntelliJPlugin

@ToString(includeNames = true, includeFields = true, ignoreNulls = true)
class PluginProjectDependency implements PluginDependency, Serializable {
    @NotNull
    private transient Project project

    @Lazy
    private File pluginDirectory = {
        def prepareSandboxTask = project?.tasks?.findByName(IntelliJPlugin.PREPARE_SANDBOX_TASK_NAME)
        //noinspection GroovyAssignabilityCheck
        return prepareSandboxTask != null ?
                new File(prepareSandboxTask.destinationDir, prepareSandboxTask.pluginName) : null
    }()

    @Lazy
    private transient PluginDependencyImpl pluginDependency = {
        if (pluginDirectory.exists()) {
            def creationResult = PluginManager.instance.createPlugin(pluginDirectory)
            if (creationResult instanceof PluginCreationSuccess) {
                def intellijPlugin = creationResult.plugin
                def pluginDependency = new PluginDependencyImpl(intellijPlugin.pluginId, intellijPlugin.pluginVersion, pluginDirectory)
                pluginDependency.sinceBuild = intellijPlugin.sinceBuild?.asStringWithoutProductCode()
                pluginDependency.untilBuild = intellijPlugin.untilBuild?.asStringWithoutProductCode()
                return pluginDependency
            }
            IntelliJPlugin.LOG.error("Cannot use $pluginDirectory as a plugin dependency. " + creationResult)
        }
        return null
    }()

    PluginProjectDependency(@NotNull Project project) {
        this.project = project
    }

    @NotNull
    @Override
    String getId() {
        return pluginDependency ? pluginDependency.id : "<unknown plugin id>"
    }

    @NotNull
    @Override
    String getVersion() {
        return pluginDependency ? pluginDependency.version : "<unknown plugin version>"
    }

    @Nullable
    @Override
    String getChannel() {
        return pluginDependency?.channel
    }

    @NotNull
    @Override
    File getArtifact() {
        return this.pluginDirectory
    }

    @NotNull
    @Override
    Collection<File> getJarFiles() {
        return pluginDependency ? pluginDependency.jarFiles : Collections.emptyList()
    }

    @Nullable
    @Override
    File getClassesDirectory() {
        return pluginDependency?.classesDirectory
    }

    @Nullable
    @Override
    File getMetaInfDirectory() {
        return pluginDependency?.metaInfDirectory
    }

    @Override
    File getSourcesDirectory() {
        return pluginDependency?.sourcesDirectory
    }

    @Override
    boolean isBuiltin() {
        return false
    }
}
