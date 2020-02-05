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
package ro.albertlr.c4.maven;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ro.albertlr.c4.maven.SourceOf.Type;

import java.io.File;

import static ro.albertlr.c4.maven.SourceOf.Type.ASPECTJ;
import static ro.albertlr.c4.maven.SourceOf.Type.JAVA;
import static ro.albertlr.c4.maven.SourceOf.Type.JAVA_SCRIPT;
import static ro.albertlr.c4.maven.SourceOf.Type.RESOURCES;


@Getter
@ToString
@EqualsAndHashCode(of = {"root"})
public class Module {
    private File root;
    private String pomFile;
    @Setter
    private String mavenId;
    @Setter
    private String groupId;
    @Setter
    private String artifactId;
    @Setter
    private String version;
    private SourceOf srcJava;
    private SourceOf srcJavaScript;
    private SourceOf srcAspect;
    private SourceOf srcResources;

    @Builder
    public Module(File root) {
        this.root = root;
        File pom = new File(root, "pom.xml");
        if (pom.exists()) {
            this.pomFile = pom.getPath();
            MavenUtils.refreshModule(this);
        }
        this.srcJava = srcDirOf(root, JAVA, true, "src/main/java");
        this.srcJavaScript = srcDirOf(root, JAVA_SCRIPT, false, "src/main/javascript", "src/javascript");
        this.srcAspect = srcDirOf(root, ASPECTJ, false, "src/main/aspect");
        this.srcResources = srcDirOf(root, RESOURCES, false, "src/main/resources");
    }

    public String getBaseSrcJava() {
        return srcJava.getSrcDir().getName();
    }

    private static final SourceOf srcDirOf(File root, Type type, boolean validate, String... srcDirs) {
        for (String srcDir : srcDirs) {
            File folder = new File(root, srcDir);
            if ((!folder.exists() || !folder.isDirectory())) {
                if (validate) {
                    throw new IllegalStateException("No root source dir for type " + type + " having root folder " + root.toString());
                } else {
                    continue;
                }
            }
            return SourceOf.builder()
                    .srcDir(folder)
                    .type(type)
                    .build();
        }
        return null;
    }

    public SourceOf getSourceOf(SourceOf.Type type) {
        switch (type) {
            case JAVA:
                return getSrcJava();
            case JAVA_SCRIPT:
                return getSrcJavaScript();
            case RESOURCES:
                return getSrcResources();
            default:
                throw new IllegalArgumentException("Unknown type " + type);
        }
    }

}
