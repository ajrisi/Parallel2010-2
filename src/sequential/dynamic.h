#ifndef RECURSIVE_H
#define RECURSIVE_H

typedef struct LCS_table_s LCS_table;
struct LCS_table_s {
  int *table;
  long long width;
  long long height;
  int valid_table;
};

#endif /* RECURSIVE_H */
