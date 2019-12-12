package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/** Represent the condition of a commit.
 *  @author zixianzang
 */
public class Commit implements Serializable {

    /** The message assigned to this commit. */
    private String _commitMessage;

    /** The ID of this commit. */
    private String _shacode;

    /** Map from filename in this commit to its sha1code. */
    private HashMap<String, String> _myBlobs;

    /** Time of this commit. */
    private String _time;

    /** First parent of this commit. */
    private String _parent1;

    /** Second parent of this commit. */
    private String _parent2;

    /** If this a merge head. */
    private boolean _merge;


    /** Constructing commit object. With message MSG, PARENT1 and
     * PARENT2 as two potential parent commits, BLOBS as all ths
     * blobs tracked in this commit, TIME as commit time. */
    Commit(String msg, String parent1, String parent2,
           HashSet<Blob> blobs, String time) {
        _time = time;
        _parent1 = parent1;
        _parent2 = parent2;
        _commitMessage = msg;
        _myBlobs = new HashMap<>();
        if (blobs != null) {
            for (Blob b : blobs) {
                if (b.blobname().startsWith(".")) {
                    continue;
                }
                _myBlobs.put(b.blobname(), b.blobShaID());
            }
        }
        String presha1 = "commit" + _time + _commitMessage + parent1 + parent2;
        for (String s : _myBlobs.values()) {
            Blob b = Utils.getBlob(s);
            presha1 = presha1.concat(b.blobInString());
            presha1 = presha1.concat(b.blobname());
        }
        _shacode = Utils.sha1(presha1);
        if (parent1 != null & parent2 != null) {
            _merge = true;
        }
        this.serializeC();
    }

    /** Return the ID of this commit. */
    public String commitID() {
        return _shacode;
    }

    /** Return mapping from filename to ID in this commit. */
    HashMap<String, String> filesInCommit() {
        return _myBlobs;
    }

    /** Return ID of parent1 of this commit. */
    String parent1() {
        return _parent1;
    }

    /** Return ID of parent2 of this commit. */
    String parent2() {
        return _parent2;
    }

    /** Return true if this commit is not the initial commit. */
    boolean hasParent() {
        return _parent1 != null | _parent2 != null;
    }


    /** Serialize this commit object. */
    void serializeC() {
        File location = new File(Utils.commitObjectDir()
                + commitID() + "/");
        Utils.writeObject(location, this);
    }

    /** Write all blobs into working directory, assume the working directory has
     * already been cleaned. */
    void writeAllFiles() {
        for (String blobID : _myBlobs.values()) {
            Blob b = Utils.getBlob(blobID);
            Utils.writeContents(new File(b.blobname()), b.blobInByte());
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("===\n");
        builder.append("commit ");
        builder.append(_shacode).append("\n");
        if (_merge) {
            builder.append("Merge: ").append(parent1().substring(0, 7));
            builder.append(" ").append(parent2().substring(0, 7)).append("\n");
        }
        builder.append("Date: ");
        builder.append(_time).append("\n");
        builder.append(_commitMessage);
        return builder.toString();
    }
}
