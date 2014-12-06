import java.util.Vector;


public class FileSystem {
	private SuperBlock superBlock;
	private Directory directory;
	private FileTable fileTable;
	
	// index 0 is not used, since this is the superblock;
	private Inode[] inodeCache;

	public FileSystem( int diskBlocks) {
		superBlock = new SuperBlock(diskBlocks);
		inodeCache = new Inode[superBlock.totalInodes +1];
		loadInodeCache(inodeCache);

		directory = new Directory(superBlock.totalInodes);
		fileTable = new FileTable(this, directory);

		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if(dirSize > 0) {
		 	byte[] dirData = new byte[dirSize];
		 	read(dirEnt, dirData);
		 	directory.bytes2directory(dirData);
		}
		close (dirEnt);
	}

	public int loadInodeCache(Inode[] cache) {
		for(short i=1; i<cache.length; i++) {
			cache[i] = new Inode(i);
		}
		return Kernel.OK;
	}

	public void sync(){
		SysLib.cout("Syncing Filesystem to disk\n");
		superBlock.sync();
		SysLib.cout("\tSync Superblock done.\n");
		SysLib.cout("\tSync inodes: ");
		for(short i=1; i<inodeCache.length; i++) {
			Inode n = inodeCache[i];
			synchronized(n) {
				n.toDisk(i);
				SysLib.cout(".");
			}
		}
		SysLib.cout("\nFileSystem Synced!\n");
	}
	
