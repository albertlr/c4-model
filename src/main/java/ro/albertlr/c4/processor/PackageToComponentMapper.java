package ro.albertlr.c4.processor;

import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.experimental.UtilityClass;
import me.tongfei.progressbar.ProgressBar;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import ro.albertlr.c4.Configuration;
import ro.albertlr.c4.csv.PackageMapper;
import ro.albertlr.c4.graph.Component;
import ro.albertlr.c4.graph.Link;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Predicates.contains;
import static me.tongfei.progressbar.ProgressBarStyle.UNICODE_BLOCK;
import static ro.albertlr.c4.processor.LinksProcessor.filterExternalComponents;
import static ro.albertlr.c4.processor.LinksProcessor.filterJiveComponents;
import static ro.albertlr.c4.processor.LinksProcessor.flat;
import static ro.albertlr.c4.processor.LinksProcessor.unique;

@UtilityClass
public class PackageToComponentMapper {

    public static void mapAndSave(String inputDot, String mappingFile, String outputDot) throws IOException {
        Set<Link> outputLinks = map(inputDot, mappingFile);

        // uncomment next section if you want only clean jive components

        outputLinks = unique(
                filterExternalComponents(
                        filterJiveComponents(
                                Sets.newHashSet(outputLinks)
                        )
                )
        );

        // comment next section if you want only clean jive components

//        outputLinks = mapJiveComponents(outputLinks);

        LinksProcessor.saveToDots(
                outputDot,
                outputLinks
        );
    }

    /**
     * @param outputDot output
     * @param inputDot  input
     * @return
     */
    public static Set<Link> map(String inputDot, String mappingFile) throws IOException {
        Set<Link> links = LinksProcessor.loadLinks(inputDot);
        Map<String, String> mapping = PackageMapper.loadMapping(mappingFile);

        return map(links, mapping);
    }

    /**
     * @param outputDot
     * @param links     Original links set
     * @param mapping   package to component mapping
     * @return
     */
    public static Set<Link> map(Set<Link> links, Map<String, String> mapping) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: mapping %s links using %s mapping started ::%n", links.size(), mapping.size());
        Set<Link> updatedLinks = new UnifiedSet<>();
        Component currentFrom = null, nextFrom = null;
        Component currentTo = null, nextTo = null;
        String fqpn;
        String comp;

        Predicate<CharSequence> isJivePackage = contains(Configuration.getInstance().getLinkJivePattern());

        // try-with-resource block
        try (ProgressBar pb = new ProgressBar("Mapping", mapping.size() * links.size(), UNICODE_BLOCK)) { // name, initial max
            // Use ProgressBar("Test", 100, ProgressBarStyle.ASCII) if you want ASCII output style
            int step = 0;
            for (Link link : links) {
                currentFrom = link.getFrom();
                currentTo = link.getTo();

                boolean nextFromFound = false;
                boolean nextToFound = false;

                for (Map.Entry<String, String> map : mapping.entrySet()) {
                    fqpn = map.getKey();
                    boolean isJive = isJivePackage.apply(fqpn);
                    comp = map.getValue();

                    pb.step(); // step by 1

                    if (currentFrom.asText().equals(fqpn)) {
                        nextFrom = Component.of(comp, currentFrom.library());
                        nextFromFound = true;
                    }
                    if (currentTo.asText().equals(fqpn)) {
                        nextTo = Component.of(comp, currentTo.library());
                        nextToFound = true;
                    }
                    if (nextFromFound && nextToFound) {
//                        if (isJive) {
//                            System.out.printf(">> from %s to %s >> from %s to %s %n",
//                                    currentFrom, currentTo.toString(true),
//                                    (nextFromFound ? nextFrom : currentFrom),
//                                    (nextToFound ? nextTo : currentTo).toString(true));
//                        }
                        break;
                    }
                }
                if (!nextFromFound) {
                    nextFrom = currentFrom;
                }
                if (!nextToFound) {
                    nextTo = currentTo;
                }
                step++;
                pb.stepTo(step * mapping.size());

                updatedLinks.add(Link.of(nextFrom, nextTo));
            }
        } // progress bar stops automatically after completion of try-with-resource block

        Set<Link> outputLinks = unique(
                filterExternalComponents(
                        flat(updatedLinks)
                )
        );
        System.out.printf(":: mapping %s links completed in %s ::%n", outputLinks.size(), stopwatch);

        return outputLinks;
    }

    public static Set<Link> mapJiveComponents(Set<Link> links) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: mapping jive components %s links using started ::%n", links.size());
        Set<Link> updatedLinks = new UnifiedSet<>();
        Component currentFrom = null;
        Component currentTo = null, nextTo = null;

        Predicate<CharSequence> isJivePackage = contains(Configuration.getInstance().getLinkJivePattern());

        // try-with-resource block
        try (ProgressBar pb = new ProgressBar("Mapping Jive Components", links.size(), UNICODE_BLOCK)) { // name, initial max
            for (Link link : links) {
                currentFrom = link.getFrom();
                currentTo = link.getTo();

                boolean nextToFound = false;

                pb.step(); // step by 1

                if (isJivePackage.apply(currentTo.asText())) {
                    String library = currentTo.library();
                    if (library.startsWith("jive-core")) {
                        library = currentTo.toString(true);
                    }
                    nextTo = Component.of(library);
                    nextToFound = true;
                }

                if (!nextToFound) {
                    nextTo = currentTo;
                }

                updatedLinks.add(Link.of(currentFrom, nextTo));
            }
        } // progress bar stops automatically after completion of try-with-resource block

        Set<Link> outputLinks = unique(updatedLinks);
        System.out.printf(":: mapping Jive components %s links completed in %s ::%n", outputLinks.size(), stopwatch);

        return outputLinks;
    }
}