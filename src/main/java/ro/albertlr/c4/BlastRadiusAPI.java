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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import ro.albertlr.c4.csv.FunctionalAreasMapper;
import ro.albertlr.c4.csv.PackageMapper;
import ro.albertlr.c4.graph.Component;
import ro.albertlr.c4.graph.Link;
import ro.albertlr.c4.processor.LinksProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import ro.albertlr.c4.git.*;

@Slf4j
public class BlastRadiusAPI {

    final static String FUNCTIONA_AREAS_MAPPING = "jive-search-FA.csv";

    final static String COMPONENTS_MAPPINGS_DOT_FILE = "jive-search-combined.dot";

    final static String PACKAGE_TO_COMPONENTS_MAPPING = "jive-search-package-components.csv";

    String projectFolder;

    private GitIntegration gitIntegration;

    public static void main(String[] args) throws Exception {
        BlastRadiusAPI api = new BlastRadiusAPI();

        //*****************************************************************
        //small changes in shated library:
        //api.projectFolder = "D:/Programowanie/Jive/Kody/Search/jive-search-shared/.git";
        //Set<String> fa = api.getFunctionalAreaForCommints("1c09832b686cd803936a8fb42c7e24eb776675d5", "a54a626c78041aed45ce3c440ca2cb14cfb43b45");
        //*****************************************************************
        //release 3.1 for search ingress adapter (change in NG routing)
        api.projectFolder = "D:/Programowanie/Jive/Kody/Search/jive-search-ingress-adapter/.git";
        Set<String> fa = api.getFunctionalAreaForCommints("6cc77c56c13324ab9f98dfa0c50d3c83b686e503","617989cf2597a4cea4d913f937489edb5394608d");

        log.info("Affected FA:");
        fa.stream().forEach(log::info);

    }

    public Set<String> getFunctionalAreaForCommints(String commit1, String commit2) throws Exception {
        gitIntegration = new GitIntegration(projectFolder);

        Set<String> affetedPackages = gitIntegration.getModifyedPackageBetweenComits(commit1, commit2);
        log.info(": packages changed:");
        affetedPackages.forEach(log::info);

        // load componetns for packages

        Map<String, String> packageToComponentsMap = PackageMapper.loadMapping(PACKAGE_TO_COMPONENTS_MAPPING);

        // load components relation links

        Set<Link> componentsMappingLinks = LinksProcessor.loadLinks(COMPONENTS_MAPPINGS_DOT_FILE);
        Set<String> allUsedComponents = componentsMappingLinks.stream().map(link -> link.getTo().asText())
                .collect(Collectors.toSet());

        // load FA
        Map<String, Set<String>> functionalAreaMapping = FunctionalAreasMapper.loadMapping(FUNCTIONA_AREAS_MAPPING);
        int numbersOfFunctionalAreas = functionalAreaMapping.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet()).size();

        int totalNoOfComponents = allUsedComponents.size();
        int sharedNoOfComponents = (int) allUsedComponents.stream().filter(c -> c.endsWith(".jar")).count();

        log.info("{}", Strings.repeat("#", 45));
        log.info("# total components:  {}", allUsedComponents.size());
        log.info("# shared components: {}", sharedNoOfComponents);
        log.info("# components:        {}", totalNoOfComponents - sharedNoOfComponents);
        log.info("# FA:                {}", numbersOfFunctionalAreas);
        log.info("# Mapped packages:   {}", packageToComponentsMap.keySet().size());
        log.info("{}", Strings.repeat("#", 45));

        Set<String> faAffected = new HashSet<>();

        for (String affectedPackage : affetedPackages) {
            String comp = packageToComponentsMap.get(affectedPackage);
            Set<String> secondDegree = new HashSet<>();
            log.debug("***********************************{}******************************************", comp);
            Set<String> linkedComponents = loadingRelatedComponents(componentsMappingLinks, comp);
            linkedComponents.add(comp);
            log.debug("***No. of First degree components: {}***", linkedComponents.size());
            for (String s : linkedComponents) {
                log.debug(" -> {}", s);
            }
            log.debug("Blast Radius for First Degree: {}",
                    ((double) (linkedComponents.size()) / allUsedComponents.size()) * 100);
            for (String s : linkedComponents) {
                Set<String> sdc = loadingRelatedComponents(componentsMappingLinks, s);
                secondDegree.addAll(sdc);
            }
            secondDegree.add(comp);
            for (String d : secondDegree) {
                log.info(" ---> {}", d);
            }

            log.debug("***No. of Second degree components: {}***", secondDegree.size());
            log.debug("Blast Radius for Second Degree: {}",
                    ((double) (secondDegree.size()) / allUsedComponents.size()) * 100);

            Set<String> allComponents = new HashSet<>(linkedComponents);
            allComponents.addAll(secondDegree);
            log.debug("****No.of component in both level {}***", allComponents.size());
            log.debug("Blast Radius of First & Second Degree: {}", ((double) (allComponents.size()) / allUsedComponents.size()) * 100);

            // //--- NOW FUNCTIONAL AREA STUFF

            for (String component : allComponents) {
                if (!functionalAreaMapping.containsKey(component)) {
                    throw new IllegalStateException("No FA for component " + component);
                }
                log.debug("------ loading FA's for '{}'",component);
                Set<String> areas = functionalAreaMapping.get(component);
                log.debug("---------loaded: {} ", areas.size());
                log.debug(areas.toString());
                faAffected.addAll(areas);
            }

        }

        return faAffected;
    }

    private static Set<String> loadingRelatedComponents(Set<Link> links, String toSearch) throws Exception {
        Set<String> components = new HashSet<>();

        links.stream().filter(link -> link.toString().contains(toSearch)).forEach(link -> {
            Component rightSide = link.getTo();
            Component leftSide = link.getFrom();
            if (toSearch.equals(rightSide.asText())) { // <-- we want to find what component use our one, not the other
                                                       // way
                components.add(leftSide.asText());
            }
        });

        return components;
    }


}
