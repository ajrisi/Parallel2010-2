import edu.rit.pj.Comm;
import java.lang.Byte;
import edu.rit.pj.reduction.LongOp;
import edu.rit.pj.CommRequest;
import edu.rit.mp.buf.LongItemBuf;
import edu.rit.mp.buf.ByteItemBuf;
import edu.rit.util.LongRange;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class HirschbergModClu 
{

    //cluster data
    static Comm world;
    static int size;
    static int rank;

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
	
	/*	if(args.length != 2) {
	    usage();
	    }*/
	
	//read in the two files to compare
	source_a = filemap_new(new String(args[0]));
	source_b = filemap_new(new String(args[1]));

	//split operation, master goes one way, slaves go another
	if(rank == 0) {
	    //master
	    //for each slave {
	    //recv score and coords from slave
	    //if best_score < score then save score ans best score, and coords as the list of coordinates
	    //else, if the best score is equal to the recvd score, then add coords to local coords list
	    //}

	    //broadcast best score to all slaves
	    //for each coord in coords, recv job request from slave
	    //send slave a coord from coords
	    //end for

	    //send job end to all slaves, to indicate they can die
	    
	} else {
	    System.out.println("Slave " + rank + " of " + size + " recvd files of size " + source_a.
	    //slave

	    //column0 gets seq0*myrank/comm_size + 1
	    //column0 gets seq0*myrank+1/comm_size

	    //calculate score, coords with smith_waterman phase 1, with column0, column1
	    //send score,coords to master
	    //recv best score from master
	    //loop
	    //ask for job from master
	    //if job is end, then break look
	    //minx, miny gets SWphase2step1(jobx, joby, best_score)
	    //store in NFS
	    //smith watermal phase2step2(minx, miny, jobx, joby, bestscore
	    //end loop

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
	    System.err.printf("Unable to read input file: %s\n", filename);
	    System.exit(1);
	}
	
	return roBuf;
    }

}