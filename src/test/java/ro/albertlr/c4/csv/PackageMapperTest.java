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

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertThat;

public class PackageMapperTest {

    @Test
    public void loadMapping() throws Exception {

        Map<String, String> mapping = PackageMapper.loadMapping("test-input.csv");

        System.out.println(mapping);

        assertThat(mapping, hasEntry("com.jive.api.access", "core-api"));
        assertThat(mapping, hasEntry("com.jive.api.aaa", "core-authentication, core-authorization, core-audit"));
        assertThat(mapping, hasEntry("resources/internationalization", "core-i18n"));

    }
}
