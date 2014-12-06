
public class SuperBlock {
	private final int defaultInodeBlocks = 64;
	public int totalBlocks;
	public int totalInodes;
	public int freeList;
	private static final int DEFAULT_TOTAL_BLOCKS = 1000; 
	
	public SuperBlock (int diskSize) {
		// read the superblock from the disk
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.rawread (0, superBlock);
		totalBlocks = SysLib.bytes2int(superBlock, 0);
		totalInodes = SysLib.bytes2int(superBlock, 4);
		freeList = SysLib.bytes2int(superBlock, 8);
		
		if( totalBlocks == diskSize && totalInodes > 0 && freeList >= 2){
			//disk contents are valid
			return;
		} else {
			// need to format disk
			totalBlocks = diskSize;
			format(defaultInodeBlocks);
		}
	}


	int format(int numInodes) {
		int status = 0;

		totalBlocks = DEFAULT_TOTAL_BLOCKS;
		totalInodes = numInodes;
		freeList = Inode.getInodeBlock((short)totalInodes)+1;

		for (short i= 1; i <= numInodes; ++i) {
			Inode temp = new Inode();
			status -= temp.toDisk(i);
		}

		status -= sync();

		return (status != 0) ? -1 : status;
	}

	int sync(){
		// create array of data to be written
		byte[] superBlockData = new byte[Disk.blockSize];

		// write the data to the array
		SysLib.int2bytes(totalBlocks, superBlockData, 0);
		SysLib.int2bytes(totalInodes, superBlockData, 4);
		SysLib.int2bytes(freeList, superBlockData, 8);

		// write the array to block 0 of the disk
		int retval = SysLib.rawwrite(0, superBlockData);
		
		// check for succes and print messages accordingly
		if (SysLib.isOk(retval)) {
			SysLib.cout("Superblock synchronized");
		} else {
			SysLib.cout("Failure when synchronizing superblock");
		}
		
		return retval;
	}

    // James
	public boolean returnBlock(int oldBlockNum){
		byte[] buffer = new byte[Disk.blockSize];
		SysLib.short2bytes((short)freeList, buffer, 0);
		SysLib.rawwrite(oldBlockNum, buffer);
		freeList = oldBlockNum;
		return true;
	}
}
