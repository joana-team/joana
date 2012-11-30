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

/** Some design decisions:
	C-style error handling (return -1, null, etc, no exceptions).
	Mostly ignore error cases, for now.
	C and UNIX style identifiers, function names.
	No buffering/caching
	Read/write one byte at a time
	No hard links, keep file names in inodes.
	File names are 8 chars, packed into a long.
	1 directory.
  	1 block per file (ie virtual disk)
	No owner/permissions/access times
	Distributed filesystem

    Open issues:
	File and record locking primitives to clients
	Can have multiple files open?
	Minimum use of dynamic memory allocation (have fixed set of
	in-core Inodes, etc.)

    Standard routines not needed/implemented
        bmap
	dup, pipe, chdir, chroot, chmod, stat, link, mknod, mount, umount

*/

package conc.daisy;

import java.util.Vector;

class LockManager {
    // Java only has signed longs
    public static final long SIZE = 0x7fffffff /*2^63*/;

    private static Vector<Mutex> plocks = new Vector<Mutex>(0);
    private static Mutex m = new Mutex(-1);

    //@ ghost public static long -> Thread locks

    //@ requires 0 <= lockno && lockno < SIZE
    /*@ performs action "act" (locks[lockno]) {
          \old(locks)[lockno] == null && locks[lockno] == \tid
        }
     */
    public static void acq(long lockno) {
        if (lockno >= plocks.size() || plocks.get((int)lockno) == null) {
            m.acq();
            if (lockno >= plocks.size()) {
                plocks.setSize((int)lockno + 100);
            }
            if (plocks.get((int)lockno) == null) {
                plocks.setElementAt(new Mutex((int)lockno), (int)lockno);
            }
            m.rel();
        }
        plocks.get((int)lockno).acq();
    }

    //@ requires 0 <= lockno && lockno < SIZE
    //@ requires locks[lockno] == \tid
    /*@ performs action "act" (locks[lockno]) {
          locks[lockno] == null
        }
     */
    public static void rel(long lockno) {
        plocks.get((int)lockno).rel();
    }
}

/*@ thread_local */
class Inode {
    // No owner/permissions/access times
    // Almost always kept flushed
    // invariant 0 <= blockno && blockno < Daisy.MAXBLOCK
    long blockno;

    // invariant 0 <= size && size <= Daisy.BLOCKSIZE
    long size;
    boolean used;

    // following fields just in in-core copy
    // in-core copy only exists if inode is locked
    // invariant 0 <= inodenum && inodenum < Daisy.MAXINODE
    long inodenum;

    public String toString() {
        return
        "[inodenum=0x" + Long.toHexString(inodenum)+
        ", blockno=" + blockno +
        ", size=" + size +
        ", used=" + used + "]";
    }
}

class DaisyLock {
    // Split LockManager into sections, also:
    // locks 0 .. MAXINODE-1 are for inodes
    // locks MAXINODE .. MAXINODE+MAXBLOCK-1 are for blocks

    static final long STARTINODELOCKAREA = 0;
    static final long   ENDINODELOCKAREA = STARTINODELOCKAREA + Daisy.MAXINODE;
    static final long STARTBLOCKLOCKAREA = ENDINODELOCKAREA;
    static final long   ENDBLOCKLOCKAREA = STARTBLOCKLOCKAREA + Daisy.MAXBLOCK;
    static final long STARTFILELOCKAREA = ENDBLOCKLOCKAREA;
    static final long   ENDFILELOCKAREA = STARTFILELOCKAREA + Daisy.MAXINODE;

    //-----------------------------------------
    // ops on locks corresponding to inodes

    //@ ghost /*@ guarded_by[i] inodeLocks[i] == \tid */ public static long -> Thread inodeLocks

