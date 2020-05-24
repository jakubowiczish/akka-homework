Za zadanie można dostać maksymalnie 15 punktów. 

Na wykonanie zadania jest dodatkowy tydzień - termin 
oddania zadania to poniedziałek 01.06 godz. 8:00.

Zadanie składa się z 3 części, za każdą część można dostać 5 punktów. 
Części 2. i 3. zależą od części 1., ale między sobą są niezależne.

Do zadania należy wykonać schemat. 
W każdej części 1 z 5 punktów do uzyskania jest za część schematu związaną 
z tą częścią zadania (sumarycznie z 15 możliwych punktów 3 punkty są za schemat).

Część 1:

Z użyciem platformy Akka zaimplementuj serwis porównujący ceny danego przedmiotu w różnych sklepach:

- Klienci to aktorzy w systemie Akka

- Serwer to aktor w systemie Akka (tworzy kolejnych aktorów wedle potrzeb)

- Klientów oraz serwer można uruchomić w jednej aplikacji (jeden system aktorów - nie trzeba korzystać z Remoting)

- Klienci wysyłają zapytania podając nazwę przedmiotu

- Serwer sprawdza cenę w dwóch sklepach i zwraca niższą z nich

- Sprawdzenie ceny symulujemy przez uśpienie na czas z zakresu 100 do 500 ms, a następnie zwrócenie losowej wartości z przedziału 1 do 10

- Serwer ma być w stanie obsługiwać wielu klientów jednocześnie

- Czas obsługi pojedynczego klienta ma być jak najkrótszy (m.in. sprawdzanie cen w sklepach powinno być równoległe)

- Zakładamy maksymalny czas na obsługę jednego zlecenia od klienta 300 ms - należy zaimplementować obsługę timeout'u zapytań o ceny - na wyniki czekamy w sumie do 300 ms

- Jeśli w podanym czasie dostaniemy dwie ceny, zwracamy niższą z nich, jeśli dostaniemy jedną, zwracamy tę cenę, jeśli żadnej, zwracamy komunikat o braku dostępności cen dla tego przedmiotu

- Proszę zwrócić uwagę na odpowiednią obsługę błędów oraz czasu życia aktorów

Część 2:3

Dodaj funkcjonalność zapisywania oraz odczytywania historii wyszukiwań:

- Przy każdym zapytaniu od klienta należy wpisać do lokalnej bazy danych informację o wystąpieniu tego zapytania

- Przy odpowiedzi na zapytanie klienta, serwer, poza ceną, zwraca dodatkowo informację ile razy to zapytanie już wystąpiło

- Baza danych ma mieć jedną tabelę z polami: zapytanie oraz liczba wystąpień

- Jeśli zapytania nie ma w bazie, dodajemy je z liczbą wystąpień 1

- Jeśli już jest w bazie, zwiększamy mu liczbę wystąpień o 1

- Można wykorzystać dowolną bazę danych (np. SQLite działający w pojedynczym pliku, bez systemu bazodanowego)

- Proszę zwrócić uwagę, aby zapis do bazy nie spowalniał odpowiedzi do klienta (zakładamy, że w bardzo krótkim czasie możemy dostać bardzo dużo zapytań)

Część 3:

Wykorzystaj Akka-HTTP w formie serwera oraz klienta:

- Z użyciem Akka-HTTP uruchom serwer http, który będzie stanowił interfejs dostępowy do systemu porównywania cen

- Serwer po wpisaniu zapytania w postaci odpowiedniej ścieżki ma zwracać odpowiedź podobną jak w konsoli

- Format ścieżki: localhost:8080/price/[nazwa_przedmiotu], np. localhost:8080/price/laptop

- Test działania wykonujemy przez przeglądarkę lub narzędzie konsolowe, które wypisuje odpowiedź http

- Wskazówki: https://developer.lightbend.com/guides/akka-http-quickstart-java/http-server.html

- Z użyciem Akka-HTTP dodaj funkcjonalność sprawdzania opinii o produkcie

- Funkcjonalność ma być dostępna pod zapytaniem localhost:8080/review/[nazwa_przedmiotu]

- Należy wysłać zapytanie z nazwą przedmiotu do serwisu opineo.pl, a następnie wypisać sekcję 'Zalety' dla pierwszej znalezionej pozycji

- Wskazówki - kod do pobierania treści strony:
```
Http.get(system).singleRequest(HttpRequest.create(url)).thenCompose(response ->
response.entity().toStrict(timeout, materializer)).thenApply(entity ->
entity.getData().utf8String()).thenAccept(body -> ...
```

Zadanie można wykonać w dowolnym języku wspierającym platformę Akka lub jej odpowiednik.