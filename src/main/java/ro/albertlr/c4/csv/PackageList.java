/*-
 * #%L
 * c4-model
 *  
 * Copyright (C) 2019 - 2020 László-Róbert, Albert (robert@albertlr.ro)
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ro.albertlr.c4.csv;

import me.tongfei.progressbar.ProgressBar;
import ro.albertlr.c4.maven.Module;
import ro.albertlr.c4.maven.SourceOf;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.io.Files.fileTreeTraverser;
import static me.tongfei.progressbar.ProgressBarStyle.UNICODE_BLOCK;
import static ro.albertlr.c4.maven.SourceOf.Type.JAVA;
import static ro.albertlr.c4.maven.SourceOf.Type.JAVA_SCRIPT;
import static ro.albertlr.c4.maven.SourceOf.Type.RESOURCES;

public class PackageList {

    public static Collection<Iterable<String>> toCsvRecords(Collection<Module> modules, Header... headers) throws IOException {
        Collection<PackageRecord> packageRecords = new ArrayList<>();

        Collection<String> packages;
        try (ProgressBar pb = new ProgressBar("Compute CSV records", 1, UNICODE_BLOCK)) {
            pb.maxHint(-1);

            pb.step();
            for (Module module : modules) {
                pb.step();
                if (module.getSrcJava() == null) {
                    continue;
                }

                packages = listJavaPackages(module);
                packages.forEach(packageName ->
                        packageRecords.add(PackageRecord.builder()
                                .module(module.getArtifactId())
                                .sourceType(module.getSrcJava().getType().name())
                                .packageName(packageName)
                                .build())
                );
                pb.stepBy(packages.size());
            }

            pb.step();
            for (Module module : modules) {
                pb.step();
                if (module.getSrcJavaScript() == null) {
                    continue;
                }

                packages = listJavaScriptPackages(module);
                packages.forEach(packageName ->
                        packageRecords.add(PackageRecord.builder()
                                .module(module.getArtifactId())
                                .sourceType(module.getSrcJavaScript().getType().name())
                                .packageName(packageName)
                                .build())
                );
                pb.stepBy(packages.size());
            }

            pb.step();
            for (Module module : modules) {
                pb.step();
                if (module.getSrcResources() == null) {
                    continue;
                }
                packages = listResourcePackages(module);
                packages.forEach(packageName ->
                        packageRecords.add(PackageRecord.builder()
                                .module(module.getArtifactId())
                                .sourceType(module.getSrcResources().getType().name())
                                .packageName(packageName)
                                .build())
                );
                pb.stepBy(packages.size());
            }


            pb.maxHint(pb.getCurrent());

            return packageRecords.stream()
                    .map(record -> record.asIterable(headers))
                    .collect(Collectors.toList());
        }

    }

    public static Collection<String> listJavaPackages(Module module) throws IOException {
        return listJavaPackages(Collections.singleton(module));
    }

    public static Collection<String> listJavaPackages(Collection<Module> modules) throws IOException {
        return listPackages(modules, JAVA);
    }

    public static Collection<String> listJavaScriptPackages(Module module) throws IOException {
        return listJavaScriptPackages(Collections.singleton(module));
    }

    public static Collection<String> listJavaScriptPackages(Collection<Module> modules) throws IOException {
        return listPackages(modules, JAVA_SCRIPT);
    }

    public static Collection<String> listResourcePackages(Module module) throws IOException {
        return listResourcePackages(Collections.singleton(module));
    }

    public static Collection<String> listResourcePackages(Collection<Module> modules) throws IOException {
        return listPackages(modules, RESOURCES);
    }

    public static Collection<String> listPackages(Collection<Module> modules, SourceOf.Type type) throws IOException {
        Collection<String> packages = new ArrayList<>();

        for (Module module : modules) {
            SourceOf sourceOf = module.getSourceOf(type);
            if (sourceOf == null) {
                continue;
            }
            String sourcePath = sourceOf.getSrcDir() == null ? null : sourceOf.getSrcDir().getPath() + "/";
            if (isNullOrEmpty(sourcePath)) {
                continue;
            }

            for (File file :
                    filter(
                            filter(
                                    fileTreeTraverser().preOrderTraversal(sourceOf.getSrcDir())
                                    , f -> f.isDirectory()
                            ),
                            path -> pathEndingIn(sourceOf, path)
                    )
            ) {
                String path = file.getPath();
                String canonicalPackageName = path.startsWith(sourcePath) ? path.substring(sourcePath.length()) : path;
                String fullyQualifiedPackageName = canonicalPackageName.replaceAll("/", ".");

                packages.add(fullyQualifiedPackageName);
            }
        }
        return packages;
    }

    private static boolean pathEndingIn(SourceOf sourceOf, File path) {
        String[] filteredFiles = path.list((dir, name) -> endsWith(sourceOf, name));
        return filteredFiles != null && filteredFiles.length != 0;
    }

    private static boolean endsWith(SourceOf sourceOf, String filename) {
        for (String extension : sourceOf.getType()) {
            if (filename.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

}
