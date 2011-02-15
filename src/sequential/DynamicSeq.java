import edu.rit.util.Range;
import java.util.Iterator;
import java.lang.StringBuffer;
import edu.rit.pj.Comm;
import java.lang.Byte;
import edu.rit.pj.reduction.LongOp;
import edu.rit.pj.reduction.LongOp;
import edu.rit.mp.buf.IntegerItemBuf;
import edu.rit.mp.buf.ObjectItemBuf;
import edu.rit.pj.CommRequest;
import edu.rit.mp.buf.LongItemBuf;
import edu.rit.mp.buf.IntegerItemBuf;
import edu.rit.mp.buf.IntegerArrayBuf_1;
import edu.rit.mp.buf.IntegerArrayBuf;
import edu.rit.util.LongRange;
import java.io.Serializable;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.io.File;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import edu.rit.util.Range;

public class DynamicSeq
{

    public static int BLOCK_SIZE = 2;

    //timing
    static long startTime;
    static long endTime;

    //cluster data
    static Comm world;
    static int size;
    static int rank;

    //for storing the input files
    static MappedByteBuffer source_a, source_b;

    public static void main(String[] args)
	throws Exception
    {
	//capture the start time
	startTime = System.currentTimeMillis();

        // Initialize world communicator.
        Comm.init (args);
        world = Comm.world();
        size = world.size();
        rank = world.rank();

	//read in the two files to compare
	source_a = filemap_new(new String(args[0]));
	source_b = filemap_new(new String(args[1]));

        //make sure they are loaded
        //source_a.load();
        //source_b.load();
        
        int source_row = 0;
        Range r = new Range(0, source_a.capacity()-1);
        int width = r.ub()-r.lb() + 2;
        int height = BLOCK_SIZE;
        final int global_height = source_b.capacity();
        
        //we need to store block_size rows
        int[][] data = new int[width][height];
        //all threads can fill the top with 0s initially
        for(int i = 0; i < width; i++) {
            data[i][0] = 0x00;
        }

        for(int i = 0; i < height; i++) {
            data[0][i] = 0x00;
        }
        

        //now, we start the main processing - if we are slave_rank 0, then
        //we process a block worth of data, if we are not slave_rank 0, we
        //recv a passing band, fill in our array, then compute a block
        //of data. At the end, we send our passing band (if we are not
        //slave_rank == size-1). Stop computation when row is
        //source_b.capacity()

        for(source_row = 0; source_row < global_height; source_row += (height-1)) {
            height = Math.min(height, global_height - source_row + 1);

            //process a block worth of data
            for(int row = 1; row < height ; row++) { 
                for(int col = 1; col < width; col++) {

                    data[col][row] = Math.max(data[col][row-1], data[col-1][row]);
                    if(source_a.get(r.lb()+col-1) == source_b.get(row+source_row-1)) {
                        data[col][row] = data[col-1][row-1]  + 1;
                    }
                    
                }
            }
            
            //prep for next iteration
            for(int i = 0; i < width; i++) {
                data[i][0] = data[i][height-1];
            }

             
        }

        endTime = System.currentTimeMillis();
        System.out.println("LCS is " + data[width-1][height-1] + " long, " + (endTime-startTime) + " mills");
        
    }

    private static MappedByteBuffer filemap_new(String filename) {
	MappedByteBuffer roBuf = null;
	
	try {
	    File file = new File(filename);
	    
	    // Create a read-only memory-mapped file
	    FileChannel roChannel = new RandomAccessFile(file, "r").getChannel();
	    roBuf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
	} catch (IOException e) {
	    System.err.println("Unable to read input file: " + filename);
	    System.exit(1);
	}
	
	return roBuf;
    }

}