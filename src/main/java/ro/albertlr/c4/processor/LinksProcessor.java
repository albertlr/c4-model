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
package ro.albertlr.c4.processor;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;
import me.tongfei.progressbar.ProgressBar;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import ro.albertlr.c4.Configuration;
import ro.albertlr.c4.graph.Link;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.base.Predicates.contains;
import static com.google.common.base.Predicates.not;
import static me.tongfei.progressbar.ProgressBarStyle.UNICODE_BLOCK;

@UtilityClass
public class LinksProcessor {

    public static void normalizeDotFile(String dotFile) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: normalizing dot file %s started ::%n", dotFile);
        Set<Link> loadedLinks = loadLinks(dotFile);

        Stopwatch processingWatch = Stopwatch.createStarted();
        System.out.printf(":: links processing started ::%n");
        Set<Link> processedLinks = unique(
                filterJiveComponents(
                        filterExternalComponents(
                                flat(loadedLinks)
                        )
                )
        );
        System.out.printf(":: links processing completed in %s ::%n", processingWatch);

        saveToDots(dotFile + ".out", processedLinks);

        try {
            if (false) {
                generatePdfGraph(dotFile + ".out");
            }
        } catch (IOException e) {
            System.err.println("Could not generate PDF");
            e.printStackTrace();
        } finally {
            System.out.printf(":: normalizing dot file %s completed in %s ::%n", dotFile, stopwatch);
        }
    }

    public static void generatePdfGraph(String dotFile) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: start generating PDF diagram ::%n");
        ProcessBuilder processBuilder = new ProcessBuilder("twopi", "-Tpdf", "-Kcirco", "-O", dotFile);
        processBuilder.inheritIO();
        processBuilder.directory(new File("."));
        Process process = processBuilder.start();

        try (ProgressBar pb = new ProgressBar("Generate PDF", 100, UNICODE_BLOCK)) {
            pb.maxHint(-1);

            process.waitFor();

            pb.maxHint(100);
            pb.stepTo(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } finally {
            System.out.printf(":: generating PDF diagram completed in %s::%n", stopwatch);
        }
    }

    public static void main(String[] args) throws Exception {
        final String dotFile =
                "jive-core-3000.5.0.jar-fas-clean.dot";
        final String mappingFile = "jive-core-v2-FAs-97-components.csv";

        PackageToComponentMapper.mapAndSave(dotFile, mappingFile, dotFile + ".comp.dot");

        normalizeDotFile(dotFile + ".comp.dot");

//        Set<Link> result = LinksProcessor.loadLinks(dotFile);
//
//        int counter = 0;
//        for (Link links :
//                unique(
//                        filterJiveComponents(
//                                filterExternalComponents(
//                                        flat(result)
//                                )
//                        )
//                )) {
//            System.out.printf("%s: %s%n", Strings.padStart(String.valueOf(counter++), 4, '0'), links);
////            counter++;
////            if (counter == 100) {
////                break;
////            }
//        }
    }

    public static Set<Link> filterJiveComponents(Set<Link> links) {
        Predicate<CharSequence> patternPredicates = not(contains(Configuration.getInstance().getLinkJivePattern()));

        return Sets.filter(
                Sets.filter(
                        links,
                        link -> patternPredicates.apply(link.getTo().asText())
                ),
                link -> patternPredicates.apply(link.getFrom().asText())
        );
    }

    public static Set<Link> filterExternalComponents(Set<Link> links) {
        Predicate<CharSequence> patternPredicates = not(contains(Configuration.getInstance().getLinkToPatterns()));

        return Sets.filter(links, (link) -> patternPredicates.apply(link.getTo().asText()));
    }

    public static SortedSet<Link> unique(Collection<Link> links) {
        return Sets.newTreeSet(links);
    }

    public static Set<Link> flat(Set<Link> links) {
        return links.stream()
                .flatMap(link -> StreamSupport.stream(link.spliterator(), false))
                .collect(Collectors.toSet());
    }

    public static Set<Link> loadLinks(String dotFile) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: loading links from %s started ::%n", dotFile);
        Set<Link> links = new UnifiedSet<>();

        try (Stream<String> lineStream = Files.lines(Paths.get(dotFile))) {
            lineStream.forEach(line ->
            {
                try {
                    links.add(new Link(line));
                } catch (IndexOutOfBoundsException exception) {
                    // first, second and last line should throw this exception, skip them
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.printf(":: loading links from %s completed in %s::%n", dotFile, stopwatch);
        }

        return links;
    }

    public static void saveToDots(String output, Set<Link> links) {
        saveToDots(output, links, false);
    }

    public static void saveToDots(String output, Set<Link> links, boolean printLibrary) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: start saving %s links to dots file to %s ::%n", links.size(), output);
        boolean oldPrintLibrary = Configuration.getInstance().isPrintLibrary();
        if (oldPrintLibrary != printLibrary) {
            Configuration.getInstance().setPrintLibrary(printLibrary);
        }
        try {
            Path path = Paths.get(output);
            Collection<String> lines = new ArrayList<>();
            lines.add("digraph \"" + path.getFileName() + "\" {");
            lines.add("    // Path: " + path.getParent() + "/" + path.getFileName());

            links.forEach(link -> lines.add(link.toString()));
            lines.add("} ");
            try {
                Files.write(Paths.get(output), lines);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            if (oldPrintLibrary != printLibrary) {
                Configuration.getInstance().setPrintLibrary(oldPrintLibrary);
            }
            System.out.printf(":: dots file generation completed in %s ::%n", stopwatch);
        }
    }

}
