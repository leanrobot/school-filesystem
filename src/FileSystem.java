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
}