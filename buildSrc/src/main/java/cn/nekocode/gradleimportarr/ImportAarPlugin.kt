/*
 * Copyright 2018. nekocode (nekocode.cn@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.nekocode.gradleimportarr

import com.android.build.gradle.api.AndroidBasePlugin
import com.android.build.gradle.internal.dependency.AarTransform
import com.android.build.gradle.internal.dependency.ExtractAarTransform
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.ArtifactTransform
import org.gradle.api.internal.artifacts.ArtifactAttributes.ARTIFACT_FORMAT
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.net.URL
import java.net.URLClassLoader

/**
 * Debug: ./gradlew bD -Dorg.gradle.daemon=false -Dorg.gradle.debug=true
 * @author nekocode (nekocode.cn@gmail.com)
 */
class ImportAarPlugin : Plugin<Project> {

    companion object {
        const val ANDROID_PLUGIN_CLASSPATH = "com.android.build.gradle.api.AndroidBasePlugin"
        const val CONFIG_NAME = "aarCompileOnly"
    }

    override fun apply(project: Project) {
        project.buildscript.run {
            val classloader = Thread.currentThread().contextClassLoader as URLClassLoader

            // Check if imported android plugin
            try {
                classloader.loadClass(ANDROID_PLUGIN_CLASSPATH)
                return@run
            } catch (ignored: ClassNotFoundException) {
            }

            // Define repository
            repositories.google()

            // Add android gradle plugin to dependencies
            val config = configurations.maybeCreate("pluginClasspath")
            config.defaultDependencies {
                it.add(dependencies.create("com.android.tools.build:gradle:3.2.1"))
            }

            // Resolve depended jars and import them
            config.forEach { file ->
                URLClassLoader::class.java
                        .getDeclaredMethod("addURL", URL::class.java)
                        .run {
                            isAccessible = true
                            invoke(classloader, file.toURI().toURL())
                        }
            }
        }

        // Check if this project is a pure java project
        val java = project.convention.findPlugin(JavaPluginConvention::class.java)
        if (java == null || project.plugins.findPlugin(AndroidBasePlugin::class.java) != null) {
            throw IllegalStateException("The 'import-aar' plugin can only be used in a pure java module.")
        }

        val mainSourceSet = java.sourceSets.getByName("main")
        val aar = AndroidArtifacts.ArtifactType.AAR.type
        val jar = AndroidArtifacts.ArtifactType.JAR.type

        // Create configuration
        val config = project.configurations.maybeCreate(CONFIG_NAME)
        config.attributes.attribute(ARTIFACT_FORMAT, jar)

        // Add aar's jars to compileClasspath
        mainSourceSet.compileClasspath += config

        // Add aar's jars to "PROVIDED" scope of idea
        project.pluginManager.apply(IdeaPlugin::class.java)
        project.extensions.getByType(IdeaModel::class.java)
                .module.scopes["PROVIDED"]!!["plus"]!!.add(config)

        // Register aar transform
        project.dependencies.run {
            registerTransform {
                it.from.attribute(ARTIFACT_FORMAT, aar)
                it.to.attribute(ARTIFACT_FORMAT, jar)
                it.artifactTransform(AarJarArtifactTransform::class.java)
            }
        }
    }

    class AarJarArtifactTransform : ArtifactTransform() {

        override fun transform(input: File): MutableList<File> {
            val extractTrans = ExtractAarTransform()
            extractTrans.outputDirectory = File(outputDirectory, "exploded")
            var files = extractTrans.transform(input)

            val aarTrans = AarTransform(AndroidArtifacts.ArtifactType.JAR, false, false)
            aarTrans.outputDirectory = outputDirectory
            files = files.flatMap { aarTrans.transform(it) }

            // Copy and rename the classes.jar
            val jarFile = files.singleOrNull() ?: return arrayListOf()
            val renamedJarFile = File(outputDirectory, "${input.nameWithoutExtension}.jar")
            renamedJarFile.writeBytes(jarFile.readBytes())

            return arrayListOf(renamedJarFile)
        }
    }
}