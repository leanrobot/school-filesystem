
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

		// FileTableEntry dirEnt = open("/", "r");
		// int dirSize = fsize(dirEnt);
		// if(dirSize > 0) {
		// 	byte[] dirData = new byte[dirSize];
		// 	read(dirEnt, dirData);
		// 	directory.bytes2directory(dirdata);
		// }
		// close (dirEnt);
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
	
	public int read(FileTableEntry ftEnt, byte[] buffer){
		//int bufferSize = buffer.length;
		
		
		//reads up to buffer.length bytes from the file indicated by fd, 
		//starting at the position currently pointed to by the seek pointer. 
		//If bytes remaining between the current seek pointer and the end of file are less 
		//than buffer.length, SysLib.read reads as many bytes as possible, putting them into 
		//the beginning of buffer. It increments the seek pointer by the number of bytes to 
		//have been read. The return value is the number of bytes that have been read, or a 
		//negative value upon an error.
		return Kernel.ERROR;
	}
	
	public int write(FileTableEntry ftEnt, byte[] buffer){

		//writes the contents of buffer to the file indicated by fd, starting at the 
		//position indicated by the seek pointer. The operation may overwrite existing data 
		//in the file and/or append to the end of the file. SysLib.write increments the seek 
		//pointer by the number of bytes to have been written. The return value is the number 
		//of bytes that have been written, or a negative value upon an error.
		return Kernel.ERROR;
	}
	
	public FileTableEntry open(String fileName, String mode){
		FileTableEntry newFileTableEntry = fileTable.falloc(fileName, mode);

		//TODO: SysLib.open must return a negative number as an error 
		//value if the file does not exist in the mode "r".  
		return newFileTableEntry;
	}
	
	public boolean close(FileTableEntry ftEnt){
		//closes the file corresponding to fd, commits all file transactions on this file, and 
		//unregisters fd from the user file descriptor table of the calling thread's TCB. The 
		//return value is 0 in success, otherwise -1.
		return false;
	}
	
	public int fsize(FileTableEntry ftEnt){
		//returns the size in bytes of the file indicated by fd.
		return Kernel.ERROR;
	}
	
	public int seek(FileTableEntry ftEnt, int offset, int whence){
		//Updates the seek pointer of the ftEnt
		return ftEnt.setSeekPtr(offset, whence);
	}
	
	public boolean format(int maxInodes) {
		//formats the disk, (i.e., Disk.java's data contents). The parameter files specifies 
		//the maximum number of files to be created, (i.e., the number of inodes to be 
		//allocated) in your file system. The return value is 0 on success, otherwise -1.
		int status = superBlock.format(maxInodes);
		return SysLib.isOk(status);
	}
	
	public boolean delete(String fileName) {
		//destroys the file specified by fileName. If the file is currently open, it is not 
		//destroyed until the last open on it is closed, but new attempts to open it will fail.
		return false;
	}

	/*
	aux data structure: (Inode cache) array with space for all Inode objects.
	def getInode(iNodeNum):
		if the inode is not in memory yet, load it from the disk into
		the working inode cache.

		after retrieval, return the Inode to the caller.
	*/

	public int acquireFreeBlock() {
		return superBlock.freeList++;
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

}