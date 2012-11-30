/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.daisy;

import java.util.Random;

/**
 * The DaisyUserThread provides a thread that random chooses an operation
 * (read, write, create, or delete) and performs it.  The number of operations the
 * thread will perform before quitting is configurable through the constructor.
 *
 * @author Todd Wallentine tcw AT cis ksu edu
 * @version $Revision: 1.2 $ - $Date: 2004/07/28 19:29:39 $
 */
public class DaisyUserThread extends Thread {
    private int iterations;

    public static final int MAX_OPERATIONS = 4;
    public static final int READ_OPERATION = 0;
    public static final int WRITE_OPERATION = 1;
    public static final int CREATE_OPERATION = 2;
    public static final int DELETE_OPERATION = 3;

    public static final int MAX_FILE_SIZE = 9;

    private Random random;

    private byte[][] filenames;

    private FileHandle root;

    public DaisyUserThread(int iterations, byte[][] filenames, FileHandle root) {
        this.iterations = iterations;
        this.filenames = filenames;
        this.root = root;
        random = new Random();
    }

    public void run() {
        FileHandle fh = new FileHandle();
        int status;
        int offset;
        byte[] contents;

        for(int i = 0; i < iterations; i++) {

            int operation = random.nextInt(MAX_OPERATIONS);
            int fileID = random.nextInt(filenames.length);

            switch(operation) {
            case READ_OPERATION:
                System.out.println("Reading...");

                DaisyDir.lookup(root, filenames[fileID], fh);

                // determine how much to read
                int size = random.nextInt(MAX_FILE_SIZE);

                // read it
                contents = new byte[size];
                offset = 0;
                status = DaisyDir.read(fh, offset, size, contents);

                // print it
                System.out.println(new String(contents));
                break;

            case WRITE_OPERATION:
                System.out.println("Writing...");

                DaisyDir.lookup(root, filenames[fileID], fh);

                // determine what to write
                contents = DaisyTest.stringToBytes("someRandomData");

                // write it
                offset = 0;
                status = DaisyDir.write(fh, offset, contents.length, contents);
                break;

            case CREATE_OPERATION:
                System.out.println("Creating...");

                // create it
                status = DaisyDir.creat(root, filenames[fileID], fh);
                break;

            case DELETE_OPERATION:
                System.out.println("Deleting...");

                // delete it
                status = DaisyDir.unlink(root, filenames[fileID]);
                break;

            default:
                // throw an exception?
                break;
            }
        }
    }
}
