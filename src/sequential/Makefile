all: dynamic hirschberg recursive memo

memo: memo.c hashtable.c fileio
	gcc -ggdb -pg -o memo memo.c hashtable.c fileio.o

recursive: recursive.c fileio
	gcc -ggdb -pg -o recursive recursive.c fileio.o

hirschberg: hirschberg.c fileio
	gcc -ggdb -pg -o hirschberg hirschberg.c fileio.o

dynamic: dynamic.c dynamic.h fileio
	gcc -ggdb -pg -o dynamic dynamic.c fileio.o

fileio: fileio.c fileio.h
	gcc -ggdb -c fileio.c