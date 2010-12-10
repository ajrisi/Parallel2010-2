#include <stdio.h>
#include <stdlib.h>
#include "fileio.h"
#include "dynamic.h"

#define MAX(x,y) ((x>y) ? x : y)

#define LCS_table_set_M(t, x, y, val)  t.table[t.width * (y) + (x)] = val

#define LCS_table_at_M(t, x, y) ((((x)<0)||((y)<0)) ? 0 : t.table[t.width * (y) + (x)])

void recursive_print_LCS(LCS_table table, filemap source_a, long long i, long long j)
{
  int current_val;
  if((i < 0) || (j < 0)) {
    return;
  }

  current_val = LCS_table_at_M(table, i, j);

  if(current_val == (LCS_table_at_M(table, i-1, j-1)+1)) {
    recursive_print_LCS(table, source_a, i-1, j-1);
    printf("0x%02x(%c) ", (unsigned char)source_a.data[i], (unsigned char)source_a.data[i]);
  } else if (current_val == LCS_table_at_M(table, i, j-1)) {
    recursive_print_LCS(table, source_a, i-1, j);
  } else {
    recursive_print_LCS(table, source_a, i, j-1);
  }
}

LCS_table build_table(filemap a, filemap b) {
  /* create a new table object, create the table iteratively, in
     order, and make sure you never access a negative value in the
     table */
  LCS_table t;
  long long i, j;

  t.valid_table = 0;

  /*
  if( (! a.valid_file) ||
      (! b.valid_file) ) {
    return t;
    }*/

  t.table = (int *)malloc(sizeof(int) * b.length * a.length);
  if(t.table == NULL) {
    fprintf(stderr, "Unable to generate table\n");
    return t;
  }
  
  /* a across the top, b down the side */
  t.width = a.length;
  t.height = b.length;
  t.valid_table = 1;

  /* now, do the actual operation, where the algorithm works as
     follows: to fill a location in the table, get the data in a and
     the data in b for that location - if they are the same, then that
     location is the diagonal +1, if they are different, then that
     location is the max of the space above and the space before in
     the table */
  for(j = 0; j < b.length; j++) {
    for(i = 0; i < a.length; i++) {
      if(a.data[i] == b.data[j]) {
	/* match, diag +1 */
	LCS_table_set_M(t, i, j, LCS_table_at_M(t, i-1, j-1) + 1);
      } else {
	/* no match, max of ^ and <- */
	int up, left;
	up = LCS_table_at_M(t, i, j-1);
	left = LCS_table_at_M(t, i-1, j);
	LCS_table_set_M(t, i, j, MAX(up, left));
      }
    }
  }

  return t;
}

void free_table(LCS_table t) 
{
  if(t.valid_table) {
    free(t.table);
  }
}

static void print_usage()
{
  fprintf(stderr, "usage: dynamic <file> <file>");
}

void print_table(LCS_table tab, filemap a, filemap b)
{
  long long i, j;

  printf(" ");
  for(j = -1; j < tab.height; j++) {
    for(i = -1; i < tab.width; i++) {
      if(j < 0) printf("%c ", a.data[i]);
      else if (i < 0) printf("%c ", b.data[j]);
      else printf("%d ", LCS_table_at_M(tab, i, j));
    }
    printf("\n");
  }
}

int main(int argc, char **argv)
{
  /* read in two file addresses - load each file into memory - build
     table (m*n) - fill table, walk table */
  filemap source_a, source_b;
  LCS_table table;

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

  /* build the table for the two inputs */
  table = build_table(source_a, source_b);
  if(! table.valid_table) {
    fprintf(stderr, "Error, unable to create a valid table\n");
    exit(1);
  }

  /*  print_table(table, source_a, source_b);*/
  printf("LCS length is %d\n", LCS_table_at_M(table, table.width-1, table.height-1));

  /* print the longest subsequence */
  recursive_print_LCS(table, source_a, table.width-1, table.height-1);

  printf("Done.\n");
  filemap_free(source_a);
  filemap_free(source_b);
  free_table(table);
  return EXIT_SUCCESS;
}
