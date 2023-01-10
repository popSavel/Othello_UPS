#include <stdio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <pthread.h>
#include <time.h>
#include <errno.h>
#include <arpa/inet.h>

#define BUFF_SIZE 100
#define PREFIX_LENGTH 6
#define SERVER_CAPACITY 100
#define NICK_LENGTH 10
#define DATA_BLOCK 100
#define BOARD_SIZE 64
#define DEFAULT_IP "127.0.0.1"
#define DEFAULT_PORT 9999

char* players[SERVER_CAPACITY];
char* queue[SERVER_CAPACITY];
char* pl_in_game[SERVER_CAPACITY];
char* disc_players[SERVER_CAPACITY];
int games[SERVER_CAPACITY][BOARD_SIZE];
int players_in_queue;
int start_of_queue;
int sockets[SERVER_CAPACITY];
int pings[SERVER_CAPACITY];
int client_count;
int players_in_game;
int disconnected_players;

FILE* game;

void die(const char* chr) {
	printf("ERROR: %s\r\n", chr);
	exit(1);
}


typedef struct client_info
{
	int client_socket;
	char addr[INET6_ADDRSTRLEN];
} client_info;

typedef struct {
	char type[5];
	int length;
	int content_length;
	char *content;
	char rest[BUFF_SIZE];
} message;

message *getMessage(char* text) {
	message mess = {.type = "", .length = 0, .content_length = 0, .content = "", .rest = ""};
	//struct message mess = (struct message) malloc(150);

	char mess_length[2];
	memcpy(mess_length, &text[10], 2);
	int length = atoi(mess_length);
	mess.length = length;

	char content_length[2];
	memcpy(content_length, &text[12], 2);
	length = atoi(content_length);
	mess.content_length = length;


	memcpy(mess.type, &text[6], 4);
	mess.type[4] = '\0';

	//memset(mess.content, '\0', sizeof(mess.content));
	//mess.content[strcspn(mess.content, "\n")] = 0;
	mess.content = malloc(sizeof(char) * length + 1);
	memcpy(mess.content, &text[14], length);
	mess.content[strcspn(mess.content, "\n")] = 0;
	//mess.content[length] = '\0';

	//memcpy(mess.rest, &text[14 + strlen(mess.content)], 2);
	//mess.rest[strcspn(mess.rest, "\n")] = 0;

	message *mess_return = malloc(sizeof(message));
	*mess_return = mess;

	return mess_return;
}

void confirmMessage(int skt) {
	char sbuf[BUFF_SIZE];
	memset(sbuf, '\0', BUFF_SIZE);
	sprintf(sbuf, "OK\n");
	write(skt, sbuf, sizeof(sbuf));
}

int isValid(char* message) {
	return (strncmp("KIVUPS", message, PREFIX_LENGTH) == 0);
}


/*
* odesle na socket skt vsechny dosavadni tahy ve hre s hracem hrac1
*/
void sendTurns(char* hrac1, int skt) {
	char sbuf[BUFF_SIZE];
	char hrac2[NICK_LENGTH];
	int index1, index2;
	for (int i = 0; i < players_in_game; i++) {
		if (strcmp(pl_in_game[i], hrac1) == 0) {
			if (i % 2 == 0) {
				strcpy(hrac2, pl_in_game[i + 1]);
				index2 = i + 1;
			}
			else {
				strcpy(hrac2, pl_in_game[i - 1]);
				index2 = i - 1;
			}
			index1 = i;
			break;
		}
	}

	for (int i = 0; i < BOARD_SIZE; i++) {
		memset(sbuf, '\0', BUFF_SIZE);
		if (games[index1][i] == 1) {
			if (i < 10) {
				sprintf(sbuf, "KIVUPSturn%ld0%ld%s0%d\n", 16 + strlen(hrac1), strlen(hrac1), hrac1, i);
			}
			else {
				sprintf(sbuf, "KIVUPSturn%ld0%ld%s%d\n", 16 + strlen(hrac1), strlen(hrac1), hrac1, i);
			}
			printf("odesilam %s", sbuf);
			write(skt, sbuf, sizeof(sbuf));
		}
		else if (games[index2][i] == 1) {
			if (i < 10) {
				sprintf(sbuf, "KIVUPSturn%ld0%ld%s0%d\n", 16 + strlen(hrac2), strlen(hrac2), hrac2, i);
			}
			else {
				sprintf(sbuf, "KIVUPSturn%ld0%ld%s%d\n", 16 + strlen(hrac2), strlen(hrac2), hrac2, i);
			}
			printf("odesilam %s", sbuf);
			write(skt, sbuf, sizeof(sbuf));
		}
	}
}

