package gitlet;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Zixian Zang
 *  Collaborators : Zheng Zhang, Yichu Chen
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args == null | args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        if (!args[0].equals("init")) {
            File git = new File(".gitlet");
            if (!git.exists() | !git.isDirectory()) {
                System.out.println("Not in an initialized Gitlet directory.");
                System.exit(0);
            }
        }
        if (args[0].equals("start")) {
            Utils.start();
            return;
        }
        String indicator = args[0];
        if (indicator.equals("init")) {
            doinit(args);
        } else if (indicator.equals("add")) {
            doadd(args);
        } else if (indicator.equals("commit")) {
            docommit(args);
        } else if (indicator.equals("log")) {
            dolog(args);
        } else if (indicator.equals("checkout")) {
            docheckout(args);
        } else if (indicator.equals("rm")) {
            doremove(args);
        } else if (indicator.equals("global-log")) {
            dogloballog(args);
        } else if (indicator.equals("find")) {
            dofind(args);
        } else if (indicator.equals("status")) {
            dostatus(args);
        } else if (indicator.equals("branch")) {
            dobranch(args);
        } else if (indicator.equals("rm-branch")) {
            doRmbranch(args);
        } else if (indicator.equals("reset")) {
            doReset(args);
        } else if (indicator.equals("merge")) {
            doMerge(args);
        } else if (indicator.equals("add-remote")) {
            doAddRemote(args);
        } else if (indicator.equals("rm-remote")) {
            doRmRemote(args);
        } else if (indicator.equals("push")) {
            doPush(args);
        } else if (indicator.equals("fetch")) {
            doFetch(args);
        } else if (indicator.equals("pull")) {
            doPull(args);
        } else {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }


    /** Check ARGUMENT for init and execute. */
    static void doinit(String[] argument) {
        if (argument.length > 1 | !argument[0].equals("init")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        init();
    }


    /** Do init command. */
    static void init() {
        File repo = new File(Utils.repoLoc());
        File commits = new File(Utils.commitObjectDir());
        File blobs = new File(Utils.blobObjectDir());
        if (repo.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        commits.mkdirs(); blobs.mkdirs();
        CommitTree tree = CommitTree.init();
        tree.serializeTree();
    }

    /** Check ARGUMENT for add and execute. */
    static void doadd(String[] argument) {
        if (argument.length > 2 | !argument[0].equals("add")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        if (argument.length == 1) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        add(argument[1]);
    }

    /** Do add command. Adding file FILENAME. */
    static void add(String filename) {
        File working = new File(Utils.workingDirectory());
        String[] allfiles = working.list();
        if (!Utils.arrayContains(allfiles, filename)) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        CommitTree repo = getrepo();
        repo.add(filename);
        repo.serializeTree();
    }

    /** Check ARGUMENT for commit and execute. */
    static void docommit(String[] argument) {
        if (argument.length == 1) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (argument.length == 2 & argument[1].isEmpty()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (argument.length != 2 | !argument[0].equals("commit")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        commit(argument[1]);
    }

    /** Do commit command. Commit with message MSG. */
    static void commit(String msg) {
        CommitTree repo = getrepo();
        if (repo.emptyStage()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        repo.commit(msg);
        repo.clearStage();
        repo.serializeTree();
    }

    /** Check ARGUMENT for log and execute. */
    static void dolog(String[] argument) {
        if (argument.length > 1 | !argument[0].equals("log")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        log();
    }

    /** Do log command. */
    static void log() {
        CommitTree repo = getrepo();
        String curCommit = repo.curHeadCommit();
        Commit tracker = Utils.getCommit(curCommit);
        while (tracker.hasParent()) {
            System.out.println(tracker);
            System.out.println();
            tracker = Utils.getCommit(tracker.parent1());
        }
        System.out.println(tracker);
        System.out.println();
    }

    /** Check ARGUMENT for checkout and execute. */
    static void docheckout(String[] argument) {
        if (argument.length == 3) {
            if (!argument[0].equals("checkout") | !argument[1].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            String filename = argument[2];
            checkoutHeadFile(filename);
        } else if (argument.length == 4) {
            if (!argument[0].equals("checkout") | !argument[2].equals("--")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkoutCommitFile(argument[1], argument[3]);
        } else if (argument.length == 2) {
            if (!argument[0].equals("checkout")) {
                System.out.println("Incorrect operands.");
                System.exit(0);
            }
            checkoutBranch(argument[1]);
        } else {
            System.out.println("Incorrect operand.");
            System.exit(0);
        }
    }

    /** Do checkout command for checking out file FILENAME from head commit. */
    static void checkoutHeadFile(String filename) {
        CommitTree repo = getrepo();
        repo.checkoutHeadFile(filename);
    }

    /** Do checkout command for checking out file FILENAME from commit
     * COMMITID. */
    static void checkoutCommitFile(String commitID, String filename) {
        CommitTree repo = getrepo();
        repo.checkoutCommitFile(commitID, filename);
    }

    /** Do checkout command for checking out to branch BRANCHNAME. */
    static void checkoutBranch(String branchName) {
        CommitTree repo = getrepo();
        repo.checkoutBranch(branchName);
    }

    /** Check ARGUMENT for find and execute. */
    static void dofind(String[] argument) {
        if (argument.length == 1 | !argument[0].equals("find")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (argument.length > 2 | !argument[0].equals("find")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        find(argument[1]);
    }

    /** Do find command for finding commit message MSG. */
    static void find(String msg) {
        CommitTree repo = getrepo();
        repo.find(msg);
    }

    /** Check ARGUMENT for global-log and execute. */
    static void dogloballog(String[] argument) {
        if (argument.length > 1 | !argument[0].equals("global-log")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        globalLog();
    }

    /** Do global-log command. */
    static void globalLog() {
        CommitTree repo = getrepo();
        HashSet<String> allCommit = repo.allCommits();
        Iterator<String> iter = allCommit.iterator();
        while (iter.hasNext()) {
            Commit cur = Utils.getCommit(iter.next());
            if (iter.hasNext()) {
                System.out.println(cur);
                System.out.println();
            } else {
                System.out.println(cur);
                System.out.println();
            }
        }
    }

    /** Check ARGUMENT for remove and execute. */
    static void doremove(String[] argument) {
        if (argument.length > 2 | !argument[0].equals("rm")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        remove(argument[1]);
    }

    /** Do remove command for removing file FILENAME. */
    static void remove(String filename) {
        CommitTree repo = getrepo();
        Commit latest = Utils.getCommit(repo.curHeadCommit());
        HashMap<String, String> lastC = latest.filesInCommit();
        HashMap<String, String> staged = repo.addArea();
        if (!lastC.containsKey(filename) & !staged.containsKey(filename)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        staged.remove(filename);
        if (lastC.containsKey(filename)) {
            repo.removeArea().add(filename);
            Utils.restrictedDelete(filename);
        }
        repo.serializeTree();
    }

    /** Check ARGUMENT for status and execute. */
    static void dostatus(String[] argument) {
        if (argument.length > 1 | !argument[0].equals("status")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        status();
    }

    /** Do status command. */
    static void status() {
        CommitTree repo = getrepo();
        repo.status();
    }

    /** Check ARGUMENT for adding branch and execute. */
    static void dobranch(String[] argument) {
        if (argument.length != 2 | !argument[0].equals("branch")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        branch(argument[1]);
    }

    /** Do log command.
     * @param newName name of the new branch. */
    static void branch(String newName) {
        CommitTree repo = getrepo();
        repo.branch(newName);
        repo.serializeTree();
    }

    /** Check ARGUMENT for remove branch and execute. */
    static void doRmbranch(String[] argument) {
        if (argument.length != 2 | !argument[0].equals("rm-branch")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        rmBranch(argument[1]);
    }

    /** Do remove branch command, remove branch BRANCHNAME. */
    static void rmBranch(String branchName) {
        CommitTree repo = getrepo();
        repo.rmBranch(branchName);
        repo.serializeTree();
    }

    /** Check ARGUMENT for reset and execute. */
    static void doReset(String[] argument) {
        if (argument.length != 2 | !argument[0].equals("reset")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        reset(argument[1]);
    }

    /** Do reset command, reset to commit ID. */
    static void reset(String id) {
        CommitTree repo = getrepo();
        repo.reset(id);
        repo.serializeTree();
    }

    /** Check ARGUMENT for merge and execute. */
    static void doMerge(String[] argument) {
        if (argument.length != 2 | !argument[0].equals("merge")) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        merge(argument[1]);
    }

    /** Do merge command, merge GIVENBRANCH into current Branch. */
    static void merge(String givenBranch) {
        CommitTree repo = getrepo();
        String curBranch = repo.curBranch();
        HashMap<String, String> branches = repo.branches();
        if (curBranch.equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        if (!branches.containsKey(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (!repo.emptyStage()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!repo.untrackedFiles().isEmpty()) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
            System.exit(0);
        }
        repo.preMergeCheck(givenBranch);
        repo.clearStage();
        repo.serializeTree();
    }

    /** Check AGRS for add remote and execute.
     * @param args argument array*/
    static void doAddRemote(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        CommitTree repo = getrepo();
        repo.addRemote(args[1], args[2].replaceAll("/", File.separator));
        repo.serializeTree();
    }

    /** Check ARGS for rm remote and execute. */
    static void doRmRemote(String[] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        CommitTree repo = getrepo();
        repo.rmRemote(args[1]);
        repo.serializeTree();
    }

    /** Check ARGS for push and execute. */
    static void doPush(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        CommitTree repo = getrepo();
        repo.push(args[1], args[2]);
    }

    /** Check ARGS for fetch and execute. */
    static void doFetch(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        CommitTree repo = getrepo();
        repo.fetch(args[1], args[2]);
        repo.serializeTree();
    }

    /** Check ARGS for fetch and execute. */
    static void doPull(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
        CommitTree repo = getrepo();
        repo.pull(args[1], args[2]);
        repo.serializeTree();
    }

    /** Return the CommitTree object stored by previous initialization. */
    static CommitTree getrepo() {
        File repoDir = new File(Utils.repoLoc());
        CommitTree repo = Utils.readObject(repoDir, CommitTree.class);
        return repo;
    }

}
