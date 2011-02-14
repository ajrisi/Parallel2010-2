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

public class HirschbergModClu 
{

    public static int BLOCK_SIZE = 5;

    //timing
    static long startTime;
    static long endTime;

    //cluster data
    static Comm world;
    static int size;
    static int rank;

    static MappedByteBuffer source_a, source_b;

    //for storing results of SW Phase 1
    static Phase1Result phase1Result;
        
    //best score
    static IntegerItemBuf best_score;

    static ObjectItemBuf<Phase1Result>[] phase1Results;

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

        if(size < 2) {
            if(rank == 0) System.err.println("Parallel Hirschberg requires atleast a master and a slave node in the cluster\n");
        }
	
	//read in the two files to compare
	source_a = filemap_new(new String(args[0]));
	source_b = filemap_new(new String(args[1]));

        //make sure they are loaded
        source_a.load();
        source_b.load();
        
        best_score = new IntegerItemBuf(-1);
        phase1Results = (ObjectItemBuf<Phase1Result>[])new ObjectItemBuf[size];
        for(int i = 0; i < size; i++) phase1Results[i] = new ObjectItemBuf();

        if(rank == 0) {
            System.out.println("source_a is " + source_a.capacity() + " long");
            System.out.println("source_b is " + source_b.capacity() + " long");
        }

