#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include "fileio.h"
#include "hashtable.h"

#define MAX3(A, B, C) ( ((A) > (B)) ? ( (A) > (C) ? A : C ) : ( (B) > (C) ? B : C ) )

void print_usage();
long long LCS(char *A, long long lena, char *B, long long lenb, char **out, long long number_matched);

hashtable *memotable;

typedef struct memotable_entry_s memotable_entry;
struct memotable_entry_s {
  long long lena;
  long long lenb;
  char *lcs;
  long long lcs_len;
};

unsigned long mt_hash(void *mt_entry)
{
  memotable_entry *mte;
  mte = (memotable_entry*)mt_entry;
  char str[100] = {0};
  sprintf(str, "%lld:%lld", mte->lena, mte->lenb);
  return hshstrhash(str);
}

unsigned long mt_rehash(void *mt_entry)
{
  memotable_entry *mte;
  mte = (memotable_entry*)mt_entry;
  char str[100] = {0};
  sprintf(str, "%lld:%lld", mte->lena, mte->lenb);
  return hshstrehash(str);
}

int mt_cmp(void *rt, void *lt)
{
  memotable_entry *mrt, *mlt;
  mrt = (memotable_entry*)rt;
  mlt = (memotable_entry*)lt;
  return !( (mrt->lena == mlt->lena) &&
	    (mrt->lenb == mlt->lenb) );
}

void *mt_dup(void *mt_entry)
{
  memotable_entry *mte;
  memotable_entry *ret = malloc(sizeof(memotable_entry));
  mte = (memotable_entry*)mt_entry;

  ret->lena = mte->lena;
  ret->lenb = mte->lenb;
  ret->lcs_len = mte->lcs_len;
  ret->lcs = calloc(ret->lcs_len + 1, 1);
  memcpy(ret->lcs, mte->lcs, mte->lcs_len);
  return ret;
}

void mt_free(void *mt_entry)
{
  memotable_entry *mte;
  mte = (memotable_entry*)mt_entry;
  free(mte->lcs);
  free(mte);
}

long long LCS_memo(long long lena, long long lenb, char **out)
{
  /* lookup lena:lenb in the hashtable, if its there, dup and set out,
     return len */
  memotable_entry ent = {lena, lenb, NULL, 0};
  memotable_entry *ret = NULL;
  ret = (memotable_entry*)hashtable_find(memotable, &ent);
  if(ret == NULL) {
    return -1;
  } 

  *out = ret->lcs;
  return ret->lcs_len;
}

void LCS_memo_set(long long lena, long long lenb, char *lcs, long long lcs_len)
{
  memotable_entry ent = {lena, lenb, lcs, lcs_len};
  hashtable_insert(memotable, &ent);
}

long long LCSm(char *A, long long lena, char *B, long long lenb, char **out, long long number_matched)
{
  long long memolen;
  char *memo = NULL;
  
  
  if((memolen = LCS_memo(lena, lenb, &memo)) != -1) {
    /* able to do a memo table lookup, use this instead */
    *out = realloc(*out, number_matched + memolen);
    memcpy(*out+number_matched, memo, memolen);
    
    return number_matched + memolen;
  }
  
  long long len = LCS(A, lena, B, lenb, out, number_matched);
  LCS_memo_set(lena, lenb, *out+number_matched, len-number_matched);
  return len;
}

long long LCS(char *A, long long lena, char *B, long long lenb, char **out, long long number_matched)
{
  long long solna, solnb, solnc, longest;
  char *memo;
  long long memolen;
  
  if( (lena == 0) ||
      (lenb == 0) ) {
    /* no more possible matches, this is the end of this branch */
    return number_matched;
  }

  if(B[lenb-1] == A[lena-1]) {
    /* match the ending character of both sequences */
    number_matched++;
    *out = realloc(*out, number_matched+1);
    (*out)[number_matched-1] = A[lena-1];
    (*out)[number_matched] = '\0';

    return LCSm(A, lena-1, B, lenb-1, out, number_matched);
  }

  /* duplicate what has been matched so far, one for each branch */
  char *left = memcpy(calloc(number_matched+1, 1), *out, number_matched);
  char *right = memcpy(calloc(number_matched+1, 1), *out, number_matched);

  solna = LCSm(A, lena-1, B, lenb, &left, number_matched);
  solnb = LCSm(A, lena, B, lenb-1, &right, number_matched);

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

  memotable = hashtable_new(mt_hash, mt_rehash, mt_cmp, mt_dup, mt_free);
  
  total_len = LCSm(source_a.data, source_a.length, source_b.data, source_b.length, &total_out, 0);

  printf("LCS length is %lld\n", total_len);

  for(i = total_len-1; i >= 0; --i) {
    printf("0x%02x(%c) ", (unsigned char)total_out[i], total_out[i]);
  }

  free(total_out);
  hashtable_free(memotable);
  printf("Done. \n");

  return 0;
}

void print_usage()
{
  fprintf(stderr, "usage: recursive <file1> <file2>\n");
}
