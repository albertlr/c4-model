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

    private static final String SORUCE_FOLDER_PREFIX = "src/main/java";
    private static final String SORUCE_FOLDER_PREFIX_REGEXP = ".*\\/src\\/main\\/java\\/";
    private static final String SORUCE_FILE_REGEXP = "\\/[A-Z].*.java";
    private static final String SOURCE_FILE_EXTENSION = ".java";

    static Repository repository;

    public GitIntegration(String dotGitFolderURL) throws IOException {
        repository = new FileRepositoryBuilder().setGitDir(new File(dotGitFolderURL)).build();
    }

    public static void main(String[] args) throws Exception {

        // ObjectId commit1 = ObjectId.fromString("8b8cf76921553173e6a52cd94a66be9ffdeab8fd");
        // ObjectId commit2 = ObjectId.fromString("c011c4a5977be3d0eaf2d5536a87782879b84910");

        GitIntegration gitIntegration = new GitIntegration(
                "D:/Programowanie/Jive/Kody/Search/jive-search-ingress/.git");
        System.out.println(gitIntegration.getModifyedPackageBetweenComits("8b8cf76921553173e6a52cd94a66be9ffdeab8fd",
                "c011c4a5977be3d0eaf2d5536a87782879b84910"));

                System.out.println("service/src/main/java/com/jivesoftware/search/ingress/pollers/ProfileIngressSQSPoller.java".contains(".java"));

        // System.out.println("service/src/main/java/com/jivesoftware/search/ingress/pollers/ProfileIngressSQSPoller.java"
        // .replaceFirst(SORUCE_FOLDER_PREFIX_REGEXP, "")
        // .replaceFirst(, "")
        // .replaceAll("\\/", "."));
    }

    public Set<String> getModifyedPackageBetweenComits(String commit1, String comit2) throws IOException{
        return getFilesChangedBeteenCommits( commit1,  comit2).stream()
        .filter(path -> path.contains(SORUCE_FOLDER_PREFIX))
        .filter(path -> path.contains(SOURCE_FILE_EXTENSION))
        .map(path -> path.replaceFirst(SORUCE_FOLDER_PREFIX_REGEXP, "")
        .replaceFirst(SORUCE_FILE_REGEXP, "")
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