void* thread_ping(void* arg) {
	struct client_info* clinfo = (client_info*)arg;
	int skt = clinfo->client_socket;
	int index;
	free(arg);
	for (int i = 0; i < client_count; i++) {
		if (sockets[i] == skt) {
			index = i;
			break;
		}
	}
	char *sbuf;
	sbuf = malloc(sizeof(char) * BUFF_SIZE);
	memset(sbuf, '\0', BUFF_SIZE);
	sprintf(sbuf, "KIVUPSping12\n");
	for (;;) {
		/* odesilani ping zpravy v loopu
		* pole pings se nastavi pro kazdy socket na 1 po odeslani ping zpravy a na 0 po prijeti ping zpravy
		* pokud klient neposle ping do nekolika sekund je socket ukoncen
		*/
		sleep(20);
		pings[index] = 1;
		write(skt, sbuf, sizeof(sbuf));
		sleep(5);
		//if (pings[index] == 1) {
			//printf("[%d] klient neodpovedel na ping a byl odpojenn", skt);
			//close(skt);
			//return NULL;
		//}
	}
}

void* thread_fnc(void* arg) {

	char cbuf[BUFF_SIZE];

	struct client_info* clinfo = (client_info*)arg;

	int skt = clinfo->client_socket;

	char empty[NICK_LENGTH] = { '\0' };

	sockets[client_count] = skt;
	pings[client_count] = 0;
	int index = client_count;
	client_count++;

	message *mess;

	printf("[%d] Pripojil se %s\r\n", skt, clinfo->addr);

	int tmp, disc_index;
	char sbuf[BUFF_SIZE];
	int inGame = 0;
	char *player_name;
	player_name = malloc(sizeof(char) * NICK_LENGTH);

	/* poslani ping zpravy na potvrzeni spojeni */
	memset(sbuf, '\0', sizeof(sbuf));
	sprintf(sbuf, "KIVUPSping12\n");
	write(skt, sbuf, sizeof(sbuf));

		while (tmp = recv(skt, cbuf, BUFF_SIZE, 0) > 0) {
			printf("[%d] Prijato %s", skt, cbuf);
			if (isValid(cbuf)) {
				confirmMessage(skt);
				mess = getMessage(cbuf);
				int comp = strcmp(mess->type, "logn");
				if (comp == 0) {
					for (int i = 0; i < client_count; i++) {
						if (sockets[i] == skt) {
							strcpy(players[i], mess->content);
							printf("pridavam %s do seznamu hracu\n", players[i]);
							memset(sbuf, '\0', sizeof(sbuf));
							sprintf(sbuf, "OK\n");
							write(skt, sbuf, sizeof(sbuf));
							break;
						}
					}
				}
				comp = strcmp(mess->type, "join");
				if (comp == 0) {
					int recn = 0;
					char *hrac1;
					char *hrac2;
					hrac1 = malloc(sizeof(char) * NICK_LENGTH);
					hrac2 = malloc(sizeof(char) * NICK_LENGTH);
					int hrac1_length;
					int hrac2_length;
					int total_length;
					/* nejprve kontrola zda hrac uz nema rozehranou hru */
					for (int i = 0; i < disconnected_players; i++) {
						if (strcmp(disc_players[i], mess->content) == 0) {
							disc_players[i] = strdup(empty);
							recn = 1;
							for (int j = 0; j < players_in_game; j++) {
								if (strcmp(pl_in_game[j], mess->content) == 0) {
									if (j % 2 == 0) {
										strcpy(hrac2, pl_in_game[j + 1]);
										strcpy(hrac1, mess->content);
										hrac1_length = strlen(mess->content);
										hrac2_length = strlen(hrac2);
										total_length = 16 + hrac1_length + hrac2_length;
									}
									else {
										strcpy(hrac1, pl_in_game[j - 1]);
										strcpy(hrac2, mess->content);
										hrac1_length = strlen(hrac1);
										hrac2_length = strlen(mess->content);
										total_length = 16 + hrac1_length + hrac2_length;
									}
									memset(sbuf, '\0', sizeof(sbuf));
									sprintf(sbuf, "KIVUPSgame%d0%d%s0%d%s\n", total_length, hrac1_length, hrac1, hrac2_length, hrac2);
									break;
								}
							}
							printf("odesilam %s", sbuf);
							for (int j = 0; j < client_count; j++) {
								write(sockets[j], sbuf, sizeof(sbuf));
							}
							/* odeslat stav hry a zpravu pro druheho hrace, ze se oponent opet pripojil */
							sendTurns(hrac1, skt);
							strcpy(hrac1, mess->content);
							memset(sbuf, '\0', sizeof(sbuf));
							sprintf(sbuf, "KIVUPSrecn%ld0%ld%s\n", 14 + strlen(hrac1), strlen(hrac1), hrac1);
							printf("odesilam %s", sbuf);
							for (int i = 0; i < client_count; i++) {
								write(sockets[i], sbuf, sizeof(sbuf));
							}
							break;
						}
					}
					/* hrac nemel rozehranou hru a pripojil se poprve do queue */
					if (recn == 0) {
						strcpy(queue[players_in_queue], mess->content);
						printf("hrac %s ceka na hru\n", queue[players_in_queue]);
						players_in_queue++;
						if (players_in_queue - start_of_queue > 1) {
							//KIVUPSgame2605ondra05jirka
							strcpy(hrac1, queue[start_of_queue]);
							strcpy(hrac2, queue[start_of_queue + 1]);
							start_of_queue += 2;
							hrac1_length = strlen(hrac1);
							hrac2_length = strlen(hrac2) - 1;
							total_length = 16 + hrac1_length + hrac2_length;

							memset(sbuf, '\0', sizeof(sbuf));
							sprintf(sbuf, "KIVUPSgame%d0%d%s0%d%s\n", total_length, hrac1_length, hrac1, hrac2_length, hrac2);
							printf("odesilam %s", sbuf);
							for (int i = 0; i < client_count; i++) {
								write(sockets[i], sbuf, sizeof(sbuf));
							}

							strcpy(pl_in_game[players_in_game], hrac1);
							players_in_game++;
							strcpy(pl_in_game[players_in_game], hrac2);
							players_in_game++;

							for (int i = 0; i < BOARD_SIZE; i++) {
								games[players_in_game - 1][i] = 0;
								games[players_in_game - 2][i] = 0;
							}
							games[players_in_game - 1][28] = 1;
							games[players_in_game - 1][35] = 1;
							games[players_in_game - 2][27] = 1;
							games[players_in_game - 2][36] = 1;
						}
					}
				}
				comp = strcmp(mess->type, "turn");
				if (comp == 0) {
					memset(sbuf, '\0', sizeof(sbuf));
					memcpy(sbuf, &cbuf, mess->length);
					strcat(sbuf, "\n");
					printf("odesilam %s", sbuf);
					for (int i = 0; i < client_count; i++) {
						write(sockets[i], sbuf, sizeof(sbuf));
					}

					int turn = atoi(mess->rest);
					for (int i = 0; i < players_in_game; i++) {
						if (strcmp(mess->content, pl_in_game[i]) == 0) {
							games[i][turn] = 1;
							if (i % 2 == 0) {
								games[i + 1][turn] = 0;
							}
							else {
								games[i - 1][turn] = 0;
							}
						}
					}
				}
				comp = strcmp(mess->type, "ping");
				if (comp == 0) {
					for (int i = 0; i < client_count; i++) {
						if (sockets[i] == skt) {
							pings[i] = 0;
						}
					}
				}
			}
			else {
				printf("Zprava ve spatnem formatu\n");
				memset(sbuf, '\0', sizeof(sbuf));
				sprintf(sbuf, "zprava nebyla validni\n");
				write(skt, sbuf, sizeof(sbuf));
				close(skt);
			}
			memset(cbuf, '\0', BUFF_SIZE);
		}
		/* odpojil se klient */
		printf("[%d] klient se odpojil\n", skt);
		if (strlen(players[index]) == 0) {
			printf("Odpojil se klient bez prihlaseneho uzivatele\n");
		}
		else {
			memset(sbuf, '\0', BUFF_SIZE);
			sprintf(sbuf, "KIVUPSdisc%ld0%ld%s\n", 14 + strlen(players[index]), strlen(players[index]), players[index]);
			printf("Odpojil se hrac %s\n", players[index]);
			/* odebrat pokud byl ve hre */
			for (int i = 0; i < client_count; i++) {
				if (strcmp(players[index], pl_in_game[i]) == 0) {
					inGame = 1;
					disc_players[disconnected_players] = strdup(players[index]);
					disc_index = disconnected_players;
					disconnected_players++;
					strcpy(player_name, players[index]);
					players[index] = strdup(empty);
				}
			}
			/* odebrat pokud byl v queue  ale ne ve hre*/
			for (int i = 0; i < client_count; i++) {
				if (strcmp(players[index], queue[i]) == 0 && inGame == 0) {
					players_in_queue--;
					if (i == players_in_queue - 1) {
						queue[players_in_queue] = strdup(empty);
					}
					else {
						queue[players_in_queue - 1] = strdup(queue[players_in_queue]);
						queue[players_in_queue] = strdup(empty);
					}
				}
				write(sockets[i], sbuf, sizeof(sbuf));
			}
			/* pokud se hrac po chvili nepripoji znovu z clienta -> trvale odstranen */
			if (inGame == 1) {
				sleep(30);
				if (strcmp(disc_players[disc_index], player_name) == 0) {
					disc_players[disc_index] = strdup(empty);
					for (int i = 0; i < client_count; i++) {
						write(sockets[i], sbuf, sizeof(sbuf));
					}
				}
			}
		}
		return NULL;
}

