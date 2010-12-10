#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include "fileio.h"
#include "recursive.h"

#define MAX3(A, B, C) ( ((A) > (B)) ? ( (A) > (C) ? A : C ) : ( (B) > (C) ? B : C ) )

void print_usage();

long long LCS(char *A, long long lena, char *B, long long lenb, char **out, long long number_matched)
{
  long long solna, solnb, solnc, longest;
  char *pref_a, *pref_b;

  if( (lena == 0) ||
      (lenb == 0) ) {
    /* no more possible matches, this is the end of this branch */
    return number_matched;
  }

  if(B[lenb-1] == A[lena-1]) {
    /* match the ending character of both sequences */
    *out = realloc(*out, number_matched+2);
    (*out)[number_matched] = A[lena-1];
    (*out)[number_matched+1] = '\0';

    return LCS(A, lena-1, B, lenb-1, out, number_matched+1);
  }

  /* duplicate what has been matched so far, one for each branch */
  char *left = memcpy(calloc(number_matched+1, 1), *out, number_matched);
  char *right = memcpy(calloc(number_matched+1, 1), *out, number_matched);

  solna = LCS(A, lena-1, B, lenb, &left, number_matched);
  solnb = LCS(A, lena, B, lenb-1, &right, number_matched);

  if(solna > solnb) {
    free(*out);
    free(right);
    *out = left;
    return solna;
  } else {
    free(*out);
    free(left);
    *out = right;
    return solnb;
  }

}

int main(int argc, char **argv) 
{
  filemap source_a, source_b;
  char *total_out = NULL;
  long long total_len;
  long long i;

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
  
  total_len = LCS(source_a.data, source_a.length, source_b.data, source_b.length, &total_out, 0);

  printf("LCS length is %lld\n", total_len);

  for(i = total_len-1; i >= 0; --i) {
    printf("0x%02x(%c) ", (unsigned char)total_out[i], total_out[i]);
  }

  free(total_out);
  printf("Done. \n");

  return 0;
}

void print_usage()
{
  fprintf(stderr, "usage: recursive <file1> <file2>\n");
}
