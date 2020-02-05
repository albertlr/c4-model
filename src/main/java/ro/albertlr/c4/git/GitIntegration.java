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
package ro.albertlr.c4.git;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.io.DisabledOutputStream;

public class GitIntegration {

    private static final String SOURCE_FOLDER_PREFIX = "src/main/java";
    private static final String SOURCE_FOLDER_PREFIX_REGEXP = ".*\\/src\\/main\\/java\\/";
    private static final String SOURCE_FILE_REGEXP = "\\/[A-Z].*.java";
    private static final String SOURCE_FILE_EXTENSION = ".java";

    static Repository repository;

    public GitIntegration(String dotGitFolderURL) throws IOException {
        repository = new FileRepositoryBuilder().setGitDir(new File(dotGitFolderURL)).build();
    }

    public Set<String> getPackageChangedBetweenCommits(String commit1, String comit2) throws IOException{
        return getFilesChangedBeteenCommits( commit1,  comit2).stream()
        .filter(path -> path.contains(SOURCE_FOLDER_PREFIX))
        .filter(path -> path.contains(SOURCE_FILE_EXTENSION))
        .map(path -> path.replaceFirst(SOURCE_FOLDER_PREFIX_REGEXP, "")
        .replaceFirst(SOURCE_FILE_REGEXP, "")
        .replaceAll("\\/", "."))
        .collect(Collectors.toSet());
    }

    public Set<String> getFilesChangedBeteenCommits(String commitID1, String commitID2) throws IOException {
        ObjectId commit1 = ObjectId.fromString(commitID1);
        ObjectId commit2 = ObjectId.fromString(commitID2);

        DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
        diffFormatter.setRepository(repository);
        Set<String> updatedFiles = new HashSet<>();
        List<DiffEntry> entries = diffFormatter.scan(commit1, commit2);

        updatedFiles.addAll(entries.stream().map(e -> e.getNewPath()).collect(Collectors.toList()));

        diffFormatter.close();
        return updatedFiles;

    }

}