	public int read(FileTableEntry fte, byte[] buffer){
		//reads up to buffer.length bytes from the file indicated by fte, 
		//starting at the position currently pointed to by the seek pointer.
		if(!fte.isOpen() || !(fte.mode.equals("r") || fte.mode.equals("w+"))) {
			return Kernel.ERROR;
		}
		
    	byte[] blockData = getBlockArray();
    	int seekPtr = fte.seekPtr;
    	int status;
    	
    	for(int bufferIndex=0; bufferIndex<buffer.length && seekPtr < fte.inode.length; /*bufferIndex++*/) {
    		int blockId = getSeekBlock(fte.inode, seekPtr);
    		// if the block is unallocated, return an error
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

	// TODO
	//		Error handling of statuses return from raw read/write.
	//		Allocation of indirect blocks.
	//		check to see if the fte is open.
	public int write(FileTableEntry fte, byte[] buffer) {
		if(!fte.isOpen() || fte.mode.equals("r")) {
			return Kernel.ERROR;
		}
    	byte[] blockData = getBlockArray();
    	int seekPtr = fte.seekPtr;
    	int status;

    	for(int bufferIndex=0; bufferIndex<buffer.length; /*bufferIndex++*/) {
    		int blockId = getSeekBlock(fte.inode, seekPtr);
    		// if the block is unallocated, allocate a new one for the inode.
            if(blockId == Inode.UNALLOCATED) {
            	//TODO handling of allocation error, return # of bytes written instead?
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
    	// copy operation is complete, update inode and file table entry structures.
    	if(seekPtr > fte.inode.length) {
    		fte.inode.length = seekPtr;
    	}
    	fte.seekPtr = seekPtr;
    	
    	return amountWritten;
    }

    // TODO add handling for indirect file handles.
    protected int allocateNewBlock(Inode inode) {
    	int status;
    	
        int newBlock = acquireFreeBlock();
        if(!SysLib.isError(newBlock)) {
        	// allocating direct blocks
        	for(int i=0; i<inode.direct.length; i++) {
        		if(inode.direct[i] == Inode.UNALLOCATED) {
        			inode.direct[i] = (short) newBlock;
        			return Kernel.OK;
        		}
        	}
        	// allocating indirect blocks
        	// if this is the first indirect allocation, we need to setup an indirect block.
        	int indirectBlock = inode.indirect;
        	if(indirectBlock == Inode.UNALLOCATED) {
        		indirectBlock = acquireFreeBlock();
        		if(SysLib.isError(indirectBlock)) {
        			return Kernel.ERROR;
        		}
        		inode.indirect = (short)indirectBlock;
        		// initialize the indirect block
        		byte[] indirect = getBlockArray();
        		for(int offset=0; offset<indirect.length; offset+=2) {
        			SysLib.short2bytes((short)Inode.UNALLOCATED, indirect, offset);
        		}
        		SysLib.rawwrite(inode.indirect, indirect);
        	}
        	byte[] indirect = getBlockArray();
        	status = SysLib.rawread(indirectBlock, indirect);

        	for(int offset=0; offset<indirect.length; offset+=2) {
        		short cur = SysLib.bytes2short(indirect, offset);
        		if(cur == Inode.UNALLOCATED) {
        			newBlock = acquireFreeBlock();
        			if(SysLib.isError(newBlock)) {
        				return Kernel.ERROR;
        			}
        			SysLib.short2bytes((short)newBlock, indirect, offset);
        			return Kernel.OK;
        		}
        	}
        }
        return Kernel.ERROR;
    }

    // TODO add handling for indirect file handles.
    protected static int getSeekBlock(Inode inode, int seekPtr) {
        // retrieve the direct list from the inode.
        int index = seekPtr/Disk.blockSize;
        if(index < inode.direct.length)
	        return inode.direct[index];
        // check if the indirect block is set.
        if(inode.indirect != Inode.UNALLOCATED) {
        	short[] indirect = getIndirectArray(inode);
        	index -= inode.direct.length;
        	return indirect[index];
        }
	    return Inode.UNALLOCATED;
    }
    
    public static short[] getIndirectArray(Inode inode) {
    	int status;
    	int indirectBlockId = inode.indirect;
    	
    	byte[] blockData = getBlockArray();
    	short[] indirectBlocks = new short[blockData.length/2];
    	
    	status = SysLib.rawread(indirectBlockId, blockData);
    	
    	for(int offset=0, indirectOffset=0; offset<blockData.length; offset+=2, indirectOffset++){
    		indirectBlocks[indirectOffset] = SysLib.bytes2short(blockData, offset);
    	}
    	
    	return indirectBlocks;	
    }

    protected static int getSeekOffset(int seekPtr) {
        return seekPtr % Disk.blockSize;
    }
	
	// James
	public FileTableEntry open(String fileName, String mode){
		FileTableEntry newFileTableEntry = fileTable.falloc(fileName, mode);
		
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
		} else {
			// Handle w+ or r ? I dont think we need to....
		}
		
		return newFileTableEntry;
	}
	
	public boolean close(FileTableEntry ftEnt){
		//closes the file corresponding to fd, commits all file transactions on this file, and 
		//unregisters fd from the user file descriptor table of the calling thread's TCB. The 
		//return value is 0 in success, otherwise -1.
		ftEnt.inode.count--;
		if(ftEnt.inode.count<=0) {
			ftEnt.seekPtr = 0;
			if (ftEnt.inode.flag == Inode.FLAG_DELETE){
				short iNumber = ftEnt.iNumber;
				boolean deleted = directory.ifree(iNumber);
				ftEnt.inode.flag = Inode.FLAG_UNUSED;
				return deleted;
			}
		}
		return true;
	}
	
	public int fsize(FileTableEntry ftEnt){
		//returns the size in bytes of the file indicated by fd.
		return ftEnt.size();
	}
	
	public int seek(FileTableEntry ftEnt, int offset, int whence){
		//Updates the seek pointer of the ftEnt
		return ftEnt.setSeekPtr(offset, whence);
	}
	
	public boolean format(int maxInodes) {
		int status = superBlock.format(maxInodes);
		return SysLib.isOk(status);
	}
	
	public boolean delete(String fileName) {
		//destroys the file specified by fileName. If the file is currently open, it is not 
		//destroyed until the last open on it is closed, but new attempts to open it will fail.
		
		//count on the iNode is correct
		boolean deleted = false;
		short fileINum = directory.namei(fileName);
		Inode deleteInode = getInode(fileINum);
		if (deleteInode.count == 0){			//not open, delete
			deleted = directory.ifree(fileINum);
			deleteInode.flag = Inode.FLAG_UNUSED;		
		}
		else{
			deleteInode.flag = Inode.FLAG_DELETE;		//prevent new attempts from opening but does not delete
		}
		
		return deleted;
	}

	/*
	aux data structure: (Inode cache) array with space for all Inode objects.
	def getInode(iNodeNum):
		if the inode is not in memory yet, load it from the disk into
		the working inode cache.

		after retrieval, return the Inode to the caller.
	*/

	public int acquireFreeBlock() {
		if(superBlock.freeList < superBlock.totalBlocks)
			return superBlock.freeList++;
		return Kernel.ERROR;
	}

	public Inode getInode(short iNumber) {
		// If Inode not in cache, load it in.
		if(inodeCache[iNumber] == null) {
			Inode inode = new Inode(iNumber);
			inodeCache[iNumber] = inode;
		}

		return inodeCache[iNumber];
	}

	public int getFreeNodeNumber() {
		for(int i=1; i<inodeCache.length; i++) {
			Inode n = inodeCache[i];
			if(n.flag == 0) {
				return i;
			}
		}
		return Kernel.ERROR;
	}

	public Inode acquireFreeInode() {
		int freeINumber = getFreeNodeNumber();
		if(SysLib.isError(freeINumber)) {
			Inode n = inodeCache[freeINumber];
			synchronized(n) {
				n.flag = 1;
			}
			return n;
		}
		return null;
	}

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

    public static byte[] getBlockArray() {
        return new byte[Disk.blockSize];
    }
    // James
    private void deallocateInode(Inode toDeallocate) {
    	int seekptr = toDeallocate.length;
    	int block = getSeekBlock(toDeallocate, seekptr);
    	
    	while (seekptr > 0) {
    		block = getSeekBlock(toDeallocate, seekptr);
    		superBlock.returnBlock(block);
    		seekptr -= Disk.blockSize;
    	}
    	
    	// check for indirect blocks, return them if they exists
    	if(toDeallocate.indirect != -1) {
    		superBlock.returnBlock(toDeallocate.indirect);
    	}
    	
    	// reset all variables
    	toDeallocate.count = 0;
    	toDeallocate.length = 0;
    	toDeallocate.flag = -1;
    	
    	// reset all direct pointers
    	for (int i = 0; i < toDeallocate.direct.length; ++i) {
    		toDeallocate.direct[i] = -1;
       	}
    }

}