/******************************************************************************
* FileTableEntry.java
* Programmed by: Brittany Bentley, James Hurd, Thomas Petit
* Class: CSS430 - Operating Systems
* Quarter: Autumn 2014
* University of Washington, Bothell
*  
*
******************************************************************************/


public class FileTableEntry {  // Each table entry should have
    public int seekPtr;        //    a file seek pointer
    public final Inode inode;  //    a reference to an inode
    public final short iNumber;//    this inode number
    public int count;          //    a count to maintain #threads sharing this
    public final String mode;  //    "r", "w", "w+", or "a"
    public final static int SEEK_SET = 0;
    public final static int SEEK_CUR = 1;
    public final static int SEEK_END = 2;
    
    //constructor
    FileTableEntry ( Inode i, short inumber, String m ) {
        seekPtr = 0;           // the seek pointer is set to the file top.
        inode = i;
        iNumber = inumber;     
        count = 1;           // at least one thread is using this entry.
        mode = m;            // once file access mode is set, it never changes.

        if ( mode.compareTo( "a" ) == 0 )
            seekPtr = inode.length;
    }

    //Checks to see if there are any opens on the file
    public boolean isOpen() {
        return count > 0;
    }
    
    
    //sets the seek pointer starting at different
    //points at the file (whence) by a given
    //offset amount
    public int setSeekPtr(int offset, int whence){
        int whenceOffset;
        switch(whence) {
            case SEEK_SET:          //start the pointer from the beginning
                whenceOffset = 0;
                break;
            case SEEK_CUR:          //start the pointer from current location
                whenceOffset = this.seekPtr;
                break;
            case SEEK_END:          //start the pointer from the end
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
        
        return this.seekPtr;
    }

    //Returns the size of the file
    public int size(){
        return inode.length;
    }
}