    //@ requires 0 <= inodenum && inodenum < Daisy.MAXINODE
    /*@ performs action "act" (inodeLocks[inodenum]) {
          \old(inodeLocks)[inodenum] == null && inodeLocks[inodenum] == \tid
	}
     */
    static void acqi(long inodenum) {
        LockManager.acq(STARTINODELOCKAREA+inodenum);
    } // nowarn Post

    //@ requires 0 <= inodenum && inodenum < Daisy.MAXINODE;
    //@ requires inodeLocks[inodenum] == \tid
    /*@ performs action "act" (inodeLocks[inodenum]) {
          inodeLocks[inodenum] == null
	}
     */
    static void reli(long inodenum) {
        LockManager.rel(STARTINODELOCKAREA+inodenum);
    } // nowarn Post

    //-----------------------------------------
    // ops on locks corresponding to blocks (covers both alloc bit & block)

    //@ ghost /*@ guarded_by[i] blockLocks[i] == \tid */ public static long -> Thread blockLocks

    //@ requires 0 <= blockno && blockno < Daisy.MAXBLOCK
    /*@ performs action "act" (blockLocks[blockno]) {
          \old(blockLocks)[blockno] == null && blockLocks[blockno] == \tid
	}
     */
    static void acqb(long blockno) {
        LockManager.acq(STARTBLOCKLOCKAREA+blockno);
    } // nowarn Post

    //@ requires 0 <= blockno && blockno < Daisy.MAXBLOCK
    //@ requires blockLocks[blockno] == \tid
    /*@ performs action "act" (blockLocks[blockno]) {
          blockLocks[blockno] == null
	}
     */
    static void relb(long blockno) {
        LockManager.rel(STARTBLOCKLOCKAREA+blockno);
    } // nowarn Post


    //-----------------------------------------
    // ops on locks corresponding to blocks (covers both alloc bit & block)

    //@ ghost /*@ guarded_by[i] fileLocks[i] == \tid */ public static long -> Thread fileLocks

    //@ requires 0 <= inodenum && inodenum < Daisy.MAXINODE
    /*@ performs action "act" (fileLocks[inodenum]) {
          \old(fileLocks)[inodenum] == null && fileLocks[inodenum] == \tid
	}
     */
    public static void lock_file(long inodenum) {
        LockManager.acq(STARTFILELOCKAREA+inodenum);
    }

    //@ requires 0 <= inodenum && inodenum < Daisy.MAXINODE;
    //@ requires fileLocks[inodenum] == \tid
    /*@ performs action "act" (fileLocks[inodenum]) {
          fileLocks[inodenum] == null
	}
     */
    public static void unlock_file(long inodenum) {
        LockManager.rel(STARTFILELOCKAREA+inodenum);
    }
}

class DaisyDisk {
    //========================================================
    // Splits Petal into sections, much like Frangipani

    static final long STARTINODEAREA = 0;
    static final long   ENDINODEAREA = STARTINODEAREA + Daisy.MAXINODE * Daisy.INODESIZE;
    static final long STARTBLOCKAREA = ENDINODEAREA;

    static final long   ENDBLOCKAREA = STARTBLOCKAREA + Daisy.MAXBLOCK * Daisy.BLOCKSIZE;

    static final long STARTALLOCAREA = ENDBLOCKAREA;
    static final long   ENDALLOCAREA = STARTALLOCAREA + Daisy.MAXBLOCK/8;

    //@ ghost /*@ guarded_by[i] DaisyLock.inodeLocks[i] == \tid */ public static long -> long inodeSizes
    //@ ghost /*@ guarded_by[i] DaisyLock.inodeLocks[i] == \tid */ public static long -> long inodeBlocknos
    //@ ghost /*@ guarded_by[i] DaisyLock.inodeLocks[i] == \tid */ public static long -> boolean inodeUsed

    /* global_invariant (\forall int i; inodeLocks[i] == null ==> 0 <= inodeBlocknos[i] && inodeBlocknos[i] < Daisy.MAXBLOCK) */

