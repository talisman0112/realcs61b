package gitlet;

import javax.swing.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Utils.*;

public class Repository {


    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File REFS_DIR = join(GITLET_DIR, "refs");
    public static final File HEADS_DIR = join(REFS_DIR, "heads");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File INDEX = join(GITLET_DIR, "index");



    private void validateInitialized() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        GITLET_DIR.mkdirs();
        OBJECTS_DIR.mkdirs();
        REFS_DIR.mkdirs();
        HEADS_DIR.mkdirs();

        Commit initial = new Commit();
        saveCommit(initial);

        writeObject(INDEX, new StagingArea());
        writeContents(HEAD, "ref: refs/heads/master");
        writeContents(join(HEADS_DIR, "master"), initial.getHash());
    }

    public void add(String fileName) {
        validateInitialized();
        File file = join(CWD, fileName);
        if (!file.exists()) {
            message("File does not exist.");
            System.exit(0);
        }

        Blob blob = new Blob(file);
        saveBlob(blob);

        StagingArea stage = readObject(INDEX, StagingArea.class);
        Commit head = getHeadCommit();

        String oldHash = head.getBlobs().get(fileName);
        if (oldHash != null && oldHash.equals(blob.getHash())) {
            stage.getAddition().remove(fileName);
        } else {
            stage.getAddition().put(fileName, blob.getHash());
        }
        stage.getRemoval().remove(fileName);
        writeObject(INDEX, stage);
    }

    public void commit(String message) {
        validateInitialized();
        if (message.isEmpty()) {
            message("Please enter a commit message.");
            System.exit(0);
        }
        StagingArea stage = readObject(INDEX, StagingArea.class);
        if (stage.isEmpty()) {
            message("No changes added to the commit.");
            System.exit(0);
        }
        Commit parent = getHeadCommit();
        Map<String, String> blobs = new HashMap<>(parent.getBlobs());
        blobs.putAll(stage.getAddition());
        for (String rm : stage.getRemoval()) blobs.remove(rm);

        Commit newCommit = new Commit(message, parent.getHash(), blobs);
        saveCommit(newCommit);
        updateBranchPointer(newCommit.getHash());

        stage.clear();
        writeObject(INDEX, stage);
    }

    public void rm(String fileName) {
        validateInitialized();
        StagingArea stage = readObject(INDEX, StagingArea.class);
        Commit head = getHeadCommit();

        boolean staged = stage.getAddition().containsKey(fileName);
        boolean tracked = head.getBlobs().containsKey(fileName);

        if (!staged && !tracked) {
            message("No reason to remove the file.");
            System.exit(0);
        }

        if (staged) stage.getAddition().remove(fileName);
        if (tracked) {
            stage.getRemoval().add(fileName);
            restrictedDelete(join(CWD, fileName));
        }
        writeObject(INDEX, stage);
    }

    public void log() {
        validateInitialized();
        Commit cur = getHeadCommit();
        while (cur != null) {
            System.out.println("===");
            System.out.println("commit " + cur.getHash());
            System.out.println("Date: " + cur.getTimestampString());
            System.out.println(cur.getMessage());
            System.out.println();

            String parentHash = cur.getParent();
            cur = (parentHash == null) ? null : readCommit(parentHash);
        }
    }

    public void globalLog() {
        validateInitialized();

        for (String hash : plainFilenamesIn(OBJECTS_DIR)) {
            Commit c = readCommit(hash);
            if (c == null) continue;
            System.out.println("===");
            System.out.println("commit " + hash);
            System.out.println("Date: " + c.getTimestampString());
            System.out.println(c.getMessage());
            System.out.println();
        }
    }

    public void find(String message) {
        validateInitialized();
        List<String> FILE = plainFilenamesIn(OBJECTS_DIR);
        boolean found = false;
        for (String hash : FILE) {
            Commit c = readCommit(hash);
            if (c != null && c.getMessage().equals(message)) {
                System.out.println(hash);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        validateInitialized();
        if (!GITLET_DIR.exists()) {
            message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        System.out.println("=== Branches ===");
        String currentBranch = getCurrentBranchName();
        for (String b : plainFilenamesIn(HEADS_DIR)) {
            System.out.println(b.equals(currentBranch) ? "*" + b : b);
        }
        System.out.println();

        StagingArea stage = readObject(INDEX, StagingArea.class);
        Commit head = getHeadCommit();

        System.out.println("=== Staged Files ===");
        sortedStrings(stage.getAddition().keySet()).forEach(System.out::println);
        System.out.println();

        System.out.println("=== Removed Files ===");
        sortedStrings(stage.getRemoval()).forEach(System.out::println);
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String name : plainFilenamesIn(CWD)) {
            if (!head.getBlobs().containsKey(name) && !stage.getAddition().containsKey(name)) {
                System.out.println(name);
            }
        }
        System.out.println();
    }

    // checkout -- [fileName]
    public void checkoutFile(String fileName) {
        checkoutFileFromCommit(getHeadCommit().getHash(), fileName);
    }

    // checkout [commitId] -- [fileName]
    public void checkoutFile(String commitId, String fileName) {
        validateInitialized();

        if (commitId.length() < UID_LENGTH) {
            commitId = findCommitByPrefix(commitId);
            if (commitId == null) {
                message("No commit with that id exists.");
                System.exit(0);
            }
        }

        checkoutFileFromCommit(commitId, fileName);
    }
    // checkout [branchName]
    public void checkoutBranch(String branchName) {
        validateInitialized();
        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            message("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(getCurrentBranchName())) {
            message("No need to checkout the current branch.");
            System.exit(0);
        }

        String targetHash = readContentsAsString(branchFile);
        Commit target = readCommit(targetHash);


        if (target != null) {
            for (String name : target.getBlobs().keySet()) {
                File f = join(CWD, name);
                if (f.exists() && !getHeadCommit().getBlobs().containsKey(name)
                        && !readObject(INDEX, StagingArea.class).getAddition().containsKey(name)) {
                    message("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        for (String name : getHeadCommit().getBlobs().keySet()) {
            restrictedDelete(join(CWD, name));
        }
        StagingArea stage = readObject(INDEX, StagingArea.class);
        for (String name : stage.getAddition().keySet()) {
            restrictedDelete(join(CWD, name));
        }

        for (Map.Entry<String, String> e : target.getBlobs().entrySet()) {
            Blob b = readBlob(e.getValue());
            writeContents(join(CWD, e.getKey()),(Object) b.getContent());
        }

        writeObject(INDEX, new StagingArea());
        writeContents(HEAD, "ref: refs/heads/" + branchName);
    }

    public void branch(String branchName) {
        validateInitialized();
        if (join(HEADS_DIR, branchName).exists()) {
            message("A branch with that name already exists.");
            System.exit(0);
        }
        writeContents(join(HEADS_DIR, branchName), getHeadCommit().getHash());
    }
    public void rm_branch(String branchname){
        validateInitialized();
        String currentcommitbranch=getCurrentBranchName();
        if(currentcommitbranch.equals(branchname)){
            message("Cannot remove the current branch.");
            System.exit(0);
        }
        File F=join(HEADS_DIR,branchname);
            if (!F.exists()) {
                message("A branch with that name does not exist.");
                System.exit(0);
            }
            F.delete();

    }
    // like checkout
    public void reset(String commitId){
        validateInitialized();
        if (commitId.length() < UID_LENGTH) {
            commitId = findCommitByPrefix(commitId);
            if (commitId == null) {
                message("No commit with that id exists.");
                System.exit(0);
            }
        }
        Commit targetcommit=readCommit(commitId);
        if (targetcommit==null){
            message("No commit with that id exists");
            System.exit(0);
        }
        for (Map.Entry<String, String> entry : targetcommit.getBlobs().entrySet()) {
            String fileName = entry.getKey();
            File file = join(CWD, fileName);
            if (file.exists() && !getHeadCommit().getBlobs().containsKey(fileName)
                    && !readObject(INDEX, StagingArea.class).getAddition().containsKey(fileName)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        // delete all heads file
        List<String> allFilesInCWD = plainFilenamesIn(CWD);
        if (allFilesInCWD != null) {
            for (String fileName : allFilesInCWD) {
                restrictedDelete(join(CWD, fileName));
            }
        }
        // cover
        for (Map.Entry<String, String> entry : targetcommit.getBlobs().entrySet()) {
            Blob blob = readBlob(entry.getValue());
            writeContents(join(CWD, entry.getKey()),(Object) blob.getContent());
        }
        writeObject(INDEX, new StagingArea());
        updateBranchPointer(targetcommit.getHash());

    }
    public void merge(String branchname) {
        validateInitialized();

        File branchFile = join(HEADS_DIR, branchname);
        if (!branchFile.exists()) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }

        String currentBranch = getCurrentBranchName();
        if (currentBranch.equals(branchname)) {
            message("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Commit currentCommit = getHeadCommit();
        Commit givenCommit = readCommit(readContentsAsString(branchFile));

        // ========== 关键：正确的祖先判断，彻底解决 test36a ==========
        if (isAncestor(givenCommit, currentCommit)) {
            message("Given branch is an ancestor of the current branch.");
            return;
        }

        if (isAncestor(currentCommit, givenCommit)) {
            checkoutBranch(branchname);
            writeObject(INDEX, new StagingArea());
            updateBranchPointer(givenCommit.getHash());
            message("Current branch fast-forwarded.");
            return;
        }
        // ==================================================================

        // 检查是否有未提交的更改（staging area）
        StagingArea stagingArea = readObject(INDEX, StagingArea.class);
        if (!stagingArea.isEmpty()) {
            message("You have uncommitted changes.");
            System.exit(0);
        }

        // 检查工作区是否有修改但未add的文件
        for (Map.Entry<String, String> entry : currentCommit.getBlobs().entrySet()) {
            String fileName = entry.getKey();
            String headBlobHash = entry.getValue();
            File fileInCWD = join(CWD, fileName);
            if (fileInCWD.exists()) {
                Blob currentBlob = new Blob(fileInCWD);
                if (!currentBlob.getHash().equals(headBlobHash)) {
                    message("You have uncommitted changes.");
                    System.exit(0);
                }
            } else {
                message("You have uncommitted changes.");
                System.exit(0);
            }
        }

        // 检查 untracked file in the way
        for (String name : givenCommit.getBlobs().keySet()) {
            File f = join(CWD, name);
            if (f.exists()
                    && !currentCommit.getBlobs().containsKey(name)
                    && !stagingArea.getAddition().containsKey(name)) {
                message("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }

        // ========== 下面开始真正的三路合并 ==========
        // 我们直接用你原来的手动找 split point 的方式，但这次要正确遍历 secondParent！
        // （因为你不想加新方法，我们就修改原来的循环，加入 secondParent 遍历）

        Set<String> currentAncestors = new HashSet<>();
        Queue<Commit> queue = new LinkedList<>();
        queue.add(currentCommit);
        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            Commit temp = queue.remove();
            currentAncestors.add(temp.getHash());
            visited.add(temp.getHash());

            String parentHash = temp.getParent();
            if (parentHash != null && !visited.contains(parentHash)) {
                queue.add(readCommit(parentHash));
            }
            String secondParentHash = temp.getSecondParent();
            if (secondParentHash != null && !visited.contains(secondParentHash)) {
                queue.add(readCommit(secondParentHash));
            }
        }

        // 从 givenCommit 往前找第一个在 currentAncestors 中的 commit
        queue.clear();
        visited.clear();
        queue.add(givenCommit);
        visited.add(givenCommit.getHash());

        String splitcommithash = null;
        while (!queue.isEmpty()) {
            Commit temp = queue.remove();
            if (currentAncestors.contains(temp.getHash())) {
                splitcommithash = temp.getHash();
                break;
            }

            String parentHash = temp.getParent();
            if (parentHash != null && !visited.contains(parentHash)) {
                visited.add(parentHash);
                queue.add(readCommit(parentHash));
            }
            String secondParentHash = temp.getSecondParent();
            if (secondParentHash != null && !visited.contains(secondParentHash)) {
                visited.add(secondParentHash);
                queue.add(readCommit(secondParentHash));
            }
        }

        if (splitcommithash == null) {
            message("Internal error: no split point found.");
            System.exit(0);
        }

        Commit splitcommit = readCommit(splitcommithash);
        Commit curcommit = currentCommit;      // 统一用 currentCommit
        Commit branchcommit = givenCommit;     // 统一用 givenCommit

        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(splitcommit.getBlobs().keySet());
        allFiles.addAll(curcommit.getBlobs().keySet());
        allFiles.addAll(branchcommit.getBlobs().keySet());

        boolean hasConflict = false;
        Map<String, String> mergedBlobs = new HashMap<>(curcommit.getBlobs());

        for (String fileName : allFiles) {
            String splitBlob = splitcommit.getBlobs().get(fileName);
            String headBlob   = curcommit.getBlobs().get(fileName);
            String branchBlob = branchcommit.getBlobs().get(fileName);

            if (Objects.equals(headBlob, branchBlob)) {
                if (headBlob != null) {
                    mergedBlobs.put(fileName, headBlob);
                } else {
                    mergedBlobs.remove(fileName);
                    restrictedDelete(join(CWD, fileName));
                }
                continue;
            }

            if (Objects.equals(splitBlob, headBlob)) {
                if (branchBlob != null) {
                    mergedBlobs.put(fileName, branchBlob);
                    writeContents(join(CWD, fileName), (Object) readBlob(branchBlob).getContent());
                } else {
                    mergedBlobs.remove(fileName);
                    restrictedDelete(join(CWD, fileName));
                }
                continue;
            }

            if (Objects.equals(splitBlob, branchBlob)) {
                if (headBlob != null) {
                    mergedBlobs.put(fileName, headBlob);
                } else {
                    mergedBlobs.remove(fileName);
                }
                continue;
            }

            if (splitBlob != null && headBlob == null && branchBlob == null) {
                mergedBlobs.remove(fileName);
                restrictedDelete(join(CWD, fileName));
                continue;
            }

            hasConflict = true;

            String headContent = headBlob == null ? "" : new String(readBlob(headBlob).getContent(), StandardCharsets.UTF_8);
            String branchContent = branchBlob == null ? "" : new String(readBlob(branchBlob).getContent(), StandardCharsets.UTF_8);

            String conflictContent =
                    "<<<<<<< HEAD\n" +
                            headContent +
                            "=======\n" +
                            branchContent +
                            ">>>>>>>\n";

            File file = join(CWD, fileName);
            writeContents(file, conflictContent);

            Blob conflictBlob = new Blob(conflictContent.getBytes(StandardCharsets.UTF_8));
            saveBlob(conflictBlob);
            mergedBlobs.put(fileName, conflictBlob.getHash());
        }

        if (hasConflict) {
            message("Encountered a merge conflict.");
        }

        String mergeMessage = "Merged " + branchname + " into " + currentBranch + ".";
        Commit mergeCommit = new Commit(mergeMessage, curcommit.getHash(), branchcommit.getHash(), mergedBlobs);
        saveCommit(mergeCommit);
        writeObject(INDEX, new StagingArea());
        updateBranchPointer(mergeCommit.getHash());
    }
//    public void merge(String branchname) {
//        validateInitialized();
//        File F = join(HEADS_DIR, branchname);
//        if (!F.exists()) {
//            message("A branch with that name does not exist.");
//            System.exit(0);
//        }
//        String currentbranch = getCurrentBranchName();
//        if (currentbranch.equals(branchname)) {
//            message("Cannot merge a branch with itself.");
//            System.exit(0);
//        }
//        Commit currentCommit = getHeadCommit();
//        Commit givenCommit = readCommit(readContentsAsString(join(HEADS_DIR, branchname)));
//
//// 情况1: given branch 是 current branch 的祖先 → 什么都不做
//        if (isAncestor(givenCommit, currentCommit)) {
//            message("Given branch is an ancestor of the current branch.");
//            return;
//        }
//
//// 情况2: current branch 是 given branch 的祖先 → fast-forward
//        if (isAncestor(currentCommit, givenCommit)) {
//            checkoutBranch(branchname);
//            writeObject(INDEX, new StagingArea());
//            updateBranchPointer(givenCommit.getHash());
//            message("Current branch fast-forwarded.");
//            return;
//        }
//        StagingArea stagingArea = readObject(INDEX, StagingArea.class);
//        if (!stagingArea.isEmpty()) {
//            message("You have uncommitted changes.");
//            System.exit(0);
//        }
//        Commit head = getHeadCommit();
//        for (Map.Entry<String, String> entry : head.getBlobs().entrySet()) {
//            String fileName = entry.getKey();
//            String headBlobHash = entry.getValue();
//            File fileInCWD = join(CWD, fileName);
//            if (fileInCWD.exists()) {
//                Blob currentBlob = new Blob(fileInCWD);
//                if (!currentBlob.getHash().equals(headBlobHash)) {
//                    message("You have uncommitted changes.");
//                    System.exit(0);
//                }
//            } else {
//                message("You have uncommitted changes.");
//                System.exit(0);
//            }
//        }
//        String targetHash = readContentsAsString(F);
//        Commit target = readCommit(targetHash);
//        for (String name : target.getBlobs().keySet()) {
//            File f = join(CWD, name);
//            if (f.exists() && !getHeadCommit().getBlobs().containsKey(name)
//                    && !readObject(INDEX, StagingArea.class).getAddition().containsKey(name)) {
//                message("There is an untracked file in the way; delete it, or add and commit it first.");
//                System.exit(0);
//            }
//        }
//
//        Commit curcommit = getHeadCommit();
//        String branchhash = readContentsAsString(join(HEADS_DIR, branchname));
//        Commit branchcommit = readCommit(branchhash);
//        Set<String> currentAncestors = new HashSet<>();
//        Commit temp = curcommit;
//        while (temp != null) {
//            currentAncestors.add(temp.getHash());
//            String parentHash = temp.getParent();
//            temp = (parentHash == null) ? null : readCommit(parentHash);
//        }
//        temp = branchcommit;
//        String splitcommithash = null;
//        while (temp != null) {
//            if (currentAncestors.contains(temp.getHash())) {
//                splitcommithash = temp.getHash();
//                break;
//            }
//            String parentHash = temp.getParent();
//            temp = (parentHash == null) ? null : readCommit(parentHash);
//        }
//        if (splitcommithash == null) {
//            message("Internal error: no split point found.");
//            System.exit(0);
//        }
//
//        Commit splitcommit = readCommit(splitcommithash);
//
//        Set<String> allFiles = new HashSet<>();
//        allFiles.addAll(splitcommit.getBlobs().keySet());
//        allFiles.addAll(curcommit.getBlobs().keySet());
//        allFiles.addAll(branchcommit.getBlobs().keySet());
//
//        boolean hasConflict = false;
//        Map<String, String> mergedBlobs = new HashMap<>(curcommit.getBlobs());  // 关键！从当前 commit 继承所有文件
//
//        for (String fileName : allFiles) {
//            String splitBlob = splitcommit.getBlobs().get(fileName);
//            String headBlob   = curcommit.getBlobs().get(fileName);
//            String branchBlob = branchcommit.getBlobs().get(fileName);
//
//            if (Objects.equals(headBlob, branchBlob)) {
//                if (headBlob != null) {
//                    mergedBlobs.put(fileName, headBlob);
//                } else {
//                    mergedBlobs.remove(fileName);
//                    restrictedDelete(join(CWD, fileName));
//                }
//                continue;
//            }
//
//            if (Objects.equals(splitBlob, headBlob)) {
//                if (branchBlob != null) {
//                    mergedBlobs.put(fileName, branchBlob);
//                    writeContents(join(CWD, fileName), (Object) readBlob(branchBlob).getContent());
//                } else {
//                    mergedBlobs.remove(fileName);
//                    restrictedDelete(join(CWD, fileName));
//                }
//                continue;
//            }
//
//
//            if (Objects.equals(splitBlob, branchBlob)) {
//                if (headBlob != null) {
//                    mergedBlobs.put(fileName, headBlob);
//                } else {
//                    mergedBlobs.remove(fileName);
//                }
//                continue;
//            }
//
//            if (splitBlob != null && headBlob == null && branchBlob == null) {
//                mergedBlobs.remove(fileName);
//                restrictedDelete(join(CWD, fileName));
//                continue;
//            }
//
//            hasConflict = true;
//
//            String headContent = headBlob == null ? "" : new String(readBlob(headBlob).getContent(), StandardCharsets.UTF_8);
//            String branchContent = branchBlob == null ? "" : new String(readBlob(branchBlob).getContent(), StandardCharsets.UTF_8);
//
//            String conflictContent =
//                    "<<<<<<< HEAD\n" +
//                            headContent +
//                            "=======\n" +
//                            branchContent +
//                            ">>>>>>>\n";
//
//            File file = join(CWD, fileName);
//            writeContents(file, conflictContent);
//
//            Blob conflictBlob = new Blob(conflictContent.getBytes(StandardCharsets.UTF_8));
//            saveBlob(conflictBlob);
//            mergedBlobs.put(fileName, conflictBlob.getHash());
//        }
//
//        if (hasConflict) {
//            message("Encountered a merge conflict.");
//        }
//
//        String mergeMessage = "Merged " + branchname + " into " + currentbranch + ".";
//        Commit mergeCommit = new Commit(mergeMessage, curcommit.getHash(), branchcommit.getHash(), mergedBlobs);
//        saveCommit(mergeCommit);
//        writeObject(INDEX, new StagingArea());
//        updateBranchPointer(mergeCommit.getHash());}



//fuzhufangfa
private boolean isAncestor(Commit ancestor, Commit descendant) {
    if (descendant == null) return false;
    Set<String> visited = new HashSet<>();
    Queue<Commit> queue = new LinkedList<>();
    queue.add(descendant);

    while (!queue.isEmpty()) {
        Commit curr = queue.remove();
        if (curr.getHash().equals(ancestor.getHash())) {
            return true;
        }
        visited.add(curr.getHash());

        String parent = curr.getParent();
        if (parent != null && !visited.contains(parent)) {
            queue.add(readCommit(parent));
        }
        String secondParent = curr.getSecondParent();
        if (secondParent != null && !visited.contains(secondParent)) {
            queue.add(readCommit(secondParent));
        }
    }
    return false;
}
    private Commit getHeadCommit() {
        String ref = readContentsAsString(HEAD);
        String branch = ref.substring("ref: refs/heads/".length());
        String hash = readContentsAsString(join(HEADS_DIR, branch));
        return readCommit(hash);
    }

    private String getCurrentBranchName() {
        return readContentsAsString(HEAD).substring("ref: refs/heads/".length());
    }

    private void updateBranchPointer(String commitHash) {
        String branch = getCurrentBranchName();
        writeContents(join(HEADS_DIR, branch), commitHash);
    }

    private void checkoutFileFromCommit(String commitHash, String fileName) {
        Commit c = readCommit(commitHash);
        if (c == null) {
            message("No commit with that id exists.");
            System.exit(0);
        }
        String blobHash = c.getBlobs().get(fileName);
        if (blobHash == null) {
            message("File does not exist in that commit.");
            System.exit(0);
        }
        Blob b = readBlob(blobHash);
        writeContents(join(CWD, fileName), (Object) b.getContent());
    }
    private String findCommitByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        String result = null;
        for (String fullHash : plainFilenamesIn(OBJECTS_DIR)) {
            if (fullHash.startsWith(prefix)) {
                if (result != null) {
                    throw new GitletException("More than one commit with that prefix.");
                }
                result = fullHash;
            }
        }
        return result;
    }

    private Commit readCommit(String hash) {
        File file = join(OBJECTS_DIR, hash);
        if (!file.exists()) {
            return null;
        }
        try {
            return readObject(file, Commit.class);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    private Blob readBlob(String hash) {
        File file = join(OBJECTS_DIR, hash);
        return readObject(file, Blob.class);
    }

    private void saveBlob(Blob b) {
        writeObject(join(OBJECTS_DIR, b.getHash()), b);
    }

    private void saveCommit(Commit c) {
        writeObject(join(OBJECTS_DIR, c.getHash()), c);
    }


    private List<String> sortedStrings(Collection<String> c) {
        List<String> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }

}
