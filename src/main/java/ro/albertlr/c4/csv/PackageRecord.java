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

import lombok.Builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static ro.albertlr.c4.csv.Header.SourceType;

@Builder
public class PackageRecord implements Iterable<String> {
    private String module;
    private String sourceType;
    private String packageName;

    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(module, sourceType, packageName)
                .iterator();
    }

    public Iterable<String> asIterable(Header[] headers) {
        Collection<String> iterable = new ArrayList<>();
        for (Header header : headers) {
            // using switch it was keep getting me NoClassDefFoundError ..
            // see https://stackoverflow.com/questions/24473247/java-enum-noclassdeffounderror
            if (header == Header.Module) {
                iterable.add(module);
            } else if (header == Header.SourceType) {
                iterable.add(sourceType);
            } else if (header == Header.Package) {
                iterable.add(packageName);
            }
        }
        return iterable;
    }
}
