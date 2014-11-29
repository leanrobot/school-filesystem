public class FileSystem {
	private SuperBlock superBlock;
	private Directory Directory;
	private FileStructureTable fileTable;

	public FileSystem( int diskBlocks) {
		superblock = new SuperBlock(diskBlocks);
		directory = new Directory(superBlock.totalInodes);
		fileTable = new FileStructureTable(directory);

		FileTableEntry dirEnt = open("/", "r");
		int dirSize = fsize(dirEnt);
		if(dirSize > 0) {
			byte[] dirData = new byte[dirSize];
			read(dirEnt, dirData);
			directory.bytes2directory(dirdata);
		}
		close (dirEnt);
	}

	/*
	aux data structure: (Inode cache) array with space for all Inode objects.
	def getInode(iNodeNum):
		if the inode is not in memory yet, load it from the disk into
		the working inode cache.

		after retrieval, return the Inode to the caller.
	*/

	/*
	def readRawData(int[] data, blockId, blockOffset):
	def writeRawData(int[] data, blockId, blockOffset):
		abstraction to allow only specific pieces of a block to be written,
		instead of the entire block at once.

		This will be used by the Inode and all other physical writes to the disk
		by the filesystem.
	*/
}