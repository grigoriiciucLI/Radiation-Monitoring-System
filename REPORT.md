# Radiation Monitoring System – Raport

## 1. Decizii de Design

### Arhitectura aplicației
Aplicația urmează o arhitectură pe straturi, cu separare clară a responsabilităților:

- **Model** – clase POJO (`MonitoringStation`, `RadiationReading`) derivate dintr-o clasă abstractă `Entity<ID>`, astfel încât repository-ul generic să poată lucra cu orice tip de entitate.
- **Repository** – interfață generică `Repository<ID, E>` cu implementări JDBC concrete (`StationRepositoryImpl`, `ReadingRepositoryImpl`). Tot SQL-ul este scris manual, fără ORM.
- **Validator** – interfață generică `Validator<E>` cu implementări specifice pentru fiecare entitate. Validarea este separată de logica de business.
- **Service** – clasă abstractă generică `IdentifiableService<ID, E, R>` care validează entitatea și apoi o trimite către repository. Serviciile concrete (`StationService`, `ReadingService`) construiesc obiectele din parametri bruti și apelează metodele din serviciul abstract.
- **UI** – `MainFrame` apelează exclusiv serviciile, fără acces direct la repository sau baza de date. `ReadingFormDialog` este un dialog modal reutilizabil pentru operațiile Add și Edit.

### Gestionarea conexiunilor
`DatabaseManager` este o clasă utilitară statică cu o singură metodă `getConnection()`. Nu există un connection pool — conexiunile sunt create la cerere și închise imediat după fiecare operație prin `try-with-resources`, garantând că nicio conexiune nu rămâne deschisă.

### Prevenirea SQL injection
Fiecare valoare introdusă de utilizator ajunge la baza de date exclusiv prin parametrii `PreparedStatement` (placeholder-ul `?`), inclusiv pattern-urile LIKE din căutări. Concatenarea de string-uri în query-uri SQL nu este folosită niciodată.

### Validare
Validarea se face pe trei niveluri:
- **UI** – `ReadingFormDialog` verifică că câmpurile sunt completate și că formatul este corect înainte de a trimite datele mai departe.
- **Service** – `ReadingValidator` și `StationValidator` verifică regulile de business (ex: radiation level ≥ 0, câmpuri obligatorii).
- **Baza de date** – constrângeri `CHECK` și `NOT NULL` în PostgreSQL ca ultimă linie de apărare.

---

## 2. Provocări Întâmpinate și Soluții

### Conectarea la baza de date
Inițial s-a încercat conectarea la Microsoft SQL Server, care a ridicat probleme legate de autentificarea Windows (necesita `sqljdbc_auth.dll`), instanțe named (`localhost\SQLEXPRESS01`) și porturi dinamice. Soluția a fost migrarea la PostgreSQL, care se conectează simplu prin URL + user + password, fără configurări suplimentare.

### Generics în Java
Definirea corectă a tipurilor generice pentru `IdentifiableService` a ridicat probleme de compilare — Java nu putea rezolva automat că `StationRepository extends Repository<Integer, MonitoringStation>` este compatibil cu `Repository<ID, E>`. Soluția a fost adăugarea unui al treilea parametru generic `R extends Repository<ID, E>`, astfel încât Java să știe exact ce tip de repository se folosește.

### Sortare și selecție în JTable
La sortarea coloanelor, indexul vizual al rândului diferă de indexul din model. Uitarea metodei `convertRowIndexToModel()` ducea la editarea sau ștergerea unui rând greșit. Soluția a fost aplicarea consistentă a acestei conversii în toate operațiile CRUD.

### Maparea tipurilor de date JDBC
`TIMESTAMP` din PostgreSQL se mapează la `java.sql.Timestamp`, nu la `java.time.LocalDateTime` folosit în model. Conversia se face explicit în repository (`Timestamp.valueOf(localDT)` și `ts.toLocalDateTime()`), păstrând modelul curat de tipuri JDBC.

---

## 3. Ce Am Învățat

- **Ciclul de viață JDBC** – deschiderea, folosirea și închiderea corectă a obiectelor `Connection`, `PreparedStatement` și `ResultSet` cu `try-with-resources`.
- **Pattern-ul master-detail în Swing** – folosirea unui `ListSelectionListener` pe tabelul părinte pentru a declanșa un query parametrizat pe tabelul copil la fiecare schimbare de selecție.
- **Generics avansate în Java** – definirea de interfețe și clase generice cu bounds (`E extends Entity<ID>`, `R extends Repository<ID, E>`) pentru a scrie cod reutilizabil fără duplicare.
- **Separarea responsabilităților** – importanța de a ține logica bazei de date, logica de validare și logica UI în straturi separate, astfel încât fiecare clasă să aibă o singură responsabilitate.
- **PostgreSQL** – sintaxa specifică (`SERIAL`, `ILIKE`, `ON DELETE CASCADE`), diferențele față de SQL Server și configurarea conexiunii JDBC.
