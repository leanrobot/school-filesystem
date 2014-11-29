
public class FileSystem {
	private SuperBlock superBlock;
	// private Directory directory;
	// private FileStructureTable fileTable;
	
	// index 0 is not used, since this is the superblock;
	private Inode[] inodeCache;

	public FileSystem( int diskBlocks) {
		superBlock = new SuperBlock(diskBlocks);
		inodeCache = new Inode[superBlock.totalInodes +1];
		loadInodeCache(inodeCache);

		// directory = new Directory(superBlock.totalInodes);
		// fileTable = new FileStructureTable(directory);

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
	
	public int open(String fileName, String mode){
		//FileTableEntry newFileTableEntry = fileTable.falloc(fileName, mode);





		//if bad - return null;

		//if good - return newFileTableEntry;


		//opens the file specified by the fileName string in the given mode (where 
		//"r" = ready only, "w" = write only, "w+" = read/write, "a" = append), and allocates 
		//a new file descriptor, fd to this file. The file is created if it does not exist in 
		//the mode "w", "w+" or "a". SysLib.open must return a negative number as an error 
		//value if the file does not exist in the mode "r". Note that the file descriptors 0, 
		//1, and 2 are reserved as the standard input, output, and error, and therefore a newly 
		//opened file must receive a new descriptor numbered in the range between 3 and 31. If 
		//the calling thread's user file descriptor table is full, SysLib.open should return an 
		//error value. The seek pointer is initialized to zero in the mode "r", "w", and "w+", 
		//whereas initialized at the end of the file in the mode "a".
		return Kernel.ERROR;
	}
	
	public int close(FileTableEntry ftEnt){
		//closes the file corresponding to fd, commits all file transactions on this file, and 
		//unregisters fd from the user file descriptor table of the calling thread's TCB. The 
		//return value is 0 in success, otherwise -1.
		return Kernel.ERROR;
	}
	
	public int fsize(FileTableEntry ftEnt){
		//returns the size in bytes of the file indicated by fd.
		return Kernel.ERROR;
	}
	
	public int seek(FileTableEntry ftEnt, int offset, int whence){
		//Updates the seek pointer corresponding to fd as follows:
		//If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes 
		//from the beginning of the file
		//If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current 
		//value plus the offset. The offset can be positive or negative.
		//If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the 
		//file plus the offset. The offset can be positive or negative. 
		//If all cases you should clamp the seek pointer the size of the file. For example, 
		//if the user attempts to set the seek pointer to a negative number you must clamp 
		//it to zero. If the user attempts to set the pointer to beyond the file size, you 
		//must set the seek pointer to the end of the file. In both cases, you should return 
		//success.
		return Kernel.ERROR;
	}
	
	public boolean format(int maxInodes) {
		//formats the disk, (i.e., Disk.java's data contents). The parameter files specifies 
		//the maximum number of files to be created, (i.e., the number of inodes to be 
		//allocated) in your file system. The return value is 0 on success, otherwise -1.
		int status = superBlock.format(maxInodes);
		return SysLib.isOk(status);
	}
	
	public int delete(String fileName) {
		//destroys the file specified by fileName. If the file is currently open, it is not 
		//destroyed until the last open on it is closed, but new attempts to open it will fail.
		return Kernel.ERROR;
	}

	/*
	aux data structure: (Inode cache) array with space for all Inode objects.
	def getInode(iNodeNum):
		if the inode is not in memory yet, load it from the disk into
		the working inode cache.

		after retrieval, return the Inode to the caller.
	*/

	public Inode getInode(short iNumber) {
		// If Inode not in cache, load it in.
		if(inodeCache[iNumber] == null) {
			Inode inode = new Inode(iNumber);
			inodeCache[iNumber] = inode;
		}

		return inodeCache[iNumber];
	}

	public Inode acquireFreeInode() {
		for(Inode n : inodeCache) {
			if(n.flag == 0) {
				synchronized(n) {
					n.flag = 1;
				}
				return n;
			}
		}
		return null;
	}


}