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

import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import lombok.AccessLevel;
import lombok.Getter;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import ro.albertlr.c4.Configuration;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class ComponentImpl implements Component {
    @Getter(AccessLevel.PACKAGE)
    private MutableSet<String> references;
    private String library;

    // keep-pre-computed asText for speed
    private transient String asText;

    ComponentImpl(String component, String library) {
//        this.references = UnifiedSet.newSetWith(component);
        this(component);
        this.library = library;
    }

    /**
     * @param component The component. Can have the following syntax:
     *                  <ul>
     *                  <li>single component: {@code comp}</li>
     *                  <li>single component and library: {@code comp (lib)}</li>
     *                  <li>multiple components: {@code comp1, comp2, comp3, ..., compN}</li>
     *                  <li>multiple components and library, {@code comp1, comp2, comp3, ..., compN (lib)}</li>
     *                  </ul>
     */
    public ComponentImpl(String component) {
        if (isNullOrEmpty(component)) {
            this.library = null;
            this.references = new UnifiedSet<>();
            return;
        }
        // remove quote (") from the string: "comp" -> comp
        component = CharMatcher.is('\"').removeFrom(component);

        // expected format: "comp1, comp2, comp3 (library)"
        int startBracketPos = component.indexOf("(");
        if (startBracketPos != -1 && component.contains(")")) {
            this.library = component.substring(
                    startBracketPos + 1,
                    component.indexOf(")")
            ).trim();
            component = component.substring(0, startBracketPos).trim();
        }
        Collection<String> individualComponents = Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .splitToList(component);
        this.references = UnifiedSet.newSet(individualComponents);
    }

    @Override
    public String asText() {
        if (references.size() == 1) {
            return references.getFirst();
        }
        if (asText == null) {
            asText = references.toSortedSet().stream()
                    .collect(Collectors.joining(", "));
        }
        return asText;
    }

    @Override
    public boolean hasLibrary() {
        return !isNullOrEmpty(library);
    }

    @Override
    public String library() {
        return library;
    }

    @Override
    public String component() {
        return references.getFirst();
    }

    @Override
    public String component(int index) {
        return Iterables.get(references, index, null);
    }

    @Override
    public Iterator<Component> iterator() {
        return references.collect(s -> (Component) new ComponentImpl(s, library))
                .iterator();
    }

    @Override
    public String toString() {
        return toString(usePrintLibrary());
    }

    private static boolean usePrintLibrary() {
        return Configuration.getInstance().isPrintLibrary();
    }

    public String toString(boolean printLibrary) {
        return printLibrary && hasLibrary() ? asText() + " (" + library + ')' : asText();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComponentImpl)) {
            return false;
        }
        ComponentImpl that = (ComponentImpl) o;

        return Objects.equal(references, that.references) &&
                (usePrintLibrary() && Objects.equal(library, that.library));
    }

    @Override
    public final int hashCode() {
        String libraryReplacement = usePrintLibrary() ? library : "";
        return Objects.hashCode(references, libraryReplacement);
    }

    @Override
    public int compareTo(Component o) {
        ComparisonChain comparison = ComparisonChain.start()
                .compare(references.toSortedSet(), ((ComponentImpl) o).references.toSortedSet());
        if (hasLibrary()) {
            comparison.compare(library, o.library());
        }
        return comparison.result();
    }
}
