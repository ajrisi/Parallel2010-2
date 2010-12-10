#include "fileio.h"

#include <stdio.h>
#include <sys/mman.h>

long long file_size(char *path)
{
  struct stat st;

  if( stat(path, &st) ) {
    /* error with stat */
    return -1;
  }

  return st.st_size;
}

filemap filemap_new(char *path)
{
  filemap m;

  m.path = path;
  m.length = file_size(path);
  m.data = NULL;
  m.valid_file = 0;

  if(m.length < 0) {
    return m;
  }

  /* open the file */
  m.fd = open(path, O_RDONLY);
  if(m.fd < 0) {
    return m;
  }

  /* memory map the file */
  m.data = mmap((caddr_t)0, m.length, PROT_READ, MAP_PRIVATE, m.fd, 0);

  /* this is a valid file */
  m.valid_file = 1;
  return m;
}

void filemap_free(filemap m) 
{
  if(m.valid_file) {
    munmap(m.data, m.length);
  }
}

filemap filemap_new_sized(long long size) 
{
  filemap m;
  FILE *fp;

  m.length = size;
  m.valid_file = 0;
  /*
  fp = tmpfile();
  if(fp == NULL) {
    return m;
    }*/

  /* scan forward in fp to generate file of wanted size 
  if(fseek(fp, size, SEEK_SET)) {
    return m;
  }

  if(fwrite("", 1, 1, fp) != 1) {
    return m;
  }

  if(fseek(fp, 0, SEEK_SET)) {
    return m;
    }*/
  
  /*  m.fd = fileno(fp);*/
  m.data = mmap((caddr_t)0, m.length, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
  if(m.data == NULL) {
    fprintf(stderr, "Unable to generate table\n");
    return m;
  }

  /* this is a valid file */
  m.valid_file = 1;
  return m;
}