    //@ requires 0 <= inodenum && inodenum < Daisy.MAXINODE;
    //@ requires i != null
    //@ requires DaisyLock.inodeLocks[inodenum] == \tid
    //@ modifies i.blockno, i.size, i.used, i.inodenum
    //@ ensures i.blockno  == inodeBlocknos[inodenum]
    //@ ensures i.size     == inodeSizes[inodenum]
    //@ ensures i.used     == inodeUsed[inodenum]
    //@ ensures i.inodenum == inodenum
    //@ ensures 0 <= i.blockno && i.blockno < Daisy.MAXBLOCK
    static void readi(long inodenum, Inode i) {
        i.blockno = Petal.readLong(STARTINODEAREA +
                (inodenum * Daisy.INODESIZE));
        i.size= Petal.readLong(STARTINODEAREA +
                (inodenum * Daisy.INODESIZE) + 8);
        i.used = Petal.read(STARTINODEAREA +
                (inodenum * Daisy.INODESIZE) + 16) == 1;
        i.inodenum = inodenum;
        // read the right bytes, put in inode
    } //@ nowarn Post

    //@ requires i != null
    //@ requires DaisyLock.inodeLocks[i.inodenum] == \tid
    //@ modifies inodeBlocknos[i.inodenum], inodeUsed[i.inodenum], inodeSizes[i.inodenum]
    //@ ensures i.blockno  == inodeBlocknos[i.inodenum]
    //@ ensures i.size     == inodeSizes[i.inodenum]
    //@ ensures i.used     == inodeUsed[i.inodenum]
    static void writei(Inode i) {
        Petal.writeLong(STARTINODEAREA +
                (i.inodenum * Daisy.INODESIZE), i.blockno);
        Petal.writeLong(STARTINODEAREA +
                (i.inodenum * Daisy.INODESIZE) + 8, i.size);
        Petal.write(STARTINODEAREA +
                (i.inodenum * Daisy.INODESIZE) + 16,
                (byte)(i.used ? 1 : 0));
    } //@ nowarn Post

    // bits indicating block allocation status
    //@ ghost /*@ guarded_by[i] DaisyLock.blockLocks[i] == \tid */ public static long -> boolean allocBit

    //@ requires 0 <= blockno && blockno < Daisy.MAXBLOCK
    //@ requires DaisyLock.blockLocks[blockno] == \tid
    //@ ensures \result == allocBit[blockno]
    static boolean readAllocBit(long blockno) {
        return (Petal.read( STARTALLOCAREA+blockno/8 ) &  (1<<(blockno & 7))) != 0;
    } //@ nowarn Post

    //@ requires 0 <= blockno && blockno < Daisy.MAXBLOCK
    //@ requires DaisyLock.blockLocks[blockno] == \tid
    //@ modifies allocBit[blockno]
    //@ ensures allocBit[blockno] == bit
    static void writeAllocBit(long blockno, boolean bit) {
        byte b = Petal.read( STARTALLOCAREA+blockno/8 );
        byte mask = (byte)(1<<(blockno & 7));
        b = bit ? (byte)(b | mask) : (byte)(b & ~mask);
        Petal.write( STARTALLOCAREA+blockno/8, b );
    } //@ nowarn Post

    //@ ghost /*@ guarded_by[i][j] DaisyLock.blockLocks[i] == \tid */ public static long -> long -> byte blocks

    //@ requires 0 <= blockno && blockno < Daisy.MAXBLOCK
    //@ requires 0 <= offset && offset < Daisy.BLOCKSIZE
    //@ requires DaisyLock.blockLocks[blockno] == \tid
    //@ ensures \result == blocks[blockno][offset]
    static byte readBlockByte(long blockno, long offset) {
        return Petal.read( STARTBLOCKAREA + blockno * Daisy.BLOCKSIZE + offset );
    } //@ nowarn Post

