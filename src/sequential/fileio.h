#ifndef FILEIO_H
#define FILEIO_H

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#ifdef __APPLE_CC__
#define MAP_ANONYMOUS MAP_ANON
#endif

typedef struct filemap_s filemap;
struct filemap_s {
  char *path;
  long long length;
  char *data;
  int valid_file;
  int fd;
};

long long file_size(char *path);

filemap filemap_new(char *path);

void filemap_free(filemap m);

filemap filemap_new_sized(long long size);

#endif /* FILEIO_H */
