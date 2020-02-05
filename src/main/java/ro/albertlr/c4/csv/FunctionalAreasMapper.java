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

import com.google.common.base.Stopwatch;
import lombok.experimental.UtilityClass;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FunctionalAreasMapper {

    private static final int PACKAGE_IDX = 0;
     

    public static Map<String, Set<String>> loadMapping(String csv) throws IOException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        System.out.printf(":: mapping CSV %s loading started ::%n", csv);
        Map<String, Set<String>> mapping = new HashMap<>();

        Reader input = new FileReader(csv);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withDelimiter(';')
                .withFirstRecordAsHeader()
                .parse(input);

       

        for (CSVRecord record : records) {
            Set<String> areas = new HashSet<>();
            for(int i = 1; i < record.size(); i++){
                areas.add(record.get(i));
            }
            mapping.put(
                    record.get(PACKAGE_IDX),
                    areas
            );
        }

        System.out.printf(":: mapping CSV %s loading completed in %s ::%n", csv, stopwatch);
        return mapping;
    }

}
