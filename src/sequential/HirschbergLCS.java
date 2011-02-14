import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.io.File;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.io.IOException;

public class HirschbergLCS {

	private HirschbergLCS() { }

	public static void main(String[] args) throws Exception {
		MappedByteBuffer source_a, source_b;

		if(args.length != 2) {
			usage();
		}

		source_a = filemap_new(new String(args[0]));
		source_b = filemap_new(new String(args[1]));

		/**
		 * This is an implementation of hirschbergs quadratic time linear
		 * space "algorithm C"
		 * 
		 */
		byte[] lcs_str = algc(source_a.capacity(), source_b.capacity(), source_a, source_b);

		System.out.printf("LCS length is %d\n", lcs_str.length);

		for(int i = 0; i < lcs_str.length; i++) {
			System.out.printf("0x%02x ", lcs_str[i]);
		}

		System.out.printf("\nDone. \n");
	}

	private static byte[] algc(int m, int n, ByteBuffer A, ByteBuffer B){
		int M = 0;
		int k = 0;
		int i;

		/* if the problem is trivial, then solve it */
		if(m == 0) {
			return null;
		}

		if(m == 1) {
			/* if b's byteacter is in a, then print it, else, no matches */
			for(i = 0; i < n; i++) {
				if(A.get(0) == B.get(i)) {
					byte[] lcs = {A.get(0)};
					return lcs;
				}
			}

			/* no matches */
			return null;
		}

		/* otherwise */
		i = (int) m / 2;

		/* determine split location */
		int[] L1, L2;
		L1 = algb(i, n, submat(A, 0, i-1), submat(B, 0, n-1));
		L2 = algb(m-i, n, submat(A, m-1, i), submat(B, n-1, 0));

		for(int j = 0; j <= n; j++) {
			int cM;
			if( (cM = (L1[j] + L2[(int)n-j])) > M) {
				M = cM;
				k = j;
			}
		}

		byte[] C1;
		byte[] C2;
		C1 = algc(i, k, submat(A, 0, i-1), submat(B, 0, k-1)); 
		C2 = algc(m-i, n-k, submat(A, i, m-1), submat(B, k, n-1));
		
		int C1_len, C2_len;

		if (C1 == null){ C1_len = 0; } 
		else { C1_len = C1.length; }

		if (C2 == null) { C2_len = 0; } 
		else { C2_len = C2.length; }

		byte[] C = new byte[C1_len + C2_len];
		if (C1 != null) System.arraycopy(C1, 0, C, 0, C1_len);
		if (C2 != null) System.arraycopy(C2, 0, C, C1_len, C2_len);

		return C;
	}

	private static int[] algb(long m, long n, ByteBuffer A, ByteBuffer B){
		int[] LL = new int[(int)n+1];
		int[] tmp;
		int[] k0 = new int[(int)n+1];
		int[] k1 = new int[(int)n+1];

		/* for each row */
		for (int i = 0; i < m; i++) {
			/* rotate the 2nd row the to first */
			tmp = k0;
			k0 = k1;
			k1 = tmp;

			/* calc the next row */
			for (int j = 1; j <= n; j++) {
				if(A.get(i) == B.get(j-1)) {
					k1[j] = k0[j-1] + 1;
				} else {
					k1[j] = Math.max(k1[j-1], k0[j]);
				}
			}
		}

		/* load last row into LL */
		System.arraycopy(k1, 0, LL, 0, (int)n+1);

		return LL;
	}

	private static ByteBuffer submat(ByteBuffer A, int b, int e) {
		int sz = 0;
		byte[] ret;
		int step = (e-b < 0) ? -1 : 1;
		int i;

		if( (b < 0) || (e < 0) ) {
			return ByteBuffer.wrap(new byte[sz]);
		}

		sz = ((e-b < 0) ? b-e : e-b) + 1;
		ret = new byte[sz+1];
		ret[sz] = '\0';

		int idx = 0;
		for(i = b; i != e+step; i += step) {
			ret[idx] = A.get(i);
			idx++;
		}

		return ByteBuffer.wrap(ret);
	}

	private static MappedByteBuffer filemap_new(String filename) {
		MappedByteBuffer roBuf = null;

		try {
			// Create a read-only memory-mapped file
			File file = new File(filename);
			FileChannel roChannel = new RandomAccessFile(file, "r").getChannel();
			roBuf = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
		} catch (IOException e) {
			System.err.printf("Unable to read input file: %s\n", filename);
			System.exit(1);
		}

		return roBuf;
	}

	private static void usage() {
		System.err.printf("usage: HirschbergLCS <file1> <file2>\n");
		System.exit(1);
	}
}
