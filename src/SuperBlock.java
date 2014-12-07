/******************************************************************************
* SuperBlock.java
* Programmed by: Brittany Bentley, James Hurd, Thomas Petit
* Class: CSS430 - Operating Systems
* Quarter: Autumn 2014
* University of Washington, Bothell
*  
*
******************************************************************************/

public class SuperBlock {
	private final int defaultInodeBlocks = 64;
	public int totalBlocks;
	public int totalInodes;
	public int freeList;
	
	public SuperBlock (int diskSize) {
		boolean loadSuccess = load(diskSize);
		
		if(!loadSuccess) {
			// need to format disk
			totalBlocks = diskSize;
			format(defaultInodeBlocks);
		}
		save();
	}
	
	public int format(int numInodes) {
		int status = 0;
		
		// write a zero-ed superblock.
		byte[] zeroed = new byte[Disk.blockSize];
		for(int i=0; i<zeroed.length;i++) zeroed[i] = 0;
		SysLib.rawwrite(0, zeroed);

		// set-up member variables
		// no need to set the total blocks, a format does not change this.
		totalInodes = numInodes;
		freeList = Inode.getInodeBlock((short)totalInodes)+1;
		
		save();

		// In case of failure, decrement status

		return (status != 0) ? -1 : status;
	}
	
	public boolean load(int diskSize) {
		// read the superblock from the disk
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.rawread (0, superBlock);
		totalBlocks = SysLib.bytes2int(superBlock, 0);
		totalInodes = SysLib.bytes2int(superBlock, 4);
		freeList = SysLib.bytes2int(superBlock, 8);
		
		if( totalBlocks == diskSize && totalInodes > 0 && freeList >= 2){
			//disk contents are valid
			return true;
		}
		return false;
	}

	public boolean returnBlock(int oldBlockNum){
		// Enqueue a given block to the end of the freelist
		byte[] buffer = new byte[Disk.blockSize];
		SysLib.short2bytes((short)freeList, buffer, 0);
		SysLib.rawwrite(oldBlockNum, buffer);
		freeList = oldBlockNum;
		return true;
	}

	public boolean save() {
		// create array of data to be written
		byte[] superBlockData = new byte[Disk.blockSize];

		// write the data to the array
		SysLib.int2bytes(totalBlocks, superBlockData, 0);
		SysLib.int2bytes(totalInodes, superBlockData, 4);
		SysLib.int2bytes(freeList, superBlockData, 8);

		// write the array to block 0 of the disk
		int retval = SysLib.rawwrite(0, superBlockData);
		
		// check for success and print messages accordingly
		if (SysLib.isOk(retval)) {
			return true;
		} else {
			return false;
		}
	}

	int sync(){
		boolean saveSuccess = save();
		if(saveSuccess) {
			SysLib.cout("Superblock synchronized.\n");
			return Kernel.OK;
		}
		return Kernel.ERROR;
	}
}
