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

import com.google.common.collect.Iterables;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import ro.albertlr.c4.Configuration;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.singleton;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class LinkTest {

    @BeforeClass
    public static void beforeClass() {
        Configuration.getInstance().setPrintLibrary(true);
    }

    @Parameters(name = "{index}: {0} ")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"   \"com.jivesoftware.search.ingress\"                  -> \"com.google.inject (guice-4.1.0.jar)\";", singleton(new Link("com.jivesoftware.search.ingress", "com.google.inject (guice-4.1.0.jar)"))},
                {"   \"com.jivesoftware.search.ingress\"                  -> \"com.google.inject.binder (guice-4.1.0.jar)\";", singleton(new Link("com.jivesoftware.search.ingress", "com.google.inject.binder (guice-4.1.0.jar)"))},
                {"\"com.jivesoftware.cloud.ingressadapter.models\"     -> \"java.lang\";", singleton(new Link("com.jivesoftware.cloud.ingressadapter.models", "java.lang"))},
                {"\"core-authentication, core-authorization, core-audit\" -> \"java.lang\";", Arrays.asList(
                        new Link("core-authentication", "java.lang"),
                        new Link("core-authorization", "java.lang"),
                        new Link("core-audit", "java.lang")
                )},
                {"   \"core-authentication, core-authorization, core-audit\" -> \"org.apache.commons.lang3 (commons-lang3-3.3.2.jar)\";", Arrays.asList(
                        new Link("core-authentication", "org.apache.commons.lang3 (commons-lang3-3.3.2.jar)"),
                        new Link("core-authorization", "org.apache.commons.lang3 (commons-lang3-3.3.2.jar)"),
                        new Link("core-audit", "org.apache.commons.lang3 (commons-lang3-3.3.2.jar)")
                )},
                {"   \"core-authentication, core-authorization, core-audit\" -> \"core-authentication, core-authorization, core-audit (jive-core-3000.5.0-SNAPSHOT.jar)\";", Arrays.asList(
                        new Link("core-authentication", "core-authentication (jive-core-3000.5.0-SNAPSHOT.jar)"),
                        new Link("core-authorization", "core-authentication (jive-core-3000.5.0-SNAPSHOT.jar)"),
                        new Link("core-audit", "core-authentication (jive-core-3000.5.0-SNAPSHOT.jar)"),

                        new Link("core-authentication", "core-authorization (jive-core-3000.5.0-SNAPSHOT.jar)"),
                        new Link("core-authorization", "core-authorization (jive-core-3000.5.0-SNAPSHOT.jar)"),
                        new Link("core-audit", "core-authorization (jive-core-3000.5.0-SNAPSHOT.jar)"),

                        new Link("core-authentication", "core-audit (jive-core-3000.5.0-SNAPSHOT.jar)"),
                        new Link("core-authorization", "core-audit (jive-core-3000.5.0-SNAPSHOT.jar)"),
                        new Link("core-audit", "core-audit (jive-core-3000.5.0-SNAPSHOT.jar)")
                )},
        });
    }

    private String linkText;
    private Collection<Link> links;

    public LinkTest(String link, Collection<Link> links) {
        this.linkText = link;
        this.links = links;
    }

    @Test
    public void assertTest() {
        Link link = new Link(linkText);

        System.out.printf("link: %s%n", link);

        for (Link l : link) {
            System.out.printf("    %s%n", l);
            assertThat(l, containsInAnyOrder(l));
        }

        assertEquals(Iterables.size(link), links.size());
    }

}