    //@ requires 0 <= blockno && blockno < Daisy.MAXBLOCK
    //@ requires 0 <= offset && offset < Daisy.BLOCKSIZE
    //@ requires DaisyLock.blockLocks[blockno] == \tid
    //@ modifies blocks[blockno][offset]
    //@ ensures b == blocks[blockno][offset]
    static void writeBlockByte(long blockno, long offset, byte b) {
        Petal.write( STARTBLOCKAREA + blockno * Daisy.BLOCKSIZE + offset, b );
    } //@ nowarn Post
}

public class Daisy {

    public static final long MAXINODE  = 64;
    public static final long MAXBLOCK = 64;

    public static final int  INODESIZE = 24;    // 2 longs + boolean
    public static final long BLOCKSIZE = 1024 * 10;

    /*@
      axiom MAXINODE > 0;
      axiom MAXBLOCK > 0;
     */

    // error codes for Daisy functions.  These are
    // the negated values of the standard NFS error codes.
    static final int DAISY_ERR_OK = -0;
    static final int DAISY_ERR_NOENT = -2;
    static final int DAISY_ERR_BADHANDLE = -2;
    static final int DAISY_ERR_EXIST = -17;
    static final int DAISY_ERR_FBIG = -27;
    static final int DAISY_ERR_NOSPC = -28;


    //====================================================
    // Lower level routines on inodes

    // returns locked inode
    //@ helper
    static Inode iget(long inodenum) {
        Inode p = new Inode();
        DaisyLock.acqi(inodenum);
        DaisyDisk.readi(inodenum, p);
        return p;
    }

    // flushes Inode to disk and unlocks
    //@ helper
    static void iput(Inode inode) {
        DaisyDisk.writei(inode);
        DaisyLock.reli(inode.inodenum);
    }

    // returns locked inode, or null
    // uses dumb, sequential search thru inodes
    // modifies DaisyLock.inodeLocks[\result.inodenum];
    /*@
      performs
      action "act1" () { \result == null }
      []
      action "act2" (DaisyLock.inodeLocks[\result.inodenum]) {
          ( \old(DaisyLock.inodeLocks)[\result.inodenum] == null &&
            DaisyLock.inodeLocks[\result.inodenum] == \tid &&
            !\old(DaisyDisk.inodeUsed)[\result.inodenum] &&
            \result.used &&
	    0 <= \result.inodenum && \result.inodenum < MAXINODE)
      };
     */
    static Inode ialloc() {
        for(long i=0; i<MAXINODE; i++) {
            Inode p = iget(i);
            if( !p.used ) {
                p.used = true;
                //@ set \witness = "act2"
                return p;
            }
            DaisyLock.reli(i);
        }
        //@ set \witness = "act1"
        return null;
    }

    // takes allocated + locked inode, and unlocks and frees it
    //@ helper
    static void ifree(Inode inode) {
        inode.used = false;
        iput(inode);
    }

    static public void init() {
        byte b[] = { 0,0,0,0,0,0,0,0 };
        creat();
        //        write(0, 0, 8, b);
    }

    //====================================================
    // Lower level routines on blocks

    // allocate a disk block
    // modifies DaisyDisk.allocBit[\result])
    /*@
      performs
      action "act1" () { \result == -1 }
      []
      action "act2" (DaisyDisk.allocBit[\result]) {
	  (0 <= \result && \result < MAXBLOCK &&
	   !\old(DaisyDisk.allocBit)[\result] && DaisyDisk.allocBit[\result] &&
	   \old(DaisyLock.blockLocks)[\result] == null)
      }
     */
    static long alloc() {
        for(long i=0; i<MAXBLOCK; i++) {
            DaisyLock.acqb(i);
            if( DaisyDisk.readAllocBit(i) == false ) {
                DaisyDisk.writeAllocBit( i, true );
                DaisyLock.relb(i);
                //@ set \witness = "act2"
                return i;
            }
            DaisyLock.relb(i);
        }
        //@ set \witness = "act1"
        return -1;
    }

