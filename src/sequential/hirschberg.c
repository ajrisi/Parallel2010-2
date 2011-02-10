#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "fileio.h"

#define MAX(x,y) ((x>y) ? x : y)

static void print_usage(void);

long long algb(long long m, long long n, char *A, char *B, int **LL)
{
	int **k;
	long long i;

	/*  k = (int*)calloc(n * 2, sizeof(int)); */
	k = (int**)calloc(2, sizeof(int*));
	k[0] = (int*)calloc(n+1, sizeof(int));
	k[1] = (int*)calloc(n+1, sizeof(int));

	*LL = (int*)calloc(n+1, sizeof(int));

	/* for each row */
	for(i = 0; i < m; i++) {

		/* rotate the 2nd row the to first */
		int j;
		for(j = 0; j <= n; j++) {
			k[0][j] = k[1][j];
		}

		/* calc the next row */
		for(j = 1; j <= n; j++) {
			if(A[i] == B[j-1]) {
				k[1][j] = k[0][j-1] + 1;
			} else {
				k[1][j] = MAX( k[1][j-1], k[0][j]);
			}
		}

	}

	/* load last row into LL */
	long long j;
	for(j = 0; j <= n; j++) {
		(*LL)[j] = k[1][j];
	}

	free(k[0]);
	free(k[1]);
	free(k);
	free(A);
	free(B);

	return n;
}

char *submat(char *A, long long b, long long e)
{
	long long sz;
	char *ret;
	int step = (e-b < 0) ? -1 : 1;
	long long i;

	if( (b < 0) ||
			(e < 0) ) {
		ret = calloc(sz, 1);
		return ret;
	}

	sz = ((e-b < 0) ? b-e : e-b) + 1;
	ret = calloc(sz+1, 1);
	ret[sz] = '\0';

	long long idx = 0;
	for(i = b; i != e+step; i += step) {
		ret[idx] = A[i];
		idx++;
	}

	return ret;
}

long long algc(long long m, long long n, char *A, char *B, char **C)
{
	long long i;
	long long j;
	int M = 0;
	long long k = 0;

	/* if the problem is trivial, then solve it */
	if(m == 0) {
		return 0;
	}

	if(m == 1) {
		/* if b's character is in a, then print it, else, no matches */
		long long i;
		for(i = 0; i < n; i++) {
			if(A[0] == B[i]) {
				*C = (char*)calloc(2, 1);
				**C = A[0];
				return 1;
			}
		}

		/* no matches */
		*C = (char*)calloc(1, 1);
		return 0;
	}

	/* otherwise */
	i = m / 2;

	/* determine split location */
	int *L1, *L2;
	long long L1_len, L2_len;
	L1_len = algb(i, n, submat(A, 0, i-1), submat(B, 0, n-1), &L1);
	L2_len = algb(m-i, n, submat(A, m-1, i), submat(B, n-1, 0), &L2);


	for(j = 0; j <= n; j++) {
		int cM;
		if( (cM = (L1[j] + L2[n-j])) > M) {
			M = cM;
			k = j;
		}
	}

	free(L1);
	free(L2);

	char *C1 = NULL;
	char *C2 = NULL;

	char *ta, *tb;

	long long parta = algc(i, k, ta = submat(A, 0, i-1), tb = submat(B, 0, k-1), &C1); 
	free(ta);
	free(tb);

	long long partb = algc(m-i, n-k, ta = submat(A, i, m-1), tb = submat(B, k, n-1), &C2);
	free(ta);
	free(tb);

	*C = calloc(parta + partb + 1, 1);
	memcpy(*C, C1, parta);
	memcpy(*C + parta, C2, partb);

	free(C1);
	free(C2);

	return parta+partb;
}


int main(int argc, char **argv)
{
	filemap source_a, source_b;

	if(argc != 3) {
		print_usage();
		return EXIT_FAILURE;
	}

	source_a = filemap_new(argv[1]);
	source_b = filemap_new(argv[2]);

	if(! source_a.valid_file) {
		fprintf(stderr, "Unable to read input file: %s\n", source_a.path);
		return EXIT_FAILURE;
	}

	if(! source_b.valid_file) {
		fprintf(stderr, "Unable to read input file: %s\n", source_b.path);
		return EXIT_FAILURE;
	}

	/**
	 * This is an implementation of hirschbergs quadratic time linear
	 * space "algorithm C"
	 * 
	 */
	char *total_out;
	long long total_len = algc(source_a.length, source_b.length,
			source_a.data, source_b.data, &total_out);

	printf("LCS length is %lld\n", total_len);

	long long i;
	for(i = 0; i < total_len; i++) {
		printf("0x%02x ", (unsigned char)total_out[i]);
		//printf("0x%02x(%c) ", (unsigned char)total_out[i], (unsigned char)total_out[i]);
	}

	printf("Done. \n");

	free(total_out);
	filemap_free(source_a);
	filemap_free(source_b);
	return EXIT_SUCCESS;
}


void print_usage()
{
	fprintf(stderr, "usage: hirschberg <file1> <file2>\n");
}
