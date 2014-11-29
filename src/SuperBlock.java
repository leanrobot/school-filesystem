
public class SuperBlock {
	private final int defaultInodeBlocks = 64;
	public int totalBlocks;
	public int totalInodes;
	public int freeList;
	
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
			SysLib.format(defaultInodeBlocks);
		}
			
		
		
		
	}
}
