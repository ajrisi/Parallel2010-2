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
import edu.rit.mp.buf.ByteItemBuf;
import edu.rit.mp.buf.ByteArrayBuf_1;
import edu.rit.mp.buf.ByteArrayBuf;
import edu.rit.util.LongRange;
import java.io.Serializable;
import java.nio.ByteBuffer;
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

public class DynamicClu 
{

    public static int BLOCK_SIZE = 5;

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
        source_a.load();
        source_b.load();
        
        //determine our column range
        Range r = new Range(0, source_b.capacity()-1).subrange(size, rank);

        int source_row = 0;
        int width = r.ub()-r.lb() + 2;
        int height = BLOCK_SIZE;
        final int global_height = source_b.capacity();
        
        //we need to store block_size rows
        byte[][] data = new byte[width][height];
        //all threads can fill the top with 0s initially
        for(int i = 0; i < width; i++) {
            data[i][0] = 0x00;
        }

        //the first slave can fill in its first column with 0s
        if(rank == 0) {
            for(int i = 0; i < height; i++) {
                data[0][i] = 0x00;
            }
        }

        //now, we start the main processing - if we are slave_rank 0, then
        //we process a block worth of data, if we are not slave_rank 0, we
        //recv a passing band, fill in our array, then compute a block
        //of data. At the end, we send our passing band (if we are not
        //slave_rank == size-1). Stop computation when row is
        //source_b.capacity()

        for(source_row = 0; source_row < global_height; source_row += (height-1)) {
            height = Math.min(height, global_height - source_row + 1);

            if(rank != 0) {
                //read passing band data and fill in our first column
                ByteArrayBuf passage_band = new ByteArrayBuf(data[0], new Range(0, height-1));
                world.receive(rank-1, source_row, passage_band);
                
                //if the passing band is short, then it should be our
                //new height (1 less), as we are probably at the end
                //if(passage_band.length() != height) {
                //    height = passage_band.length();
                //}

                //StringBuffer reportPB = new StringBuffer();
                //reportPB.append("Rank " + rank + " received passage band: [");
                //for(int i = 0; i < height; i++) {
                //    reportPB.append(data[0][i]);
                //    reportPB.append(" ");
                //}
                //reportPB.append("]\n");
                //System.out.println(reportPB);

            }

            //process a block worth of data
            for(int row = 1; row < height ; row++) { 
                for(int col = 1; col < width; col++) {
                    //                        System.out.println("data[" + col + "][" + row + "]= does [a" + (r.lb()+col-1) + "]=" + source_a.get(r.lb()+col-1) +
                    //                   " = [b" + (row+source_row-1) + "]=" + source_b.get(row+source_row-1) + 
                    //                   ", max of 0, " + (data[col][row-1]-2) + ", " + 
                    //                   (data[col-1][row] -2) + ", " + 
                    //                   (data[col-1][row-1] + (source_a.get(r.lb()+col-1) == source_b.get(row+source_row-1) ? +1 : -1)));
                    
                    //data[col][row] = (byte)Math.max((byte)0x00, 
                    //                                Math.max(data[col][row-1] - 2,
                    //                                         Math.max(data[col-1][row] - 2, 
                    //                                                  data[col-1][row-1] + (source_a.get(r.lb()+col-1) == source_b.get(row+source_row-1) ? +1 : -1))));

                    data[col][row] = (byte)Math.max(data[col][row-1], data[col-1][row]);
                    if(source_a.get(r.lb()+col-1) == source_b.get(row+source_row-1)) {
                        data[col][row] = (byte)Math.max(data[col][row], data[col-1][row-1] + 1);
                    }

                    //if(data[col][row] > best_intermediate_score) {
                    //found new best score and coord, put it into the phase 1 result
                    //    best_intermediate_score = data[col][row];
                    //res = new Phase1Result(best_intermediate_score, new HashSet());
                    //    res.coords.add(new Coord(r.lb()+col-1, source_row+row-1));
                    //} //else {
                        //if the found score is equal to the best score, then add its coord to the result
                    // if(data[col][row] == best_intermediate_score) {
                    //       res.coords.add(new Coord(r.lb()+col-1, source_row+row-1));
                    //   }
                    //}
                    
                }
            }

            
            //show the data array
            StringBuffer sb = new StringBuffer();
            sb.append("Rank " + rank + " has data:\n[" + rank + "]");
            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    sb.append("" + data[j][i] + " ");
                }
                sb.append("\n[" + rank + "]");
            }
            System.out.println(sb);
                
            //send the passage band
            if(rank != (size-1)) {
                //StringBuffer reportPB = new StringBuffer();
                ByteArrayBuf passage_band = new ByteArrayBuf(data[width-1], new Range(0, height-1));
                //reportPB.append("Rank " + rank + " sending passage band: [");
                for(int i = 0; i < height; i++) {
                    passage_band.put(i, data[width-1][i]);
                    // reportPB.append(data[width-1][i]);
                    // reportPB.append(" ");
                }

                //reportPB.append("]\n");
                
                //System.out.println(reportPB);
                //send the passage band
                world.send(rank+1, source_row, passage_band); 
            }

            //prep for next iteration
            for(int i = 0; i < width; i++) {
                data[i][0] = data[i][height-1];
            }

             
        } //end of all parallel processing                                    

        if(rank == (size-1)) {
            System.out.println("LCS is " + data[width-1][height-1] + " long");
        }
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