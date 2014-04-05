/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * This file is part of the Daisy distribution.  This software is
 * distributed 'as is' without any guarantees whatsoever. It may be
 * used freely for research but may not be used in any commercial
 * products.  The contents of this distribution should not be posted
 * on the web or distributed without the consent of the authors.
 *
 * Authors: Cormac Flanagan, Stephen N. Freund, Shaz Qadeer
 * Contact: Shaz Qadeer (qadeer@microsoft.com)
 */

package conc.daisy;

//@ thread_local
class DirectoryEntry {
    public static final int MAXNAMESIZE = 256;
    // 8 bytes for the inodenum
    // 8 bytes for the length of filename (yes, there is some redundancy here)
    // MAXNAMESIZE bytes for the contents of filename
    public static final int ENTRYSIZE = 8 + 8 + MAXNAMESIZE;
    public long inodenum;
    public byte[] filename;
    // On the disk, the contents of the directory entry are stored
    // in the order: inodenum, filename length, filename contents
}

//@ thread_local
class Directory {
    static public final int DIRSIZE=256;
    public FileHandle file;
    public long size;
    public DirectoryEntry entries[] = new DirectoryEntry[DIRSIZE];
}

public class DaisyDir {

    // Private methods

    //@ helper
    static private boolean names_equal(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    static private long readLong(long inodenum, int offset) {
        byte b[] = new byte[8];
        int x = Daisy.read(inodenum, offset, 8, b);
        return Utility.bytesToLong(b, 0);
    }

    static private int writeLong(long inodenum, int offset, long n) {
        byte b[] = new byte[8];
        Utility.longToBytes(n, b, 0);
        return Daisy.write(inodenum, offset, 8, b);
    }

    /*@ performs
        action "act1" () {
	  \result != Daisy.DAISY_ERR_OK &&
	  \old(DaisyLock.fileLocks)[dir.inodenum] == null &&
	  DaisyLock.fileLocks[dir.inodenum] == null
	}
	[]
	action "act2" (d.file, d.size, d.entries, DaisyLock.fileLocks[dir.inodenum]) {
	  \result == Daisy.DAISY_ERR_OK &&
          dirs[dir.inodenum] &&
	  (\forall int i; 0 <= i && i < d.size && d.entries[i].inodenum != -1 ==>
	                  dirContents[dir.inodenum][d.entries[i].filename] == d.entries[i].inodenum) &&
	  (\forall long f; dirContents[dir.inodenum][f] == -1 ||
 	                   (\exists int i; 0 <= i && i < d.size && d.entries[i].filename == f &&
			                                           d.entries[i].inodenum ==
						                   dirContents[dir.inodenum][f])) &&
	  d.entries != null &&
	  (\forall int i; 0 <= i && i < d.entries.length ==> d.entries[i] != null) &&
          0 <= d.size && d.size < d.entries.length &&
	  \type(DirectoryEntry) == \elemtype(\typeof(d.entries)) &&
	  \old(DaisyLock.fileLocks)[dir.inodenum] == null && DaisyLock.fileLocks[dir.inodenum] == \tid &&
	  d.file == dir
      }
     */
    static int openDirectory(FileHandle dir, Directory d) {
        Attribute a = new Attribute();
        int res;
        // Currently, we do not check that dir is indeed a directory.  We assume
        // that a client of the file system is well-behaved and does not attempt
        // to create a file in another file.
        DaisyLock.lock_file(dir.inodenum);
        d.file = dir;
        res = Daisy.get_attr(dir.inodenum, a);
        if (res != Daisy.DAISY_ERR_OK) {
            DaisyLock.unlock_file(d.file.inodenum);
            return res;
        }
        d.size = a.size / DirectoryEntry.ENTRYSIZE;
        System.out.println("Size of directory = " + d.size);
        for (int i = 0; i < d.size; i++) {
            d.entries[i] = new DirectoryEntry();
            d.entries[i].inodenum =
                DaisyDir.readLong(dir.inodenum, i * DirectoryEntry.ENTRYSIZE);
            // We know that we only need a byte to store the size of filename
            int namesize =
                (int) DaisyDir.readLong(dir.inodenum, i * DirectoryEntry.ENTRYSIZE + 8);
            byte[] b = new byte[namesize];
            DaisyDir.read(dir, i * DirectoryEntry.ENTRYSIZE + 16, namesize, b);
            d.entries[i].filename = b;
        }
        return Daisy.DAISY_ERR_OK;
    }

    /*@ performs
        action "act1" (dirContents[d.file.inodenum][*], DaisyLock.fileLocks[d.file.inodenum]) {
	  \result == Daisy.DAISY_ERR_OK &&
	  (\forall int i; 0 <= i && i < d.size && d.entries[i].inodenum != -1 ==>
	                  dirContents[d.file.inodenum][d.entries[i].filename] == d.entries[i].inodenum) &&
	  DaisyLock.fileLocks[d.file.inodenum] == null
	}
     */
    
    private static long dirsize;
    
    static int closeDirectory(Directory d) {
    	dirsize = d.size;
        for (int i = 0; i < d.size; i++) {
            DaisyDir.writeLong(d.file.inodenum,
                    i * DirectoryEntry.ENTRYSIZE,
                    d.entries[i].inodenum);
            DaisyDir.writeLong(d.file.inodenum,
                    i * DirectoryEntry.ENTRYSIZE + 8,
                    (long) d.entries[i].filename.length);
            byte[] b = new byte[DirectoryEntry.MAXNAMESIZE];
            System.arraycopy(d.entries[i].filename, 0, b, 0, d.entries[i].filename.length);
            DaisyDir.write(d.file,
                    i * DirectoryEntry.ENTRYSIZE + 16,
                    DirectoryEntry.MAXNAMESIZE,
                    b);
        }
        DaisyLock.unlock_file(d.file.inodenum);
        return Daisy.DAISY_ERR_OK;
    }

    // Set of inodenums corresponding to directories encoded as a map from inodenums to boolean
    //@ ghost /*@ guarded_by[i] DaisyLock.fileLocks[i] == \tid */ public static long -> boolean dirs

    /* Set of files in a directory: inodenum -> filename -> inodenum.
       dirContents[inodenum][filename] provides the inode number of the file named
       filename in the directory with inode number inodenum.  This is meaningful
       only when dirs[inodenum] is true.  If dirContents[inodenum][filename] == -1
       then filename does not exist in the directory with inode number inodenum.
     */
    //@ ghost /*@ guarded_by[i] DaisyLock.fileLocks[i] == \tid */ public static long -> long -> long dirContents

    /*@ performs
        action "act1" () {
  	  \result != Daisy.DAISY_ERR_OK &&
	  \old(DaisyLock.fileLocks)[dir.inodenum] == null &&
	  DaisyLock.fileLocks[dir.inodenum] == null
	}
	[]
        action "act2" (dirContents[dir.inodenum][filename], DaisyDisk.inodeUsed[fh.inodenum], fh.inodenum) {
	  (       \old(dirContents)[dir.inodenum][filename] == -1 &&
	      dirContents[dir.inodenum][filename] == fh.inodenum &&
	      \old(DaisyLock.fileLocks)[dir.inodenum] == null && DaisyLock.fileLocks[dir.inodenum] == null &&
	     !\old(DaisyDisk.inodeUsed)[fh.inodenum] && DaisyDisk.inodeUsed[fh.inodenum] )
       };
       requires dir != fh
     */
    static public int creat(/*@ non_null */ FileHandle dir,
            /*@ non_null */ byte[] filename,
            /*@ non_null */ FileHandle fh) {
        Directory d = new Directory();

        int res = DaisyDir.openDirectory(dir, d);

        if (res != Daisy.DAISY_ERR_OK) {
            //@ set \witness = "act1"
            return res;
        }

        int new_entry = (int)d.size;
        for (int i = 0; i < d.size; i++) {
            if (d.entries[i].inodenum != -1) {
                if (names_equal(filename, d.entries[i].filename)) {
                    DaisyDir.closeDirectory(d);
                    //@ set \witness = "act1"
                    return Daisy.DAISY_ERR_EXIST;
                }
            } else {
                new_entry = i;
            }
        }

        if (new_entry == Directory.DIRSIZE) {
            DaisyDir.closeDirectory(d);
            //@ set \witness = "act1"
            return Daisy.DAISY_ERR_NOSPC;
        }

        long inodenum = Daisy.creat();
        if (inodenum < 0) {
            DaisyDir.closeDirectory(d);
            //@ set \witness = "act1"
            return (int)inodenum;
        }

        d.entries[new_entry] = new DirectoryEntry();
        d.entries[new_entry].inodenum = inodenum;
        d.entries[new_entry].filename = filename;

        fh.inodenum = inodenum;

        if (new_entry == d.size) {
            d.size++;
        }

        //@ set \witness = "act2"
        return DaisyDir.closeDirectory(d);
    }

    static public int read_dir(FileHandle dir, int cookie, byte b[]) {
        Directory d = new Directory();
        int res = DaisyDir.openDirectory(dir, d);

        if (res != Daisy.DAISY_ERR_OK) {
            return res;
        }

        for (int i = cookie; i < d.size; i++) {
            if (d.entries[i].inodenum != -1) {
                for (int j = 0; j < DirectoryEntry.MAXNAMESIZE; j++) {
                    b[j] = d.entries[i].filename[j];
                }

                res = DaisyDir.closeDirectory(d);

                if (res != Daisy.DAISY_ERR_OK) {
                    return res;
                } else {
                    return i + 1;
                }
            }
        }

        DaisyDir.closeDirectory(d);
        return Daisy.DAISY_ERR_NOENT;
    }

    static public int lookup(FileHandle dir, byte[] filename, FileHandle fh) {
        Directory d = new Directory();
        int res = DaisyDir.openDirectory(dir, d);

        if (res != Daisy.DAISY_ERR_OK) {
            return res;
        }

        for (int i = 0; i < d.size; i++) {
            if (d.entries[i].inodenum != -1 && names_equal(filename, d.entries[i].filename)) {
                fh.inodenum = d.entries[i].inodenum;
                return DaisyDir.closeDirectory(d);
            }
        }

        DaisyDir.closeDirectory(d);
        return Daisy.DAISY_ERR_NOENT;
    }

    static public int unlink(FileHandle dir, byte[] filename) {
        Directory d = new Directory();
        int res = DaisyDir.openDirectory(dir, d);

        if (res != Daisy.DAISY_ERR_OK) {
            return res;
        }

        for (int i = 0; i < d.size; i++) {
            if (d.entries[i].inodenum != -1 && names_equal(filename, d.entries[i].filename)) {
                long t = d.entries[i].inodenum;
                d.entries[i].inodenum = -1;
                Daisy.unlink(t);
                return DaisyDir.closeDirectory(d);
            }
        }

        DaisyDir.closeDirectory(d);
        return Daisy.DAISY_ERR_NOENT;
    }

    static public int write(FileHandle file, int offset, int size, byte b[]) {
        return Daisy.write(file.inodenum, offset, size, b);
    }

    static public int read(FileHandle file, int offset, int size, byte b[]) {
        return Daisy.read(file.inodenum, offset, size, b);
    }

    static public int get_attr(FileHandle file, Attribute a) {
        return Daisy.get_attr(file.inodenum, a);
    }

    static public int set_attr(FileHandle file, Attribute a) {
        return Daisy.set_attr(file.inodenum, a);
    }
}
