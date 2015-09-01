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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.idea.test.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf

public class DecompiledTextConsistencyTest : TextConsistencyBaseTest() {

    override fun getPackages(): List<FqName> = emptyList()

    override fun getFacades(): List<FqName> = listOf(FqName("kotlin.JUtilKt"))

    override fun getTopLevelMembers(): Map<String, String> = mapOf("kotlin.JUtilKt" to "linkedListOf")

    override fun getVirtualFileFinder(): VirtualFileFinder =
            JvmVirtualFileFinder.SERVICE.getInstance(project)

    override fun getDecompiledText(packageFile: VirtualFile, resolver: ResolverForDecompiler?): String =
            (resolver?.let { buildDecompiledText(packageFile, it) } ?: buildDecompiledText(packageFile)).text

    override fun getModuleDescriptor(): ModuleDescriptor =
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationWithCustomContext(
                    TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, "test"),
                    listOf(), BindingTraceContext(), null, null,
                    IDEPackagePartProvider(GlobalSearchScope.allScope(project))
            ).moduleDescriptor

    override fun getProjectDescriptor() =
            object : JetWithJdkAndRuntimeLightProjectDescriptor() {
                override fun getSdk() = PluginTestCaseBase.fullJdk()
            }

    override fun isFromFacade(descriptor: CallableMemberDescriptor, facadeFqName: FqName): Boolean =
            descriptor is DeserializedCallableMemberDescriptor &&
            descriptor.proto.hasExtension(JvmProtoBuf.implClassName) &&
            facadeFqName == PackagePartClassUtils.getPackagePartFqName(descriptor)
}
