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

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import ro.albertlr.c4.processor.LinksProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.CharMatcher.is;
import static com.google.common.base.CharMatcher.whitespace;

@Getter
@EqualsAndHashCode
public class Link implements Iterable<Link>, Comparable<Link> {

    public static Link of(String from, String to) {
        return of(Component.of(from), Component.of(to));
    }

    public static Link of(Component from, Component to) {
        return new Link(from, to);
    }

    private Component from;
    private Component to;

    private Link(Component from, Component to) {
        this.from = from;
        this.to = to;
    }

    Link(String from, String to) {
        this.from = new ComponentImpl(from);
        this.to = new ComponentImpl(to);
    }

    /**
     * @param link syntax
     *             <ul>
     *             <li>{@code "package" -> "comp1, comp2, .., compN (lib)";}</li>
     *             </ul>
     */
    public Link(String link) {
        // remove semi collon if is there
        link = link.replaceFirst(";$", "");

        // should get: {@code {"pacakge", "comp1, comp2, .., compN (lib)"}} (without the quotes)
        List<String> compToComp = Splitter.on("->")
                .trimResults(whitespace().and(is('\"'))) // trim whitespaces and quotes (")
                .omitEmptyStrings()
                .limit(2)
                .splitToList(link);

        // should get: {@code pacakge}
        from = new ComponentImpl(compToComp.get(0));
        // should get: {@code comp1, comp2, .., compN (lib)}
        to = new ComponentImpl(compToComp.get(1));
    }

    @Override
    public String toString() {
        return "  \"" + from.toString() + "\" -> \"" + to.toString() + "\";";
    }

    public static void main(String[] args) {
        String dotFile = args[0];

        Set<Link> links = LinksProcessor.loadLinks(dotFile);

        LinksProcessor.saveToCsv(dotFile + ".csv", links);
    }

    public String asCsv() {
        return from.asText() + "\t" + to.asText();
    }

    private volatile transient Collection<Link> links;

    @Override
    public Iterator<Link> iterator() {
        if (links == null) {
            synchronized (this) {
                if (links == null) {
                    links = new ArrayList<>();
                    for (Component f : from) {
                        for (Component t : to) {
                            links.add(new Link(f, t));
                        }
                    }
                }
            }
        }
        return links.iterator();
    }

    @Override
    public int compareTo(Link other) {
        return ComparisonChain.start()
                .compare(from, other.from)
                .compare(to, other.to)
                .result();
    }
}
