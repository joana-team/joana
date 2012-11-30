/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.daisy;

/**
 * The DaisyTest provides a threaded example that will exercise
 * the Daisy file system.  It will initialize the file system and then create
 * 4 users of the file system.  This should provide Bandera an
 * example that could have deadlock because of how the Daisy file system is implemented.
 *
 * @author Todd Wallentine tcw AT cis ksu edu
 * @version $Revision: 1.1 $ - $Date: 2004/05/18 22:05:20 $
 */
public class DaisyTest {
    private static final int MAX_USER_COUNT = 4;

    /**
     * The main method will initialize the file system by creatng a files, initializing them with contents,
     * and then starting 4 user threads.  Each thread will randomly perform an opertion on the file
     * system (read, write, create, or delete).
     * The main method then waits for the threads to complete and then completes.
     *
     * @param args Ignored.
     */
    public static void main(String[] args) {
        FileHandle root = new FileHandle();
        FileHandle cowFileHandle = new FileHandle();
        FileHandle wombatFileHandle = new FileHandle();
        Petal.init(false);

        int fileCount = 2;
        FileHandle[] fileHandles = new FileHandle[fileCount];
        for(int i = 0; i < fileHandles.length; i++) {
            fileHandles[i] = new FileHandle();
        }
        byte[][] filenames = new byte[fileCount][];
        filenames[0] = stringToBytes("cow");
        filenames[1] = stringToBytes("wombat");
        for(int i = 0; i < filenames.length; i++) {
            DaisyDir.creat(root, filenames[i], fileHandles[i]);
            byte[] data = stringToBytes("someData");
            DaisyDir.write(fileHandles[i], 0, data.length, data);
        }

        System.out.println("Creating the DaisyUserThreads ...");
        DaisyUserThread[] daisyUserThreads = new DaisyUserThread[MAX_USER_COUNT];
        for(int i = 0; i < MAX_USER_COUNT; i++) {
            daisyUserThreads[i] = new DaisyUserThread(2, filenames, root);
        }

        System.out.println("Starting the DaisyUserThreads ...");
        for(int i = 0; i < MAX_USER_COUNT; i++) {
            daisyUserThreads[i].start();
        }

        for(int i = 0; i < MAX_USER_COUNT; i++) {
            try {
                daisyUserThreads[i].join();
            } catch(Exception e) {
                System.err.println("Error joining DaisyUserThread " + i + ".");
            }
        }

        System.out.println("Finished.");
    }

    static byte[] stringToBytes(String s) {
        byte b[] = new byte[s.length()];
        for (int i = 0; i < s.length(); i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }
}
