/*
 * Copyright 2015 guicamest
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.guicamest.dexlazyloader

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ComponentMetadataDetails
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DYLPlugin implements Plugin<Project> {

	void apply(Project project) {
		project.extensions.create('dyl', DYLPluginExtension)

		// Add 'dyl' as a source set extension
		project.android.sourceSets.all { sourceSet ->
			sourceSet.extensions.create('dyl', DefaultSourceDirectorySet, 'dyl', project.fileResolver)
			sourceSet.dyl.srcDir "src/${sourceSet.name}/dyl"
			sourceSet.dyl.getFilter().include('**/*.jar');
		}

		Logger logger = LoggerFactory.getLogger('DYLPlugin')

		project.afterEvaluate {
			def androidBuilder = null

			def variants = null
			if (project.android.hasProperty('applicationVariants')) {
				variants = project.android.applicationVariants
			}
			else if (project.android.hasProperty('libraryVariants')) {
				variants = project.android.libraryVariants
			}
			else {
				throw new IllegalStateException('Android project must have applicationVariants or libraryVariants!')
			}

			// Register our task with the variant's flow
			variants.all { variant ->
				if ( !androidBuilder ){
					androidBuilder = variant.aidlCompile.getBuilder()
				}
				//https://android.googlesource.com/platform/tools/base/+/master/build-system/gradle-core/src/main/groovy/com/android/build/gradle/internal/api/ApplicationVariantImpl.java

				variant.sourceSets.each { sourceSet ->
					def configName = sourceSet.name == 'main' ? 'provided' : "${sourceSet.name}Provided"
					def configSpecificDeps = project.configurations.findByName(configName).files
					logger.info('specific provided deps for sourceset {} : {}',sourceSet, configSpecificDeps)

					Task dexToAssetsTask = project.task("dexTo${variant.name.capitalize()}${sourceSet.name.capitalize()}Assets", type: DexToAssetsTask) {
						it.jarsToDex = getJarsToDex(sourceSet.dyl, project.dyl, project.files(configSpecificDeps).asFileTree)
						assetsOutputDir = sourceSet.assets.getSrcDirs().first()
						it.androidBuilder = androidBuilder
					}

					dexToAssetsTask.description = 'Dex jars to assets'

					Task genVariantAssets = project.tasks.findByPath("generate${variant.name.capitalize()}Assets")
					dexToAssetsTask.dependsOn genVariantAssets
					variant.mergeAssets.dependsOn dexToAssetsTask
				}
			}

			Task eclipseTask = project.getTasks().findByPath('eclipse')
			if ( eclipseTask != null ){
				def providedDeps = project.configurations.findByName('provided').files
				logger.info('provided deps for all variants : {}',providedDeps)
				def providedDepsTree = project.files(providedDeps).asFileTree

				def sourceOutputDir = project.android.sourceSets.main.assets.srcDirs[0]

				def dexToAssetsTask = project.task('dexToEclipseAssets', type: DexToAssetsTask) {
					it.jarsToDex = getJarsToDex(project.android.sourceSets.main.dyl, project.dyl, providedDepsTree)
					assetsOutputDir = sourceOutputDir
					it.androidBuilder = androidBuilder
				}
				dexToAssetsTask.description = 'Dex jars to Eclipse assets.'
				dexToAssetsTask.group = 'IDE'
				eclipseTask.dependsOn dexToAssetsTask
			}
		}
	}

	FileCollection getJarsToDex(SourceDirectorySet sds, DYLPluginExtension dylExtension, FileTree... additionalFiles){
		def inclusions = dylExtension.include
		def exclusions = dylExtension.exclude - inclusions

		FileTree wholeTree = sds
		additionalFiles?.each{ wholeTree = wholeTree.plus(it)}
		wholeTree.matching{
			exclude(exclusions)
			include(inclusions)
		}.filter { File f ->
			// We keep every dep in the dyl sourceset and
			// provided deps that are not already in the dyl sourceset
			// in case the user wants to 'override' the dep
			def inDylSS = sds.contains(f)
			if ( !inDylSS ){
				return !sds.files.collect{it.name}.contains(f.name)
			}
			true
		}
	}
}