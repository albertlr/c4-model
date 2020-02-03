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
import org.apache.commons.cli.CommandLine;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ro.albertlr.c4.csv.PackageMapper;
import ro.albertlr.c4.graph.Component;
import ro.albertlr.c4.graph.Link;
import ro.albertlr.c4.processor.LinksProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BlastRadius {
    private static final String[] COLUMNS = {"Component", "No. of First Degree Components", "First Degree Blast Radius", "No. of Second Degree Components", "Second Degree Blast Radius", "Blast Radius", "First Degree Components", "Second Degree Components"};
    public static void main(String[] args) throws Exception {
        CommandLine cli = Params.blastRadiusCli(args);

        final String mappingFile = Params.getParameter(cli, Params.CSV_FILE_ARG); // "jive-core-v2-FAs-97-components.csv";
        final String dotFile = Params.getParameter(cli, Params.DOT_FILE_ARG); // "jive-core-3000.5.0.jar-fas-clean.dot.comp.dot.out";
        final String xlsxFile = Params.getParameter(cli, Params.XLS_FILE_ARG, "blast-radius-output.xlsx");

        Map<String, String> faMapping = PackageMapper.loadMapping(mappingFile);

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

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = prepareExcelFile(workbook);
        File file = new File(xlsxFile);
        FileOutputStream outputStream = new FileOutputStream(file);
        try {

            int rowNum = 1;
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

                Set<String> allComponents = new HashSet<>(linkedComponents);
                allComponents.addAll(secondDegree);
                System.out.printf("****No.of component in both level %d***%n", allComponents.size());
                System.out.printf("Blast Radius of First & Second Degree: %f%n", ((double) (allComponents.size()) / allUsedComponents.size()) * 100);

                writeToExcel(linkedComponents, secondDegree, allComponents, totalNoOfComponents, comp, rowNum++, sheet);

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
        } finally {
            workbook.write(outputStream);
            outputStream.close();
            workbook.close();
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

    private static Sheet prepareExcelFile(Workbook workbook) throws FileNotFoundException {

        Sheet sheet = workbook.createSheet("BlastRadius");
        // Create a Font for styling header cells
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setColor(IndexedColors.RED.getIndex());

        // Create a CellStyle with the font
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        // Create a Row
        Row headerRow = sheet.createRow(0);
        // Create cells
        for(int i = 0; i < COLUMNS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(COLUMNS[i]);
            cell.setCellStyle(headerCellStyle);
        }

        return sheet;
    }

    private static void writeToExcel(Set<String> first, Set<String> second, Set<String> combined, int totalComponents, String component, int rowNum, Sheet sheet) {

        try {
            Row row = sheet.createRow(rowNum);
            row.createCell(0)
                    .setCellValue(component);
            row.createCell(1)
                    .setCellValue(first.size());
            row.createCell(2)
                    .setCellValue(((double) (first.size()) / totalComponents) * 100);
            row.createCell(3)
                    .setCellValue(second.size());
            row.createCell(4)
                    .setCellValue(((double) (second.size()) / totalComponents) * 100);
            row.createCell(5)
                    .setCellValue(((double) (combined.size()) / totalComponents) * 100);
            StringBuilder firstDegree = new StringBuilder();
            for (String s : first) {
                firstDegree.append(s + "\n");
            }
            row.createCell(6)
                    .setCellValue(firstDegree.toString());
            StringBuilder secondDegree = new StringBuilder();
            for (String s : second) {
                if (!first.contains(s)) {
                    secondDegree.append(s + "\n");
                }
            }
            row.createCell(7)
                    .setCellValue(secondDegree.toString());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.err.println("Excel's Limit reached.");
        }
    }
}
