import java.util.Vector;

public class FileTable {

      private FileSystem fs;        // pointer to the filesystem singleton.
      private Vector<FileTableEntry> table;         // the actual entity of this file table
      private Directory dir;        // the root directory

      public FileTable(FileSystem fs, Directory directory ) { // constructor
         table = new Vector<FileTableEntry>();     // instantiate a file (structure) table
         dir = directory;           // receive a reference to the Director
         this.fs = fs;
      }                             // from the file system

      // major public methods
      public synchronized FileTableEntry falloc( String filename, String mode ) {
         short iNumber = dir.namei(filename);
         if(SysLib.isError(iNumber)) {
            iNumber = dir.ialloc(filename);
         }
         Inode node = fs.getInode(iNumber);
         
         FileTableEntry fte = new FileTableEntry(node, iNumber, mode);
         synchronized(node) {
            node.count++;
         }
         table.add(fte);
         return fte;
         
         // allocate a new file (structure) table entry for this file name
         // allocate/retrieve and register the corresponding inode using dir
         // increment this inode's count
         // immediately write back this inode to the disk
         // return a reference to this file (structure) table entry
      }

      public synchronized boolean ffree( FileTableEntry e ) {
         synchronized(e.inode) {
            e.inode.count--;
            e.inode.toDisk(e.iNumber);
         }
         return table.remove(e);

         // receive a file table entry reference
         // save the corresponding inode to the disk
         // free this file table entry.
         // return true if this file table entry found in my table
      }

      public synchronized boolean fempty( ) {
         for(FileTableEntry fte : table) {
            ffree(fte);
         }
         return table.isEmpty( );  // return if table is empty
      }
   }