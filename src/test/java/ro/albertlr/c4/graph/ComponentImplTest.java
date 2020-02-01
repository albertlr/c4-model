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
package ro.albertlr.c4.graph;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ro.albertlr.c4.Configuration;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ComponentImplTest {
    @BeforeClass
    public static void beforeClass() {
        Configuration.getInstance().setPrintLibrary(true);
    }

    @Parameters(name = "{index}: {0} -> {2} & {3}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                // test case; set; asText; library
                {"com.jive.api.access.dao.impl", UnifiedSet.newSetWith("com.jive.api.access.dao.impl"), "com.jive.api.access.dao.impl", null},
                {"java.sql", UnifiedSet.newSetWith("java.sql"), "java.sql", null},
                {"java.util", UnifiedSet.newSetWith("java.util"), "java.util", null},
                {"org.apache.log4j (log4j-1.2.17.jar)", UnifiedSet.newSetWith("org.apache.log4j"), "org.apache.log4j", "log4j-1.2.17.jar"},
                {"org.springframework.beans.factory.annotation (spring-beans-4.3.22.RELEASE.jar)", UnifiedSet.newSetWith("org.springframework.beans.factory.annotation"), "org.springframework.beans.factory.annotation", "spring-beans-4.3.22.RELEASE.jar"},
                {"core-api", UnifiedSet.newSetWith("core-api"), "core-api", null},
                {"core-api, core-authorization, core-content, core-web-and-ui, core-utility (jive-core-3000.5.0-SNAPSHOT.jar)",
                        UnifiedSet.newSetWith("core-api", "core-authorization", "core-content", "core-web-and-ui", "core-utility"),
                        "core-api, core-authorization, core-content, core-utility, core-web-and-ui", "jive-core-3000.5.0-SNAPSHOT.jar"},
                {"core-authentication, core-authorization, core-audit", UnifiedSet.newSetWith("core-authentication", "core-authorization", "core-audit"),
                        "core-audit, core-authentication, core-authorization", null}
        });
    }

    private String inputText;
    private UnifiedSet<String> expectedComponents;
    private String expectedAsText;
    private String expectedLibrary;

    public ComponentImplTest(String inputText, UnifiedSet<String> expectedComponents,
                             String expectedAsText, String expectedLibrary) {
        this.inputText = inputText;
        this.expectedComponents = expectedComponents;
        this.expectedAsText = expectedAsText;
        this.expectedLibrary = expectedLibrary;
    }

    @Test
    public void assertInput() {
        ComponentImpl component = new ComponentImpl(inputText);

        for (Component comp : component) {
            assertEquals(component.hasLibrary(), comp.hasLibrary());
            if (component.hasLibrary()) {
                assertEquals(component.library(), comp.library());
            }
        }

        assertEquals(expectedComponents, component.getReferences());
        assertEquals(expectedAsText, component.asText());
        if (expectedLibrary == null) {
            assertTrue(expectedLibrary == null && !component.hasLibrary());
        } else {
            assertEquals(expectedLibrary, component.library());
        }
    }


}
