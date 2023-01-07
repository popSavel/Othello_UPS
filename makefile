CC=gcc

all:	clean  server

server: server.c
	${CC} -pthread -o server server.c

clean: 
	rm -f server
