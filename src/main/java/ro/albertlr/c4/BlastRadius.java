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
package ro.albertlr.c4;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import ro.albertlr.c4.csv.PackageMapper;
import ro.albertlr.c4.graph.Component;
import ro.albertlr.c4.graph.Link;
import ro.albertlr.c4.processor.LinksProcessor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BlastRadius {
    public static void main(String[] args) throws Exception {

        final String mappingFile = "jive-core-v2-FAs-97-components.csv";

        Map<String, String> faMapping = PackageMapper.loadMapping(mappingFile);

        final String dotFile = "jive-core-3000.5.0.jar-fas-clean.dot.comp.dot.out";

        Set<Link> links = LinksProcessor.loadLinks(dotFile);

        final String toSearch =
                links.stream()
                        .map(link -> link.getTo().asText())
                        .collect(Collectors.joining(","));

        int numberOfFunctionalAreas = 12;

        Set<String> allUsedComponents = Sets.newHashSet(
                Splitter.on(",").trimResults()
                        .splitToList(toSearch)
        );

        int totalNoOfComponents = allUsedComponents.size();
        int sharedNoOfComponents = (int) allUsedComponents.stream().filter(c -> c.endsWith(".jar")).count();
        System.out.printf("%s%n", Strings.repeat("#", 25));
        System.out.printf("# total components:  %d%n", allUsedComponents.size());
        System.out.printf("# shared components: %d%n", sharedNoOfComponents);
        System.out.printf("# components:        %d%n", totalNoOfComponents - sharedNoOfComponents);
        System.out.printf("%s%n", Strings.repeat("#", 25));


        System.out.println("********Load FA mapping ******");

        for (String comp : allUsedComponents) {
            Set<String> secondDegree = new HashSet<>();
            System.out.printf("***********************************%s******************************************%n", comp);
            Set<String> linkedComponents = loadingRelatedComponents(links, comp);
            linkedComponents.remove(comp);
            System.out.printf("***No. of First degree components: %d***%n", linkedComponents.size());
            for (String s : linkedComponents) {
                System.out.printf(" -> %s%n", s);
            }
            System.out.printf("Blast Radius for First Degree: %f%n", ((double) (linkedComponents.size()) / allUsedComponents.size()) * 100);
            for (String s : linkedComponents) {
                Set<String> sdc = loadingRelatedComponents(links, s);
                secondDegree.addAll(sdc);
            }
            secondDegree.remove(comp);
            for (String d : secondDegree) {
                System.out.printf(" ---> %s%n", d);
            }

            System.out.printf("***No. of Second degree components: %d***%n", secondDegree.size());
            System.out.printf("Blast Radius for Second Degree: %f%n", ((double) (secondDegree.size()) / allUsedComponents.size()) * 100);

            Set<String> test = new HashSet<>(linkedComponents);
            test.addAll(secondDegree);
            System.out.printf("****No.of component in both level %d***%n", test.size());
            System.out.printf("Blast Radius of First & Second Degree: %f%n", ((double) (test.size()) / allUsedComponents.size()) * 100);

//            // --- NOW FUNCTIONAL AREA STUFF
//            Set<String> faAffected = new HashSet<>();
//            for (String component : test) {
//                if (!faMapping.containsKey(component)) {
//                    throw new IllegalStateException("No FA for component " + component);
//                }
//                faAffected.add(faMapping.get(component));
//            }
//            System.out.printf("******* FunctionalAreaAffected: %d***%n", faAffected.size());
//            for (String d : faAffected) {
//                System.out.printf(" ---FA-> %s%n", d);
//            }
//            System.out.printf("Blast Radius of First & Second level FA affeted : %f%n", ((double) (faAffected.size()) / numberOfFunctionalAreas) * 100);
        }

    }


    private static Set<String> loadingRelatedComponents(Set<Link> links, String toSearch) throws Exception {
        Set<String> components = new HashSet<>();

        links.stream()
                .filter(link -> link.toString().contains(toSearch))
                .forEach(link -> {
                    Component rightSide = link.getTo();
                    Component leftSide = link.getFrom();
                    if (toSearch.equals(rightSide.asText())) {  // <-- we want to find what component use our one, not the other way
                        components.add(leftSide.asText());
                    }
                });

        return components;
    }
}
