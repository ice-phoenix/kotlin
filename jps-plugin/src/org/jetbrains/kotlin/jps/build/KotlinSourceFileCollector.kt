/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.containers.MultiMap
import org.jetbrains.jps.builders.DirtyFilesHolder
import org.jetbrains.jps.builders.java.JavaSourceRootDescriptor
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModuleSourceRoot
import org.jetbrains.jps.util.JpsPathUtil

import java.io.File

object KotlinSourceFileCollector {
    // For incremental compilation
    fun getDirtySourceFiles(dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>): MultiMap<ModuleBuildTarget, File> {
        val result = MultiMap<ModuleBuildTarget, File>()

        dirtyFilesHolder.processDirtyFiles { target, file, root ->
            //TODO this is a workaround for bug in JPS: the latter erroneously calls process with invalid parameters
            if (root.getTarget() == target && isKotlinSourceFile(file)) {
                result.putValue(target, file)
            }
            true
        }
        return result
    }

    fun getRemovedKotlinFiles(
        dirtyFilesHolder: DirtyFilesHolder<JavaSourceRootDescriptor, ModuleBuildTarget>,
        target: ModuleBuildTarget
    ): List<File> =
        dirtyFilesHolder
            .getRemovedFiles(target)
            .mapNotNull { if (FileUtilRt.extensionEquals(it, "kt")) File(it) else null }

    fun getAllKotlinSourceFiles(target: ModuleBuildTarget): List<File> {
        val moduleExcludes = target.module.excludeRootsList.urls.mapTo(HashSet(), JpsPathUtil::urlToFile)

        val compilerExcludes = JpsJavaExtensionService.getInstance()
            .getOrCreateCompilerConfiguration(target.module.project)
            .compilerExcludes

        val allKotlinFiles = ArrayList<File>()
        for (sourceRoot in getRelevantSourceRoots(target)) {
            sourceRoot.file.walkTopDown()
                .onEnter { it !in moduleExcludes }
                .filterTo(allKotlinFiles) { !compilerExcludes.isExcluded(it) && it.isFile && isKotlinSourceFile(it) }
        }
        return allKotlinFiles
    }

    private fun getRelevantSourceRoots(target: ModuleBuildTarget): Iterable<JpsModuleSourceRoot> {
        val sourceRootType = if (target.isTests) JavaSourceRootType.TEST_SOURCE else JavaSourceRootType.SOURCE
        return target.module.getSourceRoots<JavaSourceRootProperties>(sourceRootType)
    }

    internal fun isKotlinSourceFile(file: File): Boolean {
        return FileUtilRt.extensionEquals(file.name, "kt")
    }
}
