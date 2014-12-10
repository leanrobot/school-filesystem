/******************************************************************************
* Inode.java
* Programmed by: Brittany Bentley, James Hurd, Thomas Petit
* Class: CSS430 - Operating Systems
* Quarter: Autumn 2014
* University of Washington, Bothell
*  
*
******************************************************************************/


public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers

    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, ...
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer

    public static final int FLAG_UNUSED = 0;
    public static final int FLAG_USED = 1;
    public static final int FLAG_DELETE = 2;
    public static final int UNALLOCATED = -1;
    //public static final int ...

    //default constructor
    Inode( ) {                                     // a default constructor
        length = 0;
        count = 0;
        flag = FLAG_UNUSED;
        for ( int i = 0; i < directSize; i++ )
           direct[i] = UNALLOCATED;
        indirect = UNALLOCATED;
    }

    //constructor
    Inode( short iNumber ) {                       // retrieving inode from disk
        // design it by yourself.
        // makes a call to the filesystem to load the specific bytes for an
        // Inode.
        int blockId = getInodeBlock(iNumber);
        int blockOffset = getINodeOffset(iNumber);
        byte[] iNodeData = new byte[iNodeSize];

        try {
            int offset = 0;
            FileSystem.readRawData(iNodeData, blockId, blockOffset);

            // read the length (int)
            this.length = SysLib.bytes2int(iNodeData, offset);
            offset+=4;
            // read the count(short)
            this.count = SysLib.bytes2short(iNodeData, offset);
            offset+=2;
            // read the flag
            this.flag = SysLib.bytes2short(iNodeData, offset);
            offset+=2;

            // read the direct(short) array
            for(int directIndex = 0; directIndex < this.direct.length; directIndex++) {
                SysLib.short2bytes(this.direct[directIndex], iNodeData, offset);
                offset+=2;
            }

            // read the indirect (short)
            this.indirect = SysLib.bytes2short(iNodeData, offset);

        } catch(FileSystemException e) {
            //TODO error handling.
            throw e;
        }

    }

    //Saves the given inode by inumber to the disk
    public synchronized int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        int blockId = getInodeBlock(iNumber);
        int blockOffset = getINodeOffset(iNumber);
        //System.out.println("writing inode "+iNumber+" to disk at: "+blockId+" "+blockOffset+"\n");


        int offset = 0; // used to keep track when writing to iNodeData;
        byte[] iNodeData = new byte[iNodeSize];

        //write the length(int) to the iNodeData
        SysLib.int2bytes(this.length, iNodeData, offset);
        offset+=4;
        // write the count(short)
        SysLib.short2bytes(this.count, iNodeData, offset);
        offset+=2;
        //write the flag(short)
        SysLib.short2bytes(this.flag, iNodeData, offset);
        offset+=2;

        // write the direct shorts
        for(int directIndex = 0; directIndex < this.direct.length; directIndex++) {
            SysLib.short2bytes(this.direct[directIndex], iNodeData, offset);
            offset+=2;
        }

        // write the indirect(short)
        SysLib.short2bytes(this.indirect, iNodeData, offset);

        //write the iNode;
        try{
            FileSystem.writeRawData(iNodeData, blockId, blockOffset);
        } catch (FileSystemException e) {
            //TODO error handling
            throw e;
        }
        return Kernel.OK;
    }

    //gets the block in which the given inumber is located
    public static int getInodeBlock(short iNumber) {
        return ((iNumber * iNodeSize) / 512) + 1;
    }

    //returns the inode offset for the given inumber
    public static int getINodeOffset(short iNumber) {
        return (iNumber * iNodeSize) % 512;
    }
}
