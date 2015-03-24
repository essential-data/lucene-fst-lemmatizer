Slovenská podpora pre Lucene/SOLR + command-line lemmatizátor
=============================================================

Autorské práva
--------------

Copyright (c) 2015 Essential Data, s.r.o.

Toto dielo je možné používať v súlade s textami nasledujúcich licencií:

* GNU Affero General Public License, verzia 3
* Creative Commons Attribution-ShareAlike 4.0 International
* GNU Free Documentation License 1.3

Viac informácií v súbore LICENSE. 

Zaujíma vás práca s jazykom? Pracujte pre nás!
----------------------------------------------

Essential Data pracuje s jazykom, s dátami a na zaujímavých projektoch. Pozrite si
[aktuálne otvorené pozície](http://www.essential-data.sk/pracujte-pre-nas/) a pracujte v skvelom
tíme plnom šikovných ľudí.

Použitie - ako lematizátor z príkazového riadku
-----------------------------------------------

Takto spustíme fstutils:

```
java -jar target/fstutils-0.3.2-jar-with-dependencies.jar
Usage: fstutils lemmatize <path-to-fst> <options>, where options are:
-e: echo when a word is not in the dictionary, e.g. 'foo bar' -> 'foo bar'.
Without the -e option it is 'foo bar' -> 'bar'
```

Ak chceme zlematizovať stdin, použitie napr. takto:

```
java -jar target/fstutils-0.3.2-jar-with-dependencies.jar lemmatize fst/slovaklemma.fst -e
```

Tento príkaz dá všetky slová, ktoré má v slovníku do základného tvaru, ostatné len vypíše. 


Kompilácia
----------

Najjednoduchší spôsob skompilovania je pomocou 

```
mvn package
```

Odkazy
------

* [Github spoločnosti Essential Data](https://github.com/essential-data/) - obsahuje naše open-source projekty (aj) pre prácu s jazykom