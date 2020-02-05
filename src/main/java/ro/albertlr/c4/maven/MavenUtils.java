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
package ro.albertlr.c4.maven;

import me.tongfei.progressbar.ProgressBar;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import ro.albertlr.c4.csv.PackageList;
import ro.albertlr.c4.csv.Header;
import ro.albertlr.c4.csv.PackageMapper;
import ro.albertlr.c4.maven.SourceOf.Type;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.file.Files.walk;
import static me.tongfei.progressbar.ProgressBarStyle.UNICODE_BLOCK;

public class MavenUtils {

    public static void main(String[] args) throws IOException {
        List<Module> modules = MavenUtils.listModulesWithJavaSources("/Users/albertlr/workspace/jive/core/code-review-application/pom.xml");

        System.out.println(":: modules ::");

        modules.forEach(module -> {
            System.out.printf("%s%n", module);
        });

//        System.out.println(":: JAVA ::");
//
//        PackageList.listPackages(modules, JAVA)
//                .forEach(pack -> System.out.printf("%s%n", pack));

//        System.out.println(":: JAVA SCRIPT ::");

//        PackageList.listJavaScriptPackages(modules)
//                .forEach(pack -> System.out.printf("%s%n", pack));

        Header[] headers = {Header.Module, Header.SourceType, Header.Package};
        Collection<Iterable<String>> records = PackageList.toCsvRecords(modules, headers);
        System.out.printf("%s records loaded%n", records.size());
        PackageMapper.saveMapping("mapping.csv", records, headers);

    }

    public static void install() {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("/path/to/pom.xml"));
        request.setGoals(Collections.singletonList("install"));

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File("/usr"));

        try {
            invoker.execute(request);
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }

    public static List<Module> listModulesWithJavaSources(String pomFile) throws IOException {
        Set<File> allModules = listOfModules(pomFile);

        Set<File> modulesWithJavaSources = new LinkedHashSet<>();

        for (File module : allModules) {
            File srcFolder = new File(module, "src/main/java");
            if (!(srcFolder.exists() && srcFolder.isDirectory())) {
                continue;
            }

            // filter out modules that have at least a Java file in it
            boolean hasAJavaFile = walk(Paths.get(srcFolder.toURI()))
                    .filter(file -> Files.isRegularFile(file) && file.toFile().getName().endsWith(".java"))
                    .count() > 0L;

            modulesWithJavaSources.add(module);
        }

        return modulesWithJavaSources.stream()
                .map(module -> new Module(module))
                .collect(Collectors.toList());
    }

    private static Invoker newInvoker() {
        return newInvoker(new ArrayList<>());
    }

    private static Invoker newInvoker(Collection<String> errorLines) {
        Invoker invoker = new DefaultInvoker();
        invoker.setErrorHandler(errorLines::add);
        invoker.setOutputHandler(line -> {
        });
        invoker.setMavenHome(new File(System.getenv("M2_HOME")));
        return invoker;
    }

    private static void invoke(InvocationRequest request, Collection<String> errorLines) {
        invoke(newInvoker(errorLines), request, errorLines);
    }

    private static void invoke(Invoker invoker, InvocationRequest request, Collection<String> errorLines) {
        try {
            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                errorLines.forEach(System.err::println);
                throw new IllegalStateException("Maven exited with exist code " + result.getExitCode() + ", reason: " + result.getExecutionException().getMessage());
            }
        } catch (MavenInvocationException e) {
            errorLines.forEach(System.err::println);
            throw new IllegalStateException(e);
        }
    }

    private static File asPomFile(String pomFile) {
        File pom = new File(pomFile);
        if (!pom.exists()) {
            throw new IllegalStateException("pom.xml file must exist on localhost");
        }
        return pom;
    }

    /**
     * List full path of maven modules starting from a root pom file
     * <pre>
     *     mvn -q --also-make exec:exec -Dexec.executable="pwd"
     * </pre>
     *
     * @param pomFile The root pom file
     * @return Returns a set of module paths
     */
    public static Set<File> listOfModules(String pomFile) {
        File pom = asPomFile(pomFile);

        Set<File> listOfModules = new LinkedHashSet<>();
        try (ProgressBar pb = new ProgressBar("Loading modules", 1, UNICODE_BLOCK)) {
            pb.maxHint(-1);
            Collection<String> errorLines = new ArrayList<>();
            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(new File(pomFile));
            request.setAlsoMake(true);
            request.setDebug(false);
            request.setErrorHandler(errorLines::add);
            request.setOutputHandler(new ModulePathHandler(pb, listOfModules));
            request.setBaseDirectory(pom.getParentFile());
            request.setGoals(Collections.singletonList("exec:exec"));
            Properties properties = new Properties();
            properties.setProperty("exec.executable", "pwd");
            request.setProperties(properties);

            invoke(request, errorLines);

            pb.maxHint(pb.getCurrent());
        }

        return listOfModules;
    }

    /**
     * Read the pom.xml of a module .. and extract the groupId, artifactId and version
     *
     * @param module The module to update with the given information
     */
    public static void refreshModule(Module module) {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(module.getPomFile()));

            module.setMavenId(model.getId());
            module.setGroupId(model.getGroupId());
            module.setArtifactId(model.getArtifactId());
            module.setVersion(model.getVersion());
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    private static class ModulePathHandler implements InvocationOutputHandler {
        private final ProgressBar pb;
        private final Set<File> listOfModules;

        public ModulePathHandler(ProgressBar pb, Set<File> listOfModules) {
            this.pb = pb;
            this.listOfModules = listOfModules;
        }

        @Override
        public void consumeLine(String line) throws IOException {
            pb.step();

            // maven will call this handler every time wants to output something.
            // Most of the lines start with [INFO] .. so skip those
            if (line.startsWith("[")) {
                return;
            }
            // we are interested only in fully qualified path names .. so check if it is an existing folder
            File file = new File(line);
            if (file.exists() && file.isDirectory()) {
                listOfModules.add(file);
            }
        }
    }
}
