public class FileTableEntry {  // Each table entry should have
    public int seekPtr;        //    a file seek pointer
    public final Inode inode;  //    a reference to an inode
    public final short iNumber;//    this inode number
    public int count;          //    a count to maintain #threads sharing this
    public final String mode;  //    "r", "w", "w+", or "a"
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    
    FileTableEntry ( Inode i, short inumber, String m ) {
        seekPtr = 0;           // the seek pointer is set to the file top.
        inode = i;
        iNumber = inumber;     
        count = 1;           // at least one thread is using this entry.
        mode = m;            // once file access mode is set, it never changes.

        if ( mode.compareTo( "a" ) == 0 )
            seekPtr = inode.length;
    }

    public boolean isOpen() {
        return count > 0;
    }
    
    public int setSeekPtr(int offset, int whence){
        int whenceOffset;
        switch(whence) {
            case SEEK_SET:
                whenceOffset = 0;
                break;
            case SEEK_CUR:
                whenceOffset = this.seekPtr;
                break;
            case SEEK_END:
                whenceOffset = inode.length;
                break;
            default:
                return Kernel.ERROR;
        }
        int setPtrTo = whenceOffset + offset;
        if (setPtrTo > inode.length){
            this.seekPtr = inode.length;
        }
        else if(setPtrTo < 0){
            this.seekPtr = 0;
        }
        else {
            this.seekPtr = setPtrTo;
        }
    	
    	return Kernel.OK;
    }

    public int size(){
    	return inode.length;
    }
}
