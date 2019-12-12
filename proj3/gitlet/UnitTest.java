package gitlet;

import ucb.junit.textui;
import org.junit.Test;

import java.io.File;
import java.util.HashSet;

import static org.junit.Assert.*;

/** The suite of all JUnit tests for the gitlet package.
 *  @author Zixian Zang
 */
public class UnitTest {

    /** Run the JUnit tests in the loa package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /** A dummy test to avoid complaint. */
    @Test
    public void commitTest() {
        File g = new File(".gitlet");
        File f = new File(".gitlet/commits/");
        f.mkdirs();
        Commit c = new Commit("a", "b", "c", new HashSet<>(), "0");
        assertEquals("b", c.parent1());
        assertEquals("c", c.parent2());
        assertTrue(c.hasParent());
        for (File f1 : f.listFiles()) {
            f1.delete();
        }
        f.delete();
        g.delete();
    }

    @Test
    public void initTest() {
        Main.init();
        File f = new File(".gitlet/commits/");
        File g = new File(".gitlet/blobs/");
        File h = new File(".gitlet/tree");
        File a = new File(".gitlet");
        assertTrue(f.exists());
        assertTrue(g.exists());
        assertTrue(h.exists());
        for (File t : f.listFiles()) {
            t.delete();
        }
        for (File t : g.listFiles()) {
            t.delete();
        }
        f.delete();
        g.delete();
        h.delete();
        a.delete();
    }


}