    //@ helper
    static void free(long blockno) {
        DaisyLock.acqb(blockno);
        DaisyDisk.writeAllocBit( blockno, false );
        DaisyLock.relb(blockno);
    }


    //@ helper
    public static void read(Inode inode, int offset, int size, byte b[]) {
        DaisyLock.acqb( inode.blockno );
        long blk = inode.blockno;
        int i;
        for (i = 0; i < size && (offset + i < inode.size); i++) {
            b[i] = DaisyDisk.readBlockByte( blk, offset + i );
        }
        DaisyLock.relb( inode.blockno );
    }

    //@ helper
    public static void write(Inode inode, int offset, int size, byte b[]) {
        DaisyLock.acqb( inode.blockno );
        long blk = inode.blockno;
        int i;
        for (i = 0; i < size; i++) {
            DaisyDisk.writeBlockByte( blk, offset + i, b[i]);
        }

        if( inode.size < offset + size) {
            inode.size = offset + size;
            DaisyDisk.writei(inode);
        }
        DaisyLock.relb( inode.blockno );
    }

    //@ helper
    public static void get_attr(Inode inode, Attribute a) {
        a.size = inode.size;
    }

    //@ helper
    public static void set_attr(Inode inode, Attribute a) {
        inode.size = a.size;
        iput(inode);
    }



    //===================================================
    // Higher level routines

    //@ ghost /*@ guarded_by[i][j] DaisyLock.inodeLocks[i] == \tid */ public long -> long -> byte inodeContents

    /*@
      performs
      action "act1" () { \result == DAISY_ERR_NOSPC }
      []
      action "act2" (DaisyDisk.inodeUsed[\result],
                     DaisyDisk.inodeBlocknos[\result],
		     DaisyDisk.allocBit[DaisyDisk.inodeBlocknos[\result]]) {
	    \old(DaisyLock.inodeLocks)[\result] == null &&
	    DaisyLock.blockLocks[DaisyDisk.inodeBlocknos[\result]] == null &&
	    !\old(DaisyDisk.inodeUsed)[\result] &&
	    DaisyDisk.inodeUsed[\result] &&
	    !\old(DaisyDisk.allocBit)[DaisyDisk.inodeBlocknos[\result]] &&
	    DaisyDisk.allocBit[DaisyDisk.inodeBlocknos[\result]]
      }
     */
    //@ helper
    public static long creat() {
        Inode inode = ialloc();
        if( inode == null ) {
            // set \witness = "act1"
            return DAISY_ERR_NOSPC;
        }
        // assert DaisyLock.inodeLocks[inode.inodenum] == \tid
        long blockno = alloc();
        if( blockno == -1 ) {
            //@ assert DaisyLock.inodeLocks[inode.inodenum] == \tid
            ifree(inode);
            // set \witness = "act1"
            return DAISY_ERR_NOSPC;
        }
        inode.blockno = blockno;
        inode.size = 0;
        iput(inode);
        // set \witness = "act2"
        return inode.inodenum;
    }

    /*@
      performs
      action "act1" () { \result != DAISY_ERR_OK }
      []
      action "act2" (DaisyDisk.inodeUsed[inodenum],
                     DaisyDisk.inodeBlocknos[inodenum],
		     DaisyDisk.allocBit[DaisyDisk.inodeBlocknos[inodenum]]) {
		     ( !DaisyDisk.inodeUsed[inodenum] &&
		       !DaisyDisk.allocBit[\old(DaisyDisk.inodeBlocknos)[inodenum]] )
      }
     */
    public static int unlink(long inodenum) {
        if (inodenum < 0 || inodenum >= MAXINODE) {
            //@ set \witness = "act1"
            return DAISY_ERR_BADHANDLE;
        }

        Inode inode = iget(inodenum);
        if ( inode == null ) {
            //@ set \witness = "act1"
            return DAISY_ERR_NOENT;
        }
        inode.used = false;
        free( inode.blockno );
        iput( inode );
        //@ set \witness = "act2"
        return DAISY_ERR_OK;
    }

