package gitlet;

import java.io.File;
import java.io.Serializable;


/** Blob class for Gitlet.
 *  @author Zixian Zang
 */
class Blob implements Serializable {

    /** Name of the file. */
    private String _filename;

    /** Sha code of the file. */
    private String _shaID;

    /** File in byte array. */
    private byte[] _fileByteContent;

    /** File in String. */
    private String _stringContent;


    /** Constructing blob object for file FILENAME. */
    Blob(String filename) {
        File f = new File(Utils.workingDirectory() + filename);
        _filename = filename;
        _fileByteContent = Utils.readContents(f);
        _shaID = Utils.sha1(_filename, "BLOB", _fileByteContent);
        _stringContent = new String(_fileByteContent);
        this.serializeBlob();
    }

    /** Return the filename related to this blob. */
    String blobname() {
        return _filename;
    }

    /** Return the content of the file in a string. */
    String blobInString() {
        return _stringContent;
    }

    /** Return the content of this file in byte. */
    byte[] blobInByte() {
        return _fileByteContent;
    }

    /** Return the sha1 code of this blob. */
    String blobShaID() {
        return _shaID;
    }

    /** Write my file into working directory. */
    void writeIntoDirectory() {
        Utils.writeContents(new File(_filename), _stringContent);
    }

    /** Serialize this blob object. */
    void serializeBlob() {
        File location = new File(Utils.blobObjectDir() + blobShaID() + "/");
        Utils.writeObject(location, this);
    }
}
