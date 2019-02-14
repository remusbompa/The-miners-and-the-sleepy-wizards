/* Bompa Remus 335CB */

1) CommunicationChannel
- se foloseste cate un buffer de tipul ArrayBlockingQueue<Message> pentru fiecare canal:
bufferWizardChannel si bufferMinerChannel, pentru a beneficia de operatii atomice de put si
take si de bloacarea thread-ului in cazul in care aceste incearca sa puna elemente intr-o coada
plina sau sa ia elemente dintr-o coada goala.
- capacitatea buffer-elor este de max (1000), suficient de mare pentru a evita o situatie de
interlock, in care toti Wizards pun in buffer-ul lor, Miners umplu buffer-ul lor si nu mai
are cine sa ia mesaje din buffer-ul Miners.  
- pentru metodele putMessageMinerChannel, getMessageMinerChannel, getMessageWizardChannel, am 
folosit doar metodele put si take corespunzatoare buffer-ului in care se pune/ din care se ia
- pentru metoda putMessageWizardChannel, nu se vor pune in bufferWizardChannel toate mesajele
primite:
	-mesajele END vor fi ignorate
	-in cazul in care se primeste un mesaj EXIT, va fi lasat sa puna in continuare doar primul
	care l-a pus, pentru a ajunge in buffer doar nrMiners mesaje
	-cum fiecare mesaj despre un nod adiacent, primit de un Miners trebuie sa contina: parinte,
	nod si hash-ul nodului, acesta trebuie compus in CommunicationChannel din 2 mesaje 
	consecutive primite de la un thread: mesaj cu info despre nodul parinte si mesaj cu info
	despre nodul adiacent. Pentru a mentine cele 2 mesaje in ordine, se foloseste o structura
	HashMap<Long, Message> in care cheia este id-ul thread-ului si se construiesc mesaje cand
	ajunge un mesaj cu info despre parinte (deci cand id-ul thread-ului nu se afla in map). In
	noul mesaj se pune campul parinte, luat din campul currentRoom al mesajului primit, iar in
	cazul in care mesajul este de tip NO_PARENT, se pune -1. Cand se primeste si al doilea
	mesaj pentru acelasi thread (map contine id-ul thread-ului), se elimina mesajul din map si
	se pun campurile currentRoom si data, luate din cele ale mesajului primit. Pentru a evita 
	trimiterea unor mesaje cu info despre noduri adiacente, care au fost deja trimise, se 
	foloseste structura HashSet<Integer> putNodes. Pentru mesajul construit, se verifica daca
	nodul adiacent currentRoom a mai fost trimis odata (da putNodes contine currentRoom), caz in
	care mesajul nu mai e trimis. Daca nodul nu a mai fost trimis, se adauga nodul in putNodes si
	se pune mesajul in buffer ( bufferWizardChannel.put(newMessage) ).

2) Miner
- thread-ul Miner primeste mesaje pe canalul Wizard intr-o bucla infinita ( while(true) )
- daca mesajul este de tip EXIT, Miner isi termina executia
- in caz contrar, se aplica metoda encryptMultipleTimes(data, hashCount), din fisierul 
solver/Main.java, care aplica functia da hash de hashCount ori pe campul data al mesajului
primit. 
- se construieste un nou mesaj newMessage, avand campurile parentRoom si currentRoom, de la 
mesajul initial si campul data ccriptat prin aplicarea encryptMultipleTimes.
- mesajul este trimis pe canalul Miner.