    //@ helper
    public static int get_attr(long inodenum, Attribute a) {
        if (inodenum < 0 || inodenum >= MAXINODE) {
            return DAISY_ERR_BADHANDLE;
        }

        Inode inode = iget( inodenum );
        if (inode == null) {
            return DAISY_ERR_NOENT;
        }
        if (!inode.used) {
            DaisyLock.reli( inodenum );
            return DAISY_ERR_NOENT;
        }
        get_attr(inode, a);
        DaisyLock.reli (inodenum);
        return DAISY_ERR_OK;
    }

    //@ helper
    public static int set_attr(long inodenum, Attribute a) {
        if (inodenum < 0 || inodenum >= MAXINODE) {
            return DAISY_ERR_BADHANDLE;
        }

        Inode inode = iget( inodenum );
        if (inode == null) {
            return DAISY_ERR_NOENT;
        }
        if (!inode.used) {
            DaisyLock.reli( inodenum );
            return DAISY_ERR_NOENT;
        }
        set_attr(inode, a);
        return DAISY_ERR_OK;
    }

    /* performs action "act" () {
            \result == DAISY_ERR_BADHANDLE ||
	    (\forall int i; 0 <= i && i < size ==> b[i] == inodeContents[inodenum][offset+i])
        }
     */
    public static int read(long inodenum, int offset, int size, byte b[]) {
        if (inodenum < 0 || inodenum >= MAXINODE) {
            return DAISY_ERR_BADHANDLE;
        }

        Inode inode = iget( inodenum );
        if (!inode.used) {
            DaisyLock.reli( inodenum );
            return DAISY_ERR_BADHANDLE;
        }
        read(inode, offset, size, b);
        DaisyLock.reli( inode.inodenum );
        return DAISY_ERR_OK;
    }

    /* performs action "act" (inodeContents[inodenum]) {
            \result == DAISY_ERR_FBIG ||
            \result == DAISY_ERR_BADHANDLE ||
	    (\forall int i; 0 <= i && i < size ==> b[i] == inodeContents[inodenum][offset+i])
        }
     */
    public static int write(long inodenum, int offset, int size, byte b[]) {
        if (offset + size > BLOCKSIZE) {
            return DAISY_ERR_FBIG;
        }
        if (inodenum < 0 || inodenum >= MAXINODE) {
            return DAISY_ERR_BADHANDLE;
        }

        Inode inode = iget( inodenum );
        if (!inode.used) {
            DaisyLock.reli( inodenum );
            return DAISY_ERR_BADHANDLE;
        }
        write(inode, offset, size, b);
        DaisyLock.reli( inode.inodenum );
        return DAISY_ERR_OK;
    }

    public static void dumpDisk() {
        for(long i=0; i<MAXINODE; i++) {
            Inode p = iget(i);
            if (p.used) {
                System.out.println(" inode " + i + ": " + p.toString());
            }
            DaisyLock.reli(i);
        }

        for(long i=0; i<MAXBLOCK; i++) {
            String s = "";
            String t = "";
            DaisyLock.acqb(i);
            if (DaisyDisk.readAllocBit(i)) {
                s += " block " + i + ": ";
                for (int j = 0; j < 16; j++) {
                    if (j % 4 == 0 && j > 0) s += " ";
                    int n = (int)DaisyDisk.readBlockByte(i, j);
                    if (n < 0x10 && n >= 0) {
                        s += "0";
                    }
                    s += Integer.toHexString(n & 0xff);
                    t += (char)n;
                }
                System.out.println(s + " | " + t);
            }
            DaisyLock.relb(i);
        }
    }
}
