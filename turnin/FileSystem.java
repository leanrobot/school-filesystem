/******************************************************************************
* FileSystem.java
* Programmed by: Brittany Bentley, James Hurd, Thomas Petit
* Class: CSS430 - Operating Systems
* Quarter: Autumn 2014
* University of Washington, Bothell
*  
*
******************************************************************************/

public class FileSystem {
    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;
    
    // index 0 is not used, since this is the superblock;
    private Inode[] inodeCache;

    //constructor
    public FileSystem(int diskBlocks) {
        superBlock = new SuperBlock(diskBlocks);
        initInodes(superBlock.totalInodes);

        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileTable(this, directory);

        // If a directory is available on the DISK, load it into memory.
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);
        if(dirSize > 0) {
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            directory.bytes2directory(dirData);
        }
        close (dirEnt);
    }

    // This method will return the next free block, and increment the 
    // free list pointer.
    public int acquireFreeBlock() {
        if(superBlock.freeList < superBlock.totalBlocks)
            return superBlock.freeList++;
        return Kernel.ERROR;
    }

    // This method handles the allocation of all new blocks. 
    // It loops through the direct array, then the indirect block, until an
    // UNALLOCATED space is found. It then acquires a free block and adds it to 
    // the file.
    //
    // If the indirect has not been allocated, it will create this also.
    protected int allocateNewBlock(Inode inode) {
        int status;
        
        int newBlock = acquireFreeBlock();

        // A new block is available, continue allocation.
        if(!SysLib.isError(newBlock)) {
            // ====== DIRECT ALLOCATION ========================================
            for(int i=0; i<inode.direct.length; i++) {
                if(inode.direct[i] == Inode.UNALLOCATED) {
                    inode.direct[i] = (short) newBlock;
                    return newBlock;
                }
            }

            // ===== INDIRECT INDEX ALLOCATION =================================
            // if this is the first indirect allocation, we need to setup 
            // an indirect block.
            int indirectBlock = inode.indirect;
            // new indirect block allocated, need to create one
            if(indirectBlock == Inode.UNALLOCATED) {
                indirectBlock = acquireFreeBlock();
                
                if(SysLib.isError(indirectBlock)) {
                    return Kernel.ERROR;
                }
                
                //set on the inode.
                inode.indirect = (short)indirectBlock;
                // initialize the indirect block all to UNALLOCATED
                byte[] indirect = getBlockArray();
                for(int offset=0; offset<indirect.length; offset+=2) {
                    SysLib.short2bytes((short)Inode.UNALLOCATED, indirect, offset);
                }
                // write it back to the disk.
                SysLib.rawwrite(inode.indirect, indirect);
            }
            
            // ===== INDIRECT BLOCK ALLOCATION =================================
            byte[] indirect = getBlockArray();
            status = SysLib.rawread(indirectBlock, indirect);
            
            // Find the first UNALLOCATED block in the indirect index, and 
            // sets it to the newly allocated block. A save to the disk follows.
            for(int offset=0; offset<indirect.length; offset+=2) {
                short cur = SysLib.bytes2short(indirect, offset);

                // Allocated this one.
                if(cur == Inode.UNALLOCATED) {
                    if(SysLib.isError(newBlock)) {
                        return Kernel.ERROR;
                    }
                    SysLib.short2bytes((short)newBlock, indirect, offset);
                    SysLib.rawwrite(indirectBlock, indirect);
                    return newBlock;
                }
            }
        }

        // No blocks are available for allocation, return an error.
        return Kernel.ERROR;
    }
    
    // closes the file indicated by the parameter FileTableEntry
    // If close is called on a file flagged for deletion, 
    // deallocates and deregister's file's inode.
    public boolean close(FileTableEntry ftEnt){
        ftEnt.inode.count--;
        if(ftEnt.inode.count<=0) {
            ftEnt.seekPtr = 0;
            if (ftEnt.inode.flag == Inode.FLAG_DELETE){ 
                short iNumber = ftEnt.iNumber;
                boolean deleted = directory.ifree(iNumber);
                ftEnt.inode.flag = Inode.FLAG_UNUSED;
                deallocateInode(ftEnt.inode);
                return deleted;
            }
        }
        return true;
    }
    
    //Deallocates the inode given as the parameter
    //All member variables are reset as well as the
    //direct and indirect pointers
    private void deallocateInode(Inode toDeallocate) {
        // check for indirect blocks, return them if they exists
        if(toDeallocate.indirect != Inode.UNALLOCATED) {
            toDeallocate.indirect = Inode.UNALLOCATED;
        }
        
        // reset all variables
        toDeallocate.count = 0;
        toDeallocate.length = 0;
        toDeallocate.flag = Inode.FLAG_UNUSED;
        
        // reset all direct pointers
        for (int i = 0; i < toDeallocate.direct.length; ++i) {
            toDeallocate.direct[i] = Inode.UNALLOCATED;
        }
    }

    // Deletes the file specified by fileName, unless it is currently open
    // If file is currently open, sets a flag to prevent further opening but 
    // file not destroyed. Delete upon last open closed is handled by close
    public boolean delete(String fileName) {    
        boolean deleted = false;
        short fileINum = directory.namei(fileName);
        Inode deleteInode = getInode(fileINum);
        if (deleteInode.count == 0){            //not open, delete
            deleted = directory.ifree(fileINum);
            deallocateInode(deleteInode);
            deleteInode.flag = Inode.FLAG_DELETE;   //prevents further opening
        }
        else{
            deleteInode.flag = Inode.FLAG_DELETE;   //no new opens, not deleted
        }       
        return deleted;
    }

    // Formats the FileSystem on DISK, as well as the superblock.
    public boolean format(int maxInodes) {
        int status;
        
        status = superBlock.format(maxInodes);
        
        // zero out all inode blocks.
        byte[] zeroed = new byte[Disk.blockSize];
        for(int i=0; i<zeroed.length; i++) zeroed[i] = 0;

        int LastBlockToZero = Inode.getInodeBlock((short)superBlock.totalInodes);
        for(int blockId=0; blockId<LastBlockToZero; blockId++) {
            SysLib.rawwrite(blockId, zeroed);
        }
        
        if(SysLib.isError(status)) return false;
        
        // reinitialize members of FS and write to the DISK.
        initInodes(maxInodes);
        this.directory = new Directory(maxInodes);
        this.fileTable = new FileTable(this, this.directory);
        sync();

        return SysLib.isOk(status);
    }
    
    // returns the size in bytes of the file
    public int fsize(FileTableEntry ftEnt){
        return ftEnt.size();
    }
    
    // Retrieve an inode from the cache.
    public Inode getInode(short iNumber) {
        return inodeCache[iNumber];
    }
    
    // Given the total inodes, all new inodes are initialized, stored in the
    // cache, then written to the disk.
    public int initInodes(int totalInodes) {
        inodeCache = new Inode[totalInodes];
        for(short i=0; i<inodeCache.length; i++) {
            inodeCache[i] = new Inode();
            inodeCache[i].toDisk((short)(i+1));
        }
        return Kernel.OK;
    }
    

    //Opens a file. Takes two parameters, the name of the file to be opened
    //(first parameter) and the mode in which to open it (second parameter)
    //If the inode for the FileTableEntry does not exist and the mode parameter
    //is not read, then an inode is allocated. If the mode is write, all the
    //the blocks in the inode are deallocated.
    public FileTableEntry open(String fileName, String mode){
        FileTableEntry newFileTableEntry = fileTable.falloc(fileName, mode);

        //Deleted files cannot be reopened
        if (newFileTableEntry.inode.flag == Inode.FLAG_DELETE){
            return null;
        }

        short inode = directory.namei(fileName);

        // If Inode does not exist, cannot be opened in read mode, if not read mode, allocate
        if (inode < 0 ) {
            if (mode.equals("r")) {
                return null;
            } else {
                directory.ialloc(fileName);
            }
        } 
    
        // if mode is write, blow away all blocks in the inode. 
        // if mode is append, set seekptr to the end of the data.
        if(mode.equals("w")) {
            deallocateInode(newFileTableEntry.inode);
        } else if (mode.equals("a")) {
            newFileTableEntry.seekPtr = newFileTableEntry.inode.length;
        }
        
        return newFileTableEntry;
    }
    
    // reads up to buffer.length bytes from the file indicated by fte, 
    // starting at the position currently pointed to by the seek pointer.
    public int read(FileTableEntry fte, byte[] buffer){
        if(!fte.isOpen() || !(fte.mode.equals("r") || fte.mode.equals("w+"))) {
            return Kernel.ERROR;
        }
        
        byte[] blockData = getBlockArray();
        // a local seekPtr is used so that in the event of an error, the 
        // entrie's seekPtr is not moved.
        int seekPtr = fte.seekPtr;
        int status;
        
        // Outer loop controls the index inside the buffer, and whether or not
        // to load a new disk block to continue reading. When the inner loop
        // falls out of bounds on a disk block, then the outer loop will load
        // the next so that the buffer can continue to be populated.
        for(int bufferIndex=0; 
            bufferIndex<buffer.length && seekPtr < fte.inode.length;) {
            
            int blockId = getSeekBlock(fte.inode, seekPtr);

            if(blockId == Inode.UNALLOCATED) {
                return Kernel.ERROR;
            }

            // read the block to memory.
            status = SysLib.rawread(blockId, blockData);
            if (SysLib.isError(status)){
                return Kernel.ERROR;
            }

            // move the data from the block into the buffer
            for(int blockOffset=seekPtr % Disk.blockSize;
                blockOffset < blockData.length && bufferIndex < buffer.length;
                bufferIndex++, blockOffset++, seekPtr++) {

                buffer[bufferIndex] = blockData[blockOffset];
            }
        }

        // calculate the amount read
        int amountRead = seekPtr - fte.seekPtr;
        // copy operation is complete, update inode and file table entry structures.
        fte.seekPtr = seekPtr;
        
        return amountRead;
    }
    
    // Updates the seek pointer of the ftEnt
    public int seek(FileTableEntry ftEnt, int offset, int whence){
        return ftEnt.setSeekPtr(offset, whence);
    }
    
    // Synchronizes the FileSystem to the disk by writing all inodes, and the 
    // directory.
    public void sync(){
        superBlock.sync();
        for(short i=1; i<inodeCache.length; i++) {
            Inode n = inodeCache[i];
            synchronized(n) {
                n.toDisk(i);
            }
        }
        FileTableEntry dirEnt = open("/", "w");
        write(dirEnt, directory.directory2bytes());
        SysLib.cout("FileSystem Synced.\n");
    }

    // Writes the buffer to the file specified by the FileTableEntry.
    // Writes starting at the location of the seek pointer
    // Returns the number of bytes that have been written
    public int write(FileTableEntry fte, byte[] buffer) {
        // disallow writing for specific conditions
        if(!fte.isOpen() || fte.mode.equals("r")) {
            return Kernel.ERROR;
        }

        byte[] blockData = getBlockArray();
        // use local copy of seekPtr in order to maintain the original version
        // in the event of an error.
        int seekPtr = fte.seekPtr;
        int status;

        // Outer loop is used to retrieve the block that needs to be written to.
        // The inner loop copies data from the buffer into the block.
        // After the double bounds check on the inner loop is false, the
        // disk block is written back to the DISK.
        for(int bufferIndex=0; bufferIndex<buffer.length;) {
            int blockId = getSeekBlock(fte.inode, seekPtr);
            // if the block is unallocated, allocate a new one for the inode.
            if(blockId == Inode.UNALLOCATED) {
                blockId = allocateNewBlock(fte.inode);
                if(SysLib.isError(blockId)) {
                    return Kernel.ERROR;
                }
            }
            // read the block to memory.
            status = SysLib.rawread(blockId, blockData);

            // move the data from the buffer into the block.
            for(int blockOffset=seekPtr % Disk.blockSize;
                blockOffset < blockData.length && bufferIndex < buffer.length;
                bufferIndex++, blockOffset++, seekPtr++) {

                blockData[blockOffset] = buffer[bufferIndex];
            }
            // Writing to this block is completed, write it back to the disk.
            status = SysLib.rawwrite(blockId, blockData);
        }

        // calculate the amount written
        int amountWritten = seekPtr - fte.seekPtr;
        // copy operation is complete, update inode and 
        // file table entry structures.
        if(seekPtr > fte.inode.length) {
            fte.inode.length = seekPtr;
        }
        fte.seekPtr = seekPtr;
        
        return amountWritten;
    }

    // Returns a standard sized byte[] for storing block data.
    public static byte[] getBlockArray() {
        return new byte[Disk.blockSize];
    }

    //Returns the number of the block where the seek pointer is located
    protected static int getSeekBlock(Inode inode, int seekPtr) {
        // retrieve the direct list from the inode.
        int index = seekPtr/Disk.blockSize;
        if(index < inode.direct.length)
            return inode.direct[index];

        if(inode.indirect != Inode.UNALLOCATED) {
            byte[] indirect = getBlockArray();
            SysLib.rawread(inode.indirect, indirect);
            index = (index-inode.direct.length) * 2;

            short indirectBlock = SysLib.bytes2short(indirect, index);
            return indirectBlock;
        }
        return Inode.UNALLOCATED;
    }


   //Returns how far the seek pointer is in the block 
   protected static int getSeekOffset(int seekPtr) {
    return seekPtr % Disk.blockSize;
}

   //Reads the data from the disk into a byte array
    public static int readRawData(byte[] data, int blockId, int blockOffset) throws FileSystemException {
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
    
    //Writes the data into the disk
    public static int writeRawData(byte[] data, int blockId, int blockOffset) throws FileSystemException {
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
}
