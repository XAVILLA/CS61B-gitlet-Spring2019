package gitlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;


/** Assorted utilities.
 *  @author Zixian Zang
 */
class Utils {

    /** The length of a complete SHA-1 UID as a hexadecimal numeral. */
    static final int UID_LENGTH = 40;

    /** Format for commit times. */
    private static SimpleDateFormat _format =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /** Return the format of time used. */
    static SimpleDateFormat formatt() {
        return _format;
    }

    /** File separator. */
    private static String sep = File.separator;

    /** File separator.
     * @return String separator
     * */
    String sep() {
        return sep;
    }

    /** Get CommitTree object from a remote path REMOTEPATH which end
     * with ".gitlet".
     * @param remoteName name of remote
     * @return remote repo object
     * */
    static CommitTree getRemoteRepo(String remoteName) {
        CommitTree repo = Main.getrepo();
        String remotePath = repo.remote().get(remoteName);
        File path = new File(remotePath + sep + "tree");
        CommitTree remoteRepo = Utils.readObject(path, CommitTree.class);
        return remoteRepo;
    }

    /** Save the repo into remote directory.
     * @param remoteName name of remote
     * @param remoteRepo remote repo object*/
    static void saveRemoteTree(String remoteName, CommitTree remoteRepo) {
        CommitTree repo = Main.getrepo();
        String remotePath = repo.remote().get(remoteName);
        File path = new File(remotePath + sep + "tree");
        Utils.writeObject(path, remoteRepo);
    }

    /** Get Commit object from a remote path REMOTEPATH which end
     * with ".gitlet" and commit id COMMITID.
     * @param remoteName name of remote
     * @param commitID id of commit
     * @return commit object*/
    static Commit getRemoteCommit(String remoteName, String commitID) {
        CommitTree repo = Main.getrepo();
        String remotePath = repo.remote().get(remoteName);
        File path = new File(remotePath + sep + "commits" + sep + commitID);
        Commit remoteCommit = Utils.readObject(path, Commit.class);
        return remoteCommit;
    }

    /** Get Blob object from a remote path REMOTEPATH which end
     * with ".gitlet" and Blob id BLOBID.
     * @param remoteName name of remote
     * @param blobID id of blob
     * @return blob object*/
    static Blob getRemoteBlob(String remoteName, String blobID) {
        CommitTree repo = Main.getrepo();
        String remotePath = repo.remote().get(remoteName);
        File path = new File(remotePath + sep + "blobs" + sep + blobID);
        Blob remoteBlob = Utils.readObject(path, Blob.class);
        return remoteBlob;
    }

    /** Get a blob from remote repository and put it in current repository
     * storing area.
     * @param remoteName name of remote
     * @param blobID id of blob
     * */
    static void addRemoteBlobToLocal(String remoteName, String blobID) {
        Blob remote = getRemoteBlob(remoteName, blobID);
        remote.serializeBlob();
    }

    /** Get a blob from current repository and put it in remote repository
     * storing area.
     * @param remoteName name of remote
     * @param blobID id of blob
     * */
    static void addLocalBlobToRemote(String remoteName, String blobID) {
        CommitTree repo = Main.getrepo();
        String remotePath = repo.remote().get(remoteName);
        Blob local = getBlob(blobID);
        File path = new File(remotePath + sep + "blobs" + sep + blobID);
        Utils.writeObject(path, local);
    }

    /** Get a commit from remote repository and put it in current repository
     * storing area.
     * @param remoteName name of remote
     * @param commitID id of commit
     * */
    static void addRemoteCommitToLocal(String remoteName, String commitID) {
        Commit remote = getRemoteCommit(remoteName, commitID);
        remote.serializeC();
    }

    /** Get a commit from current repository and put it in remote repository
     * storing area.
     * @param remoteName path of remote
     * @param commitID id of commit
     * */
    static void addLocalCommitToRemote(String remoteName, String commitID) {
        CommitTree repo = Main.getrepo();
        String remotePath = repo.remote().get(remoteName);
        Commit local = getCommit(commitID);
        File path = new File(remotePath + sep + "commits" + sep + commitID);
        Utils.writeObject(path, local);
    }


    /** Path of the commit object directory in string. */
    private static String commitObjectDir = ".gitlet" + sep + "commits" + sep;

    /** Path of the repo object in string. */
    private static String repoLoc = ".gitlet" + sep + "tree" + sep;

    /** Path of the blob object directory in string. */
    private static String blobObjectDir = ".gitlet" + sep + "blobs" + sep;

