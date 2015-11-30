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
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
/**
 * Task that generates Assets.java based on /assets folder.
 */
class DexToAssetsTask extends DefaultTask {

    /**
     * Input jars
     */
    @InputFiles
    FileCollection jarsToDex

    /**
     * Asset directory to put dexed jars
     */
    @OutputDirectory
    File assetsOutputDir

    // https://android.googlesource.com/platform/tools/build/+/master/builder/src/main/java/com/android/builder/AndroidBuilder.java
    // * The one. https://android.googlesource.com/platform/tools/base/+/master/build-system/builder/src/main/java/com/android/builder/core/AndroidBuilder.java
    def androidBuilder

    @TaskAction
    def generateAssetsFile(IncrementalTaskInputs inputs) {
        List<File> assetFiles = []

        logger.info('Writing to {}',assetsOutputDir.absolutePath)
        def redex = []
        def remove = []
        inputs.outOfDate {
            redex << [it.file, dexFile(it.file)]
        }
        inputs.removed {
            remove << dexFile(it.file)
        }

        remove.each { File f ->
            logger.info('Deleting {}', f.absolutePath)
            f.delete()
        }

        logger.info('Dexing {} jars',redex.size())
        redex.each {
            logger.info('Dexing {} to {}',it[0], it[1])
            dexJar(it[0], it[1])
        }
    }

    def dexJar(File jar, File outDexFile){
        List args = ['--dex', '--output', outDexFile.absolutePath, jar.absolutePath]
        project.javaexec {
            main = 'com.android.dx.command.Main'
            it.args = args
            classpath = project.files(androidBuilder.dxJar.absolutePath)
            minHeapSize = '256m'
            maxHeapSize = '1g'
        }
    }

    File dexFile(File f){
        def lio = f.name.lastIndexOf('.')
        return new File(assetsOutputDir, f.name.substring(0,lio)+'.dex.jar')
    }

}
