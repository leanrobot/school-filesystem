public class FileTableEntry {  // Each table entry should have
    public int seekPtr;        //    a file seek pointer
    public final Inode inode;  //    a reference to an inode
    public final short iNumber;//    this inode number
    public int count;          //    a count to maintain #threads sharing this
    public final String mode;  //    "r", "w", "w+", or "a"
    public static int SEEK_SET = 0;
    public static int SEEK_CUR = 1;
    public static int SEEK_END = 2;
    
    FileTableEntry ( Inode i, short inumber, String m ) {
        seekPtr = 0;           // the seek pointer is set to the file top.
        inode = i;
        iNumber = inumber;     
        count = 1;           // at least one thread is using this entry.
        mode = m;            // once file access mode is set, it never changes.

        if ( mode.compareTo( "a" ) == 0 )
            seekPtr = inode.length;
    }
    
    public int setSeekPtr(int offset, int whence){
    	int success = -1;
    	if (whence == SEEK_SET){
    		//If whence is SEEK_SET (= 0), the file's seek pointer is set to offset bytes 
    		//from the beginning of the file
    		if (offset > inode.length){		//cannot go beyond file length
    			seekPtr = inode.length;
    		}
    		else if (offset < 0){		//seek pointer can't be negative
    			seekPtr = 0;
    		}
    		else{
    			seekPtr = offset;
    		}
    		success = 0;			//equal to Kernel.OK
    	}
    	else if (whence == SEEK_CUR){
    		//If whence is SEEK_CUR (= 1), the file's seek pointer is set to its current 
    		//value plus the offset. The offset can be positive or negative.
    		int setPtrTo = seekPtr + offset;		//set to offset bytes from the beginning of the file
    		if (setPtrTo > inode.length){		//cannot go beyond file
    			seekPtr = inode.length;
    		}
    		else if (setPtrTo < 0){		//seek pointer can't be negative
    			seekPtr = 0;
    		}
    		else{
    			seekPtr = setPtrTo;
    		}
    		success = 0;
    	}
    	else if (whence == SEEK_END){
    		//If whence is SEEK_END (= 2), the file's seek pointer is set to the size of the 
    		//file plus the offset. The offset can be positive or negative. 
    		int setPtrTo = inode.length + offset;
    		if (setPtrTo > inode.length){
    			seekPtr = inode.length;
    		}
    		else if(setPtrTo < 0){
    			seekPtr = 0;
    		}
    		else{
    			seekPtr = setPtrTo;
    		}
    		success = 0;
    	}
    	else{
    		success = -1; //equal to Kernel.ERROR
    	}
    	return success;
    }
}