    /** Path of the working directory in string. */
    private static String workingDir = System.getProperty("user.dir") + sep;

    /** Path of the commit object directory in string.
     * @return String
     * */
    static String commitObjectDir() {
        return commitObjectDir;
    }

    /** Path of the repo object in string.
     * @return String */
    static String repoLoc() {
        return repoLoc;
    }

    /** Path of the blob object directory in string.
     * @return String*/
    static String blobObjectDir() {
        return blobObjectDir;
    }

    /** Path of the working directory in string.
     * @return String*/
    static String workingDirectory() {
        return workingDir;
    }

    /** Get the commit object with ID COMMITID.
     * @return commit object
     * */
    static Commit getCommit(String commitID) {
        File loc = new File(commitObjectDir() + commitID + "/");
        return Utils.readObject(loc, Commit.class);
    }

    /** Get the blob object with ID BLOBID.
     * @return Blob Object*/
    static Blob getBlob(String blobID) {
        File loc = new File(blobObjectDir() + blobID + "/");
        return Utils.readObject(loc, Blob.class);
    }

    /** Return all filenames in the working directory.
     * @return ArrayList of String
     * */
    static ArrayList<String> wdFiles() {
        File loc = new File(workingDirectory());
        File[] allfiles = loc.listFiles();
        ArrayList<String> result = new ArrayList<>();
        if (allfiles == null) {
            return null;
        }
        for (File f : allfiles) {
            if (!f.isDirectory() & !f.getName().equals(".DS_Store")) {
                result.add(f.getName());
            }
        }
        return result;
    }

    /** Return true if current file FILENAME version has different ID
     * than PREVID. */
    static boolean filechanged(String prevID, String filename) {
        Blob curVersion = new Blob(filename);
        return prevID.equals(curVersion.blobShaID());
    }

    /** Clean up the working directory. */
    static void cleanWD() {
        File wd = new File(workingDirectory());
        File[] allFiles = wd.listFiles();
        for (File f : allFiles) {
            restrictedDelete(f);
        }
    }

    /** Return true if file FILENAME exists in working directory. */
    public static boolean existInWD(String filename) {
        File f = new File(filename);
        return f.exists();
    }

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }



    /** Return the entire contents of FILE as a String.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /** Write the result of concatenating the bytes in CONTENTS to FILE,
     *  creating or overwriting it as needed.  Each object in CONTENTS may be
     *  either a String or a byte array.  Throws IllegalArgumentException
     *  in case of problems. */
    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Throws IllegalArgumentException in case of problems. */
    static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                 | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write OBJ to FILE. */
    static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /* OTHER FILE UTILITIES */

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java .nio.file.Paths.#get(String, String[])}
     *  method. */
    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java .nio.file.Paths.#get(String, String[])}
     *  method. */
    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ. */
    static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw error("Internal error serializing commit.");
        }
    }



    /* MESSAGES AND ERROR REPORTING */

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline. */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    /** FUNCTIONS */

    /** Represents a function from T1 -> T2.  The apply method contains the
     *  code of the function.  The 'foreach' method applies the function to all
     *  items of an Iterable.  This is an interim class to allow use of Java 7
     *  with Java 8-like constructs.  */
    abstract static class Function<T1, T2> {
        /** Returns the value of this function on X. */
        abstract T2 apply(T1 x);
    }


    /** Check if array L contains string S.
     * @return boolean*/
    public static boolean arrayContains(String[] l, String s) {
        if (l == null) {
            return false;
        }
        for (int i = 0; i < l.length; i++) {
            if (l[i].equals(s)) {
                return true;
            }
        }
        return false;
    }

    /** Return array L as an ArrayList. */
    public static ArrayList<String> arrayToList(String[] L) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(Arrays.asList(L));
        return result;
    }

    /** Return all string sorted in ORIGIN. */
    public static ArrayList<String> sortString(Collection<String> origin) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(origin);
        result.sort(String::compareTo);
        return result;
    }


    /** THIS METHOD IS FOR TESTING ONLY.
     * Clean up the repository and construct a new one.*/
    static void start() {
        File f1 = new File(".gitlet/");
        File f2 = new File(".gitlet/tree");
        File f3 = new File(".gitlet/commits");
        File f4 = new File(".gitlet/blobs");
        f2.delete();
        for (File f : f3.listFiles()) {
            f.delete();
        }
        for (File f : f4.listFiles()) {
            f.delete();
        }
        for (File f : f1.listFiles()) {
            f.delete();
        }
        f1.delete();
        return;
    }

}