	//split operation, master goes one way, slaves go another
	if(rank == 0) {
            //set the masters phase1Result to a negative score
            phase1Result = new Phase1Result(-1, null);

	    //for each slave {
	    //recv score and coords from slave
	    //if best_score < score then save score ans best score, and coords as the list of coordinates
	    //else, if the best score is equal to the recvd score, then add coords to local coords list
	    //}

            //create array of Phase1Results and do gather
            world.gather(0, new ObjectItemBuf<Phase1Result>(phase1Result), phase1Results);

            //walk over the phase1results, aggregate the best score and coords list
            for(int i = 1; i < size; i++) {
                if(phase1Results[i].item.score > phase1Result.score) {
                    phase1Result = phase1Results[i].item;
                } else {
                    if(phase1Results[i].item.score == phase1Result.score) {
                        phase1Result.coords.addAll(phase1Results[i].item.coords);
                    }
                }
            }
            
	    //broadcast best score to all slaves
            best_score = new IntegerItemBuf(phase1Result.score);
            world.broadcast(0, best_score);
           
            System.out.println("[" + rank + "/" + size + "] best_score = " + best_score.item);
            System.out.print("Best score coords: ");
            Iterator it = phase1Result.coords.iterator();
            while(it.hasNext()) {
                System.out.print(it.next());
            }
            System.out.println("");

            
	    //for each coord in coords, recv job request from slave
	    //send slave a coord from coords
	    //end for

	    //send job end to all slaves, to indicate they can die
	    
	} else {
            int column0 = source_a.capacity()*(rank-1)/(size-1);
            int column1 = source_b.capacity()*(rank+1-1)/(size-1) -1;

            //System.out.println("Slave " + rank + " of " + size + ", range is " + column0 + " to " + column1);

	    //calculate score, coords with smith_waterman phase 1, with column0, column1
            phase1Result = smith_waterman_phase_1(source_a, source_b, column0, column1);
            
            //send score,coords to master
            world.gather(0, new ObjectItemBuf(phase1Result), null);

	    //recv best score from master
            world.broadcast(0, best_score);
                             
            //System.out.println("[" + rank + "/" + size + "] best_score = " + best_score.item);

	    //loop
	    //ask for job from master
	    //if job is end, then break look
	    //minx, miny gets SWphase2step1(jobx, joby, best_score)
	    //store in NFS
	    //smith watermal phase2step2(minx, miny, jobx, joby, bestscore
	    //end loop

	}
	
    }
    
    private static Phase1Result smith_waterman_phase_1(MappedByteBuffer source_a, MappedByteBuffer source_b, int column0, int column1) 
        throws Exception {
        //The essential algorithm is for the machine to calculate a
        //block of values (in this case, BLOCK_SIZE) after it receives
        //the passage band information from its neighbor - if the slave_rank
        //is 0, then there is no wait to recv passage band info, if
        //the slave_rank is size-1, then there is no sending passage band
        //info.
        
        int source_row = 0;
        Phase1Result res = new Phase1Result(0, new HashSet());
        int best_intermediate_score = 0;
        int width = column1-column0 + 2;
        int height = BLOCK_SIZE;
        final int global_height = source_b.capacity();
        
        //we need to store block_size rows
        byte[][] data = new byte[width][height];
        //all threads can fill the top with 0s initially
        for(int i = 0; i < width; i++) {
            data[i][0] = 0x00;
        }

        //the first slave can fill in its first column with 0s
        if(rank == 1) {
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

            if(rank != 1) {
                //read passing band data and fill in our first column
                ByteArrayBuf passage_band = new ByteArrayBuf(data[0], new Range(0, height-1));
                //StringBuffer reportPB = new StringBuffer();
                world.receive(rank-1, passage_band);
                
                //if the passing band is short, then it should be our
                //new height (1 less), as we are probably at the end
                if(passage_band.length() != height) {
                    height = passage_band.length();
                }

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
                    //                        System.out.println("data[" + col + "][" + row + "]= does [a" + (column0+col-1) + "]=" + source_a.get(column0+col-1) +
                    //                   " = [b" + (row+source_row-1) + "]=" + source_b.get(row+source_row-1) + 
                    //                   ", max of 0, " + (data[col][row-1]-2) + ", " + 
                    //                   (data[col-1][row] -2) + ", " + 
                    //                   (data[col-1][row-1] + (source_a.get(column0+col-1) == source_b.get(row+source_row-1) ? +1 : -1)));
                    
                    data[col][row] = (byte)Math.max((byte)0x00, 
                                                    Math.max(data[col][row-1] - 2,
                                                             Math.max(data[col-1][row] - 2, 
                                                                      data[col-1][row-1] + (source_a.get(column0+col-1) == source_b.get(row+source_row-1) ? +1 : -1))));
                    if(data[col][row] > best_intermediate_score) {
                        //found new best score and coord, put it into the phase 1 result
                        best_intermediate_score = data[col][row];
                        res = new Phase1Result(best_intermediate_score, new HashSet());
                        res.coords.add(new Coord(column0+col-1, source_row+row-1));
                    } else {
                        //if the found score is equal to the best score, then add its coord to the result
                        if(data[col][row] == best_intermediate_score) {
                            res.coords.add(new Coord(column0+col-1, source_row+row-1));
                        }
                    }
                    
                }
            }

            
            //show the data array
            //StringBuffer sb = new StringBuffer();
            //sb.append("Rank " + rank + " has data:\n[" + rank + "]");
            //for(int i = 0; i < height; i++) {
            //    for(int j = 0; j < width; j++) {
            //        sb.append("" + data[j][i] + " ");
            //    }
            //    sb.append("\n[" + rank + "]");
            //}
            //System.out.println(sb);
                
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
                world.send(rank+1, passage_band); 
            }

            //prep for next iteration
            for(int i = 0; i < width; i++) {
                data[i][0] = data[i][height-1];
            }

             
        } //end of all parallel processing                                    

        return res;
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

    static class Phase1Result
        implements Serializable
    {
        public int score;
        public HashSet coords;
        public Phase1Result(int _score, HashSet _coords) {
            score = _score;
            coords = _coords;
        }
    }

    static class Job
        implements Serializable
    {
        public Coord coord;
        public Job(Coord _coord) {
            coord = _coord;
        }
    }

    static class Coord
        implements Serializable
    {
        public int x;
        public int y;
        public Coord(int _x, int _y) {
            x = _x;
            y = _y;
        }

        public String toString() {
            return new String("[" + x + ", " + y + "]");
        }
    }

}