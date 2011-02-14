# Required for regex grouping in clean rule to work
SHELL=bash


all: dynamic hirschberg recursive memo hirschberg_j

memo: memo.c hashtable.c fileio
	gcc -ggdb -pg -o memo memo.c hashtable.c fileio.o

recursive: recursive.c fileio
	gcc -ggdb -pg -o recursive recursive.c fileio.o

hirschberg: hirschberg.c fileio
	gcc -O3 -ggdb -pg -o hirschberg hirschberg.c fileio.o

dynamic: dynamic.c dynamic.h fileio
	gcc -ggdb -pg -o dynamic dynamic.c fileio.o

fileio: fileio.c fileio.h
	gcc -ggdb -c fileio.c

hirschberg_j: HirschbergLCS.java
	javac HirschbergLCS.java

clean:
	rm -f memo recursive hirschberg dynamic fileio 
	rm -rf {memo,recursive,hirschberg,dynamic}.dSYM
	rm -f *.class
