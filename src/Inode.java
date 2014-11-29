public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers

   public int length;                             // file size in bytes
   public short count;                            // # file-table entries pointing to this
   public short flag;                             // 0 = unused, 1 = used, ...
   public short direct[] = new short[directSize]; // direct pointers
   public short indirect;                         // a indirect pointer

   Inode( ) {                                     // a default constructor
      length = 0;
      count = 0;
      flag = 1;
      for ( int i = 0; i < directSize; i++ )
         direct[i] = -1;
      indirect = -1;
   }

    Inode( short iNumber ) {                       // retrieving inode from disk
        // design it by yourself.
        // makes a call to the filesystem to load the specific bytes for an
        // Inode.
        int blockId = getInodeBlock(iNumber);
        int blockOffset = getINodeOffset(iNumber);
        byte[] iNodeData = new byte[iNodeSize];

        try {
            int offset = 0;
            readRawData(iNodeData, blockId, blockOffset);

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

    public int getInodeBlock(short iNumber) {
        return ((iNumber * iNodeSize) / 512) + 1;
    }

    public int getINodeOffset(short iNumber) {
        return (iNumber * iNodeSize) % 512;
    }

    public synchronized int toDisk( short iNumber ) {                  // save to disk as the i-th inode
        int blockId = getInodeBlock(iNumber);
        int blockOffset = getINodeOffset(iNumber);
    	System.out.println("writing inode "+iNumber+" to disk at: "+blockId+" "+blockOffset+"\n");


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
            writeRawData(iNodeData, blockId, blockOffset);
        } catch (FileSystemException e) {
            //TODO error handling
            throw e;
        }
        return Kernel.OK;
    }

    public int readRawData(byte[] data, int blockId, int blockOffset) throws FileSystemException {
        if(blockId < 0  ||
            blockOffset+data.length > Disk.blockSize || blockOffset < 0) {

            throw new FileSystemException("invalid blockId, or offset for write");
        }

        byte[] diskContents = new byte[Disk.blockSize];

        int status = SysLib.rawread(blockId, diskContents);

        if(SysLib.isError(status)) {
            throw new FileSystemException("Error read specific data off of disk");
        }

        for(int offset = blockOffset, i=0; 
            i < data.length && offset < diskContents.length;
            offset++, i++) {

            data[i] = diskContents[offset];
        }

        return Kernel.OK;
    }


   public int writeRawData(byte[] data, int blockId, int blockOffset) throws FileSystemException {
      // check the following
      //    blockId is valid, offset + data.length < blockLength
      if(blockId < 0  ||
         blockOffset+data.length > Disk.blockSize || blockOffset < 0) {
         
         throw new FileSystemException("invalid blockId, or offset for write");
      }

      byte[] diskContents = getBlockArray();
      int status = SysLib.rawread(blockId, diskContents);

      if(SysLib.isError(status)) {
         throw new FileSystemException("raw write was unsuccessful");
      }

      for(int offset = blockOffset, i=0;
          offset < blockOffset + data.length && i < data.length; 
          offset++, i++) {

         diskContents[offset] = data[i];
      }

      status = SysLib.rawwrite(blockId, diskContents);

      return Kernel.OK;
   }

    private byte[] getBlockArray() {
        return new byte[Disk.blockSize];
    }
}