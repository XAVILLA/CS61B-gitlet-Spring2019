package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Date;

/**
 * Tree structure to run gitlet.
 * @author Zixian Zang
 */
public class CommitTree implements Serializable {

    /** Execute add-remote command for remote name NAME and path PATH.*/
    void addRemote(String name, String path) {
        if (_remotes.containsKey(name)) {
            System.out.println("A remote with that name already exists.");
            System.exit(0);
        }
        _remotes.put(name, path);

    }

    /** Execute rm-remote command for remote name NAME.*/
    void rmRemote(String name) {
        if (!_remotes.containsKey(name)) {
            System.out.println("A remote with that name does not exist.");
            System.exit(0);
        }
        _remotes.remove(name);
    }

    /** Execute push command.
     * @param remoteName name of remote repo
     * @param remoteBranch name of remote branch we're adding to
     * */
    void push(String remoteName, String remoteBranch) {
        File remoteGit = new File(_remotes.get(remoteName));
        if (!remoteGit.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        CommitTree remoteRepo = Utils.getRemoteRepo(remoteName);
        HashSet<String> historyOfHead = allAncestors(curHeadCommit());
        String remoteHeadID = remoteRepo._branches.get(remoteBranch);
        Commit curHeadCommit = Utils.getCommit(curHeadCommit());
        if (!historyOfHead.contains(remoteHeadID)) {
            System.out.println("Please pull down remote "
                    + "changes before pushing.");
            System.exit(0);
        }
        Commit tracker = curHeadCommit;
        while (!remoteHeadID.equals(tracker.commitID())) {
            Utils.addLocalCommitToRemote(remoteName, tracker.commitID());
            remoteRepo._commits.add(tracker.commitID());
            for (String blobID : tracker.filesInCommit().values()) {
                Utils.addLocalBlobToRemote(remoteName, blobID);
            }
            tracker = Utils.getCommit(tracker.parent1());
        }
        remoteRepo._branches.put(remoteBranch, curHeadCommit());
        Utils.saveRemoteTree(remoteName, remoteRepo);
    }

    /** Execute fetch command.
     * @param remoteName name of remote repo
     * @param remoteBranch name of remote branch we're adding from
     * */
    void fetch(String remoteName, String remoteBranch) {
        File remoteGit = new File(_remotes.get(remoteName));
        if (!remoteGit.exists()) {
            System.out.println("Remote directory not found.");
            System.exit(0);
        }
        CommitTree remoteRepo = Utils.getRemoteRepo(remoteName);
        if (!remoteRepo._branches.containsKey(remoteBranch)) {
            System.out.println("That remote does not have that branch.");
            System.exit(0);
        }
        Commit remoteHead =
                Utils.getRemoteCommit(remoteName,
                        remoteRepo._branches.get(remoteBranch));
        Commit tracker = remoteHead;
        while (!_commits.contains(tracker.commitID())) {
            Utils.addRemoteCommitToLocal(remoteName, tracker.commitID());
            _commits.add(tracker.commitID());
            for (String blobID : tracker.filesInCommit().values()) {
                Utils.addRemoteBlobToLocal(remoteName, blobID);
            }
            tracker = Utils.getRemoteCommit(remoteName, tracker.parent1());
        }
        String newBranchName = remoteName + "/" + remoteBranch;
        _branches.put(newBranchName, remoteHead.commitID());
        serializeTree();
    }

    /** Execute pull command.
     * @param remoteName name of remote repo
     * @param remoteBranch name of remote branch we're adding from
     * */
    void pull(String remoteName, String remoteBranch) {
        String newBranchName = remoteName + "/" + remoteBranch;
        fetch(remoteName, remoteBranch);
        merge(newBranchName);
        serializeTree();
    }

    /** Execute add FILENAME file to this stage. */
    void add(String filename) {
        Commit last = Utils.getCommit(curHeadCommit());
        HashMap<String, String> lastCFiles = last.filesInCommit();
        Blob b = new Blob(filename);
        if (!lastCFiles.containsKey(filename)) {
            _addedArea.put(b.blobname(), b.blobShaID());
            _removeArea.remove(b.blobname());
            b.serializeBlob();
        } else {
            File thisfile = new File(Utils.workingDirectory() + filename);
            byte[] curContent = Utils.readContents(thisfile);
            Blob lastCommitBlob = Utils.getBlob(lastCFiles.get(filename));
            byte[] lastCommitVersion = lastCommitBlob.blobInByte();
            if (!Arrays.equals(curContent, lastCommitVersion)) {
                _addedArea.put(b.blobname(), b.blobShaID());
                _removeArea.remove(b.blobname());
                b.serializeBlob();
            } else {
                _removeArea.remove(b.blobname());
                _addedArea.remove(b.blobname());
            }
        }
        this.serializeTree();
    }


    /** Add a branch name NEWBRANCH. */
    void branch(String newBranch) {
        if (_branches.containsKey(newBranch)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        _branches.put(newBranch, curHeadCommit());
    }

    /** Remove the branch BRANCHNAME. */
    void rmBranch(String branchName) {
        if (!_branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(_curBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        _branches.remove(branchName);
    }

    /** Reset the head of current branch to COMMITID. */
    void reset(String commitID) {
        String fullID = findFullID(commitID);
        if (!untrackedFiles().isEmpty()) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
            System.exit(0);
        }
        Utils.cleanWD();
        Commit destination = Utils.getCommit(fullID);
        destination.writeAllFiles();
        _branches.put(curBranch(), commitID);
        clearStage();
        serializeTree();
    }

    /** Constructor of a new Commit Tree. */
    CommitTree() {
        _branches = new HashMap<>();
        _msg2ID = new HashMap<>();
        _commits = new HashSet<>();
        _addedArea = new HashMap<>();
        _removeArea = new HashSet<>();
        _short2Full = new HashMap<>();
        _remotes = new HashMap<>();
        String initialTime = "Wed Dec 31 16:00:00 1969 -0800";
        Commit firstCommit = new Commit("initial commit", null,
                null, null, initialTime);
        String firstID = firstCommit.commitID();
        _curBranch = "master";
        _commits.add(firstID);
        _short2Full.put(firstID.substring(0, 6), firstID);
        _branches.put("master", firstCommit.commitID());
        HashSet<String> msginital = new HashSet<>();
        msginital.add(firstID);
        _msg2ID.put("initial commit", msginital);
        firstCommit.serializeC();
    }

    /** Initialize a new Commit Tree as repository.
     * @return initialized tree*/
    static CommitTree init() {
        CommitTree repo = new CommitTree();
        return repo;
    }

    /** Checkout file FILENAME from current commit. */
    void checkoutHeadFile(String filename) {
        Commit curBHead = Utils.getCommit(curHeadCommit());
        HashMap<String, String> lastCFile = curBHead.filesInCommit();
        if (!lastCFile.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String lastVersionID = lastCFile.get(filename);
        Blob lastVerion = Utils.getBlob(lastVersionID);
        lastVerion.writeIntoDirectory();
    }


    /** Checkout FILENAME from commit with short code ID. */
    void checkoutCommitFile(String id, String filename) {
        Commit thatCommit = Utils.getCommit(findFullID(id));
        HashMap<String, String> files = thatCommit.filesInCommit();
        if (!files.containsKey(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String blobID = files.get(filename);
        Blob thatVersion = Utils.getBlob(blobID);
        thatVersion.writeIntoDirectory();
    }


    /** Checkout to Branch BRANCHNAME. */
    void checkoutBranch(String branchName) {
        if (!_branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (branchName.equals(_curBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }
        if (!untrackedFiles().isEmpty()) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
            System.exit(0);
        }
        Commit resultBHead = Utils.getCommit(_branches.get(branchName));
        Utils.cleanWD();
        resultBHead.writeAllFiles();
        _curBranch = branchName;
        clearStage();
        serializeTree();
    }


    /** Return the list of untracked files. */
    HashSet<String> untrackedFiles() {
        HashSet<String> result = new HashSet<>();
        List<String> wdFiles = Utils.plainFilenamesIn(Utils.workingDirectory());
        for (String f : wdFiles) {
            if (_removeArea.contains(f)) {
                result.add(f);
            }
        }
        Commit curCommit = Utils.getCommit(curHeadCommit());
        HashMap<String, String> curCFiles = curCommit.filesInCommit();
        for (String f : wdFiles) {
            if (!_addedArea.containsKey(f) && !curCFiles.containsKey(f)) {
                result.add(f);
            }
        }
        for (String f : wdFiles) {
            if (_addedArea.containsKey(f)) {
                Blob addedBlob = Utils.getBlob(_addedArea.get(f));
                byte[] addedVersion = addedBlob.blobInByte();
                byte[] curVersion = Utils.readContents(new File(f));
                if (!Arrays.equals(curVersion, addedVersion)) {
                    result.add(f);
                }
            } else if (curCFiles.containsKey(f)) {
                Blob lastCBlob = Utils.getBlob(curCFiles.get(f));
                byte[] lastCVersion = lastCBlob.blobInByte();
                byte[] curVersion = Utils.readContents(new File(f));
                if (!Arrays.equals(lastCVersion, curVersion)) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    /** Execute committing with message MSG.
     * */
    public void commit(String msg) {
        String newCommit = commit(msg, curHeadCommit(), null);
        _commits.add(newCommit);
        _short2Full.put(newCommit.substring(0, 6), newCommit);
        _msg2ID.computeIfAbsent(msg, k -> new HashSet<>());
        _msg2ID.get(msg).add(newCommit);
        _branches.put(_curBranch, newCommit);
    }



    /** Execute automatic commit after merging.
     * @param otherBranch name of other branch
     * @param conflict if the merge met a conflict
     * */
    void mergecommit(String otherBranch, boolean conflict) {
        String msg = "Merged " + otherBranch + " into " + _curBranch + ".";
        String parent2 = _branches.get(otherBranch);
        String newCommit = commit(msg, curHeadCommit(), parent2);
        _commits.add(newCommit);
        _short2Full.put(newCommit.substring(0, 6), newCommit);
        _msg2ID.computeIfAbsent(msg, k -> new HashSet<>());
        _msg2ID.get(msg).add(newCommit);
        _branches.put(_curBranch, newCommit);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Execute commit on this branch, and return the ID of the new head commit.
     * @param msg commit message
     * @param parent1 ID of parent1
     * @param parent2 ID of parent2
     * */
    public String commit(String msg, String parent1, String parent2) {
        Commit par = Utils.getCommit(curHeadCommit());
        HashMap<String, String> parentBlob = par.filesInCommit();
        HashMap<String, String> addArea = addArea();
        HashSet<String> removeArea = removeArea();
        HashSet<Blob> myBlobs = new HashSet<>();
        for (String s : parentBlob.keySet()) {
            if (removeArea.contains(s)) {
                continue;
            }
            if (addArea.containsKey(s)) {
                myBlobs.add(Utils.getBlob(addArea.get(s)));
                continue;
            }
            myBlobs.add(Utils.getBlob(parentBlob.get(s)));
        }
        for (String s : addArea.keySet()) {
            if (parentBlob.keySet().contains(s)) {
                continue;
            }
            myBlobs.add(Utils.getBlob(addArea.get(s)));
        }
        String time = Utils.formatt().format(new Date());
        Commit thisC = new Commit(msg, parent1, parent2, myBlobs, time);
        return thisC.commitID();
    }

    /** Checking edge cases for merge.
     * @param givenBranch the name of given branch
     * */
    void preMergeCheck(String givenBranch) {
        Commit curHead = Utils.getCommit(curHeadCommit());
        Commit givenHead = Utils.getCommit(_branches.get(givenBranch));
        Commit splitPoint = Utils.getCommit(findSplitPoint(givenBranch));
        if (splitPoint.commitID().equals(givenHead.commitID())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            _branches.put(givenBranch, curHeadCommit());
            serializeTree();
            return;
        } else if (splitPoint.commitID().equals(curHead.commitID())) {
            System.out.println("Current branch fast-forwarded.");
            _branches.put(_curBranch, givenHead.commitID());
            Utils.cleanWD();
            givenHead.writeAllFiles();
            serializeTree();
            return;
        }
        merge(givenBranch);
    }


    /** Execute merge command.
     * @param givenBranch the name of given branch
     * */
    void merge(String givenBranch) {
        Commit curHead = Utils.getCommit(curHeadCommit());
        Commit givenHead = Utils.getCommit(_branches.get(givenBranch));
        Commit splitPoint = Utils.getCommit(findSplitPoint(givenBranch));
        boolean conflict = false;
        HashMap<String, String> splitF = splitPoint.filesInCommit();
        HashMap<String, String> givenF = givenHead.filesInCommit();
        HashMap<String, String> curF = curHead.filesInCommit();
        for (String f : splitF.keySet()) {
            if (givenF.containsKey(f) & curF.containsKey(f)) {
                if (!givenF.get(f).equals(splitF.get(f))
                        & curF.get(f).equals(splitF.get(f))) {
                    Blob givenVerion = Utils.getBlob(givenF.get(f));
                    givenVerion.writeIntoDirectory();
                    _addedArea.put(f, givenVerion.blobShaID());
                } else if (givenF.get(f).equals(splitF.get(f))
                        & !curF.get(f).equals(splitF.get(f))) {
                    continue;
                } else if (!givenF.get(f).equals(splitF.get(f))
                        & !curF.get(f).equals(splitF.get(f))) {
                    if (curF.get(f).equals(givenF.get(f))) {
                        continue;
                    } else {
                        writeConflict(f, curF.get(f), givenF.get(f));
                        conflict = true;
                    }
                }
            } else if (!givenF.containsKey(f) & curF.containsKey(f)) {
                if (splitF.get(f).equals(curF.get(f))) {
                    Utils.restrictedDelete(f);
                    _removeArea.add(f);
                } else {
                    writeConflict(f, curF.get(f), givenF.get(f));
                    conflict = true;
                }
            } else if (givenF.containsKey(f) & !curF.containsKey(f)) {
                if (splitF.get(f).equals(givenF.get(f))) {
                    continue;
                } else {
                    writeConflict(f, curF.get(f), givenF.get(f));
                    conflict = true;
                }
            }
        }
        for (String f : givenF.keySet()) {
            if (!splitF.containsKey(f)) {
                if (curF.containsKey(f)) {
                    if (!curF.get(f).equals(givenF.get(f))) {
                        writeConflict(f, curF.get(f), givenF.get(f));
                        conflict = true;
                    }
                } else if (!curF.containsKey(f)) {
                    Blob givenBlob = Utils.getBlob(givenF.get(f));
                    givenBlob.writeIntoDirectory();
                    _addedArea.put(f, givenBlob.blobShaID());
                }
            }
        }
        mergecommit(givenBranch, conflict);
    }


    /** Write the conflict content into the file while a merge conflict
     * occurs.
     * @param fileName name of the conflict file
     * @param curBlobID current blob version
     * @param givenBlobID given branch blob version
     * */
    void writeConflict(String fileName, String curBlobID, String givenBlobID) {
        StringBuilder content = new StringBuilder();
        File path = new File(fileName);
        content.append("<<<<<<< HEAD\n");
        if (curBlobID != null & givenBlobID != null) {
            Blob curBlob = Utils.getBlob(curBlobID);
            Blob givenBlob = Utils.getBlob(givenBlobID);
            content.append(curBlob.blobInString());
            content.append("=======\n");
            content.append(givenBlob.blobInString());
        } else if (curBlobID != null) {
            Blob curBlob = Utils.getBlob(curBlobID);
            content.append(curBlob.blobInString());
            content.append("=======\n");
        } else if (givenBlobID != null) {
            Blob givenBlob = Utils.getBlob(givenBlobID);
            content.append("=======\n");
            content.append(givenBlob.blobInString());
        }

        content.append(">>>>>>>\n");
        Utils.writeContents(path, content.toString());
        Blob newContent = new Blob(fileName);
        _addedArea.put(fileName, newContent.blobShaID());
    }


    /** Find the right split point of current head and head of the GIVENBRANCH.
     * Use ALLANCESTOR to first construct the collection of all ancestors of
     * the branch which we don't care about its distance to split point,
     * then use breadth first search for the branch that we care, until
     * we get a node that is contained in the first collection.
     * @return ID of the split point. */
    String findSplitPoint(String givenBranch) {
        Commit curHead = Utils.getCommit(curHeadCommit());
        Commit givenHead = Utils.getCommit(_branches.get(givenBranch));
        HashSet<String> givenAncestor = allAncestors(givenHead.commitID());
        HashSet<String> traveled = new HashSet<>();
        ArrayList<Commit> storing = new ArrayList<>();
        storing.add(curHead);
        while (!storing.isEmpty()) {
            Commit cur = storing.get(0);
            storing.remove(0);
            if (traveled.contains(cur.commitID())) {
                continue;
            }
            if (givenAncestor.contains(cur.commitID())) {
                return cur.commitID();
            }
            for (Commit par : parents(cur)) {
                storing.add(par);
            }
            traveled.add(cur.commitID());
        }
        throw new GitletException("No common ancestor");
    }

    /** Find all ancestors of the given commit COMMITID.
     * @return Hashset of all ancestor. */
    HashSet<String> allAncestors(String commitID) {
        HashSet<String> result = new HashSet<>();
        Commit self = Utils.getCommit(commitID);
        Stack<Commit> storing = new Stack<>();
        storing.push(self);
        while (!storing.empty()) {
            Commit cur = storing.pop();
            result.add(cur.commitID());
            for (Commit par : parents(cur)) {
                if (!result.contains(par.commitID())) {
                    storing.push(par);
                }
            }
        }
        return result;
    }

    /** Return HashSet of all parents of SELF. */
    HashSet<Commit> parents(Commit self) {
        HashSet<Commit> result = new HashSet<>();
        if (self.parent1() != null) {
            result.add(Utils.getCommit(self.parent1()));
        }
        if (self.parent2() != null) {
            result.add(Utils.getCommit(self.parent2()));
        }
        return result;
    }



    /** Print out status of repo. */
    void status() {
        StringBuilder build = new StringBuilder();
        HashMap<String, String> addArea = addArea();
        HashSet<String> removeArea = removeArea();
        build.append("=== Branches ===\n");
        for (String branch : Utils.sortString(_branches.keySet())) {
            if (branch.equals(curBranch())) {
                build.append("*").append(branch).append("\n");
            } else {
                build.append(branch).append("\n");
            }
        }
        build.append("\n");
        build.append("=== Staged Files ===\n");
        for (String file : Utils.sortString(addArea.keySet())) {
            build.append(file).append("\n");
        }
        build.append("\n");
        build.append("=== Removed Files ===\n");
        for (String file : Utils.sortString(removeArea)) {
            build.append(file).append("\n");
        }
        build.append("\n");
        build.append("=== Modifications Not Staged For Commit ===\n");
        for (String f : modifiedStatus()) {
            build.append(f).append("\n");
        }
        build.append("\n");
        build.append("=== Untracked Files ===\n");
        for (String f : untrackStatus()) {
            build.append(f).append("\n");
        }
        System.out.println(build.toString());
    }


    /** Return the list of all filenames that satisfy the section
     *  modified in status. */
    HashSet<String> modifiedStatus() {
        HashSet<String> result = new HashSet<>();
        HashMap<String, String> latest
                = Utils.getCommit(curHeadCommit()).filesInCommit();
        HashMap<String, String> addArea = addArea();
        HashSet<String> removeArea = removeArea();
        for (String f : Utils.wdFiles()) {
            File path = new File(f);
            if (addArea.containsKey(f)) {
                Blob added = Utils.getBlob(addArea.get(f));
                byte[] addV = added.blobInByte();
                byte[] curV = Utils.readContents(path);
                if (!Arrays.equals(addV, curV)) {
                    result.add(f + " (modified)");
                }
            } else if (latest.containsKey(f)) {
                Blob curB = Utils.getBlob(latest.get(f));
                byte[] curV = curB.blobInByte();
                byte[] newV = Utils.readContents(path);
                if (!Arrays.equals(curV, newV)) {
                    result.add(f + " (modified)");
                }
            }
        }
        ArrayList<String> allF = Utils.wdFiles();
        for (String f : latest.keySet()) {
            if (!allF.contains(f) & !_removeArea.contains(f)) {
                result.add(f + " (deleted)");
            }
        }
        return result;
    }

    /** Return the list of all filenames that satisfy the section
     *  modified in status. */
    HashSet<String> untrackStatus() {
        HashSet<String> result = new HashSet<>();
        HashMap<String, String> latest
                = Utils.getCommit(curHeadCommit()).filesInCommit();
        HashMap<String, String> addArea = addArea();
        for (String f : Utils.wdFiles()) {
            if (!latest.containsKey(f) & !addArea.containsKey(f)) {
                result.add(f);
            }
        }
        return result;
    }

    /** Print out commit IDs with given message MSG. */
    void find(String msg) {
        if (!_msg2ID.containsKey(msg)) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
        HashSet<String> allID = _msg2ID.get(msg);
        for (String id : allID) {
            System.out.println(id);
        }
    }

    /** Find the full commit ID correspond to a SHORTID.
     * @return String of full ID. */
    String findFullID(String shortID) {
        /** Length of full ID. */
        if (shortID.length() < 6) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        if (shortID.length() == Utils.UID_LENGTH) {
            if (!_commits.contains(shortID)) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            return shortID;
        }
        String firstSix = shortID.substring(0, 6);
        if (!_short2Full.containsKey(firstSix)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return _short2Full.get(firstSix);
    }

    /** Serialize this Tree object. */
    void serializeTree() {
        File location = new File(Utils.repoLoc());
        Utils.writeObject(location, this);
    }


    /** Return my adding area. */
    HashMap<String, String> addArea() {
        return _addedArea;
    }

    /** Return all my commits. */
    HashSet<String> allCommits() {
        return _commits;
    }

    /** Return my remove area. */
    HashSet<String> removeArea() {
        return _removeArea;
    }

    /** Map names of branches to head commit ID of that Branch. */
    private HashMap<String, String> _branches;

    /** Map all commit message to commits' ID with that message. */
    private HashMap<String, HashSet<String>> _msg2ID;

    /** Names of the files scheduled addition. */
    private HashMap<String, String> _addedArea;

    /** Names of the files scheduled removal. */
    private HashSet<String> _removeArea;

    /** All IDs. */
    private HashSet<String> _commits;

    /** Map from short shacode to full code. */
    private HashMap<String, String> _short2Full;

    /** The name of branch that the commit head currently points to. */
    private String _curBranch;

    /** Clean up the stage after commit. */
    void clearStage() {
        _addedArea.clear();
        _removeArea.clear();
    }

    /** Set of all my remote repositories.
     * @return my remote hashmap*/
    HashMap<String, String> remote() {
        return _remotes;
    }

    /** Return the ID of current commit. */
    String curHeadCommit() {
        return _branches.get(_curBranch);
    }

    /** Return true if no changes have been made in this stage. */
    boolean emptyStage() {
        return _addedArea.isEmpty() & _removeArea.isEmpty();
    }

    /** Return name of the current branch. */
    String curBranch() {
        return _curBranch;
    }

    /** Return the Branch head mapping. */
    HashMap<String, String> branches() {
        return _branches;
    }

    /** Set of all my remote repositories. */
    private HashMap<String, String> _remotes;

}
