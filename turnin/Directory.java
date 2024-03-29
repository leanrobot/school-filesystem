/******************************************************************************
* Directory.java
* Programmed by: Brittany Bentley, James Hurd, Thomas Petit
* Class: CSS430 - Operating Systems
* Quarter: Autumn 2014
* University of Washington, Bothell
*  
*
******************************************************************************/


public class Directory {
      private static int maxChars = 30; // max characters of each file name

      // Directory entries
      private int fsize[];        // each element stores a different file size.
      private char fnames[][];    // each element stores a different file name.

      //constructor
      public Directory( int maxInumber ) { // directory constructor
         fsize = new int[maxInumber];     // maxInumber = max files
         for ( int i = 0; i < maxInumber; i++ ) 
             fsize[i] = 0;                 // all file size initialized to 0
         fnames = new char[maxInumber][maxChars];
         String root = "/";                // entry(inode) 0 is "/"
         fsize[0] = root.length( );        // fsize[0] is the size of "/".
         root.getChars( 0, fsize[0], fnames[0], 0 ); // fnames[0] includes "/"
      }

      // converts plain byte array info into a directory, the byte array is read from disk
      public void bytes2directory( byte[] data ) {
         // assumes data[] received directory information from disk
         // initializes the Directory instance with this data[]

         int offset = 0;
         // initailize fsize[]
         for (int i = 0; i < fsize.length; i ++) {
            fsize[i] = SysLib.bytes2int(data, offset);
            offset += 4;
         }
         // intialize fnames[]
         for (int i = 0; i < fnames.length; i++) {
            String temp = new String (data, offset, maxChars * 2);
            temp.getChars(0, fsize[i], fnames[i], 0);
            offset += maxChars * 2;
         }
      }

      //Converts directory information into a plain byte array to be written back to the disk
      public byte[] directory2bytes( ) {
         // converts and return Directory information into a plain byte array
         // this byte array will be written back to disk
         // note: only meaningfull directory information should be converted
         // into bytes.
         int dataSize = (fsize.length * 4) + (fnames.length * maxChars * 2);
         byte[] data = new byte[dataSize];

         int offset = 0;
         for (int i = 0; i < fsize.length; i ++) {
            SysLib.int2bytes(fsize[i], data, offset);
            offset += 4;
         }

         // construct strings to be converted to bytes, copy bytes to data[]
         for (int i = 0; i < fnames.length; i++) {
            String temp = new String(fnames[i], 0, fsize[i]);
            byte[] bytes = temp.getBytes();
            // copies the entirety of bytes to data starting at offset. 
            System.arraycopy(bytes, 0, data, offset, bytes.length);
            offset += maxChars * 2;
         }

         return data;
      }

      // filename is the one of a file to be created.
      // allocates a new inode number for this filename
      public short ialloc( String filename ) {
         for (short i = 1; i < fsize.length; i++) {
            if(fsize[i] == 0) {
               fsize[i] = Math.min(filename.length(), maxChars);
               filename.getChars(0, fsize[i], fnames[i], 0);
               return i;
            }
         }
         // no available iNode found.
         return Kernel.ERROR;
      }

      // deallocates this inumber (inode number)
      // the corresponding file will be deleted.
      public boolean ifree( short iNumber ) {
         if (fsize[iNumber] > 0){
            fsize[iNumber] = 0;
            
            //delete filename
            for(int i =0; i < maxChars; i++){
               fnames[iNumber][i] = '\0';
            }
            
            return true;
         }
         // not needed to delete
         return false;
      }

      // returns the inumber corresponding to this filename
      public short namei( String filename ) {
         for(short i = 0; i < fsize.length; i++) {
            String temp = new String(fnames[i], 0, fsize[i]);
            if (filename.compareTo(temp) == 0) {
               return i;
            }
         }

         // no available iNode found.
         return Kernel.ERROR;
      }
   }