int main(int argc, char** argv) {

	int skt;
	int res;
	players_in_queue = 0;
	client_count = 0;
	start_of_queue = 0;
	players_in_game = 0;
	disconnected_players = 0;

	skt = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (skt < 0) {
		die("Nelze inicializovat socket");
	}

	srand(time(NULL));

	int param = 1;
	res = setsockopt(skt, SOL_SOCKET, SO_REUSEADDR, (const char*)&param, sizeof(int));

	struct sockaddr_in addr;
	int port;
	char ip[15];

	//147.228.61.0
	addr.sin_family = AF_INET;
	if (argc > 2) {
		port = atoi(argv[2]);
		strcpy(ip, argv[1]);
	}
	else if (argc == 2) {
		strcpy(ip, argv[1]);
		port = DEFAULT_PORT;
	}
	else {
		strcpy(ip, DEFAULT_IP);
		port = DEFAULT_PORT;
	}

	addr.sin_port = htons(port);
	addr.sin_addr.s_addr = inet_addr(ip);


	res = bind(skt, (struct sockaddr*)&addr, sizeof(addr));
	if (res != 0) {
		die("Nelze nabindovat na zadanou adresu");
	}

	res = listen(skt, 10);
	if (res != 0) {
		die("Nelze vytvorit frontu spojeni");
	}

	struct sockaddr_in incoming;
	int inlen = sizeof(incoming);

	pthread_t thr;
	pthread_t ping;

	for (int i = 0; i < SERVER_CAPACITY; i++) {
        queue[i] = malloc(sizeof(char) * NICK_LENGTH);
		players[i] = malloc(sizeof(char) * NICK_LENGTH);
		pl_in_game[i] = malloc(sizeof(char) * NICK_LENGTH);
		disc_players[i] = malloc(sizeof(char) * NICK_LENGTH);
	}

	game = fopen("game_data", "w+");

	printf("\n");
	printf("Othello server - KIV/UPS");
	printf("\n");
	printf("> Nasloucham na %s:%d\n", ip, port);

	for(;;)
	{
		int inskt = accept(skt, (struct sockaddr*)&incoming, &inlen);
		if (inskt >= 0) {
			struct client_info* infoptr = malloc(sizeof(struct client_info));
			infoptr->client_socket = inskt;
			inet_ntop(AF_INET, &incoming.sin_addr.s_addr, infoptr->addr, INET6_ADDRSTRLEN);

			pthread_create(&thr, NULL, (void*)&thread_fnc, (void*)infoptr);
			pthread_detach(thr);

			pthread_create(&ping, NULL, (void*)&thread_ping, (void*)infoptr);
			pthread_detach(ping);
		}
	}

	return 0;
}
