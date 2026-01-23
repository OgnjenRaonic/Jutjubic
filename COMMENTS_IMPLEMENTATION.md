# Backend za Komentare - Implementacija

## 📋 Šta je implementirano

### 1. **Entiteti (Models)**
- `Comment.java` - Entitet za komentare sa polјima: id, videoId, user, text, createdAt

### 2. **DTOs (Data Transfer Objects)**
- `CommentDTO.java` - DTO za slanje komentara (sa username i createdAt kao ISO string)
- `CreateCommentDTO.java` - DTO za kreiranje novog komentara (samo text)
- `CommentPageResponse.java` - DTO za paginizovani odgovor
- `RateLimitInfoDTO.java` - DTO za informacije o rate limitingu

### 3. **Repozitorijumi (Repositories)**
- `CommentRepository.java` - JPA repozitorijum sa metodama:
  - `findByVideoIdOrderByCreatedAtDesc()` - dobija komentare po video ID-u sortiran od najnovijeg
  - `findByUserIdAndCreatedAtAfter()` - dobija komentare korisnika iz zadnjeg sata

### 4. **Servis za Rate Limiting**
- `CommentRateLimiter.java` - In-memory rate limiter sa metodama:
  - `canComment(userId)` - proverava da li korisnik može da komentariše
  - `recordComment(userId)` - beleži novi komentar
  - `getCommentsInLastHour(userId)` - vraća broj komentara u poslednjem satu
  - `getNextAvailableTime(userId)` - vraća vreme kada će biti dostupno

### 5. **Servis za Komentare**
- `CommentService.java` - Primarni servis sa:
  - **Keširanje** - `@Cacheable` za GET zahteve
  - **Rate limiting** - Provera pre kreiranja komentara
  - **Paginacija** - Podržava page i pageSize

### 6. **Kontroler**
- `CommentController.java` - REST endpointi:

| Metoda | Endpoint | Opis |
|--------|----------|------|
| GET | `/api/comments/video/{videoId}` | Dobija komentare sa paginacijom |
| POST | `/api/comments/video/{videoId}` | Kreira novi komentar (samo autentificirani) |
| GET | `/api/comments/rate-limit-info` | Rate limit info za trenutnog korisnika |

### 7. **Keširanje**
- `CacheConfig.java` - Cache manager sa `ConcurrentMapCacheManager`
- Kešira rezultate `/api/comments/video/{videoId}` sa ključem: `videoId_page_pageSize`
- Cache se **invalidira** kada se kreira novi komentar

### 8. **Sigurnost**
- `SecurityConfig.java` - Ažuriran sa:
  - GET `/api/comments/video/**` - JAVNO
  - POST `/api/comments/video/**` - SAMO AUTENTIFICIRANI
  - GET `/api/comments/rate-limit-info` - SAMO AUTENTIFICIRANI

### 9. **Testiranje**
- `CommentRateLimitTest.java` - JUnit test sa 2 test slučaja:
  - Test slanja 65 komentara (očekuje 60 uspešnih, 5 odbijenih)
  - Test rate limitiranja nakon 60 komentara
- `stress-test-comments.ps1` - PowerShell skript za stress test sa 65 komentara

---

## 🔧 Kako pokrenutи

### 1. Kompajliranje
```bash
mvn clean compile
```

### 2. Pokretanje aplikacije
```bash
mvn spring-boot:run
```

### 3. Pokretanje testova
```bash
# Samo comment rate limit test
mvn test -Dtest=CommentRateLimitTest

# Svi testovi
mvn test
```

### 4. Stress test sa PowerShell
```powershell
# Prvo registruj korisnika bilo kako
# Zatim pokreni skript
.\scripts\stress-test-comments.ps1
```

---

## 🛠️ ŠUMA TREBA DA IZBACI IZ FRONTEND-A

### Šta **TREBA DA BUDE UKLONENO** iz `CommentService`:

```typescript
// ❌ IZBACI SVE OVO - Rate limiting je sada na backend-u

// ❌ IZBACI ove promenljive:
private commentTimestamps: number[] = [];
private readonly HOUR_MS = 60 * 60 * 1000;
private readonly MAX_COMMENTS_PER_HOUR = 60;

// ❌ IZBACI ove metode:
canComment(): boolean { ... }
private recordCommentTimestamp(): void { ... }
private saveCommentTimestamps(): void { ... }
private loadCommentTimestamps(): void { ... }

// ❌ IZBACI iz createComment():
if (!this.canComment()) { ... throw error ... }  // Sada će server da proverava
this.recordCommentTimestamp();                   // Sada server to radi
```

### Šta **TREBA DA BUDE PROMENJENO** u `CommentService`:

1. **getRateLimitInfo()** - Trebalo bi da je server-side call:
```typescript
getRateLimitInfo(): Observable<RateLimitInfo> {
  return this.http.get<RateLimitInfo>(`${this.base}/rate-limit-info`, {
    withCredentials: true
  });
}
```

2. **createComment()** - Ukloni lokalnu proveru rate limitinga:
```typescript
createComment(videoId: number, dto: CreateCommentDTO): Observable<Comment> {
  // ❌ IZBACI: if (!this.canComment()) { ... }
  
  return this.http.post<Comment>(`${this.base}/video/${videoId}`, dto, { 
    withCredentials: true 
  }).pipe(
    tap(comment => {
      if (this.commentCache.has(videoId)) {
        const cache = this.commentCache.get(videoId)!;
        cache.next([comment, ...cache.value]);
      }
      // ❌ IZBACI: this.recordCommentTimestamp();
      this.commentAdded$.next(comment);
    })
  );
}
```

3. **Konstruktor** - Ukloni `loadCommentTimestamps()`:
```typescript
constructor(private http: HttpClient) {
  // ❌ IZBACI: this.loadCommentTimestamps();
}
```

### Što **TREBA DA OSTANE** u frontend-u:

✅ Paginacija (page, pageSize)
✅ Caching sa BehaviorSubject
✅ UI za prikazivanje rate limit info
✅ Autentifikacijska provera
✅ Error handling

---

## 📊 Kako funkcioniše Rate Limiting

### Na Backend-u (SERVER-SIDE):

1. Svaki korisnik ima `CommentRateLimiter` koji čuva **nanosecondne timestamp-e** zadnjih komentara
2. Kada korisnik pošalje komentar:
   - Proveri da li ima < 60 komentara u poslednjem satu
   - Ako ima, spremi timestamp i kreiraj komentar
   - Ako nema, vrati HTTP 429 (Too Many Requests)

3. Rate limiter automatski **očisti stare timestamp-e** (starije od 1 sata)

### Na Frontend-u (APÓS UPDATE):

1. Frontend **poziva `/api/comments/rate-limit-info`** da dobije:
   - Broj komentara u poslednjem satu
   - Da li može da komentariše
   - Kada će biti dostupno (ako je blokiran)

2. Frontend **prikazuje** rate limit info u UI bez greške

---

## 🔍 Testiranje Rate Limitiranja

### Test 1: 65 komentara u nizu
```bash
mvn test -Dtest=CommentRateLimitTest#testCommentRateLimit
```
**Očekivani rezultat:**
- ✅ 60 komentara uspešno kreirano
- ❌ 5 komentara odbijeno

### Test 2: Blokiranje nakon 60 komentara
```bash
mvn test -Dtest=CommentRateLimitTest#testRateLimitReset
```
**Očekivani rezultat:**
- ✅ Prvih 60 komentara prolaze
- ❌ 61. komentar biva odbijen

### Stress Test sa PowerShell
```powershell
.\scripts\stress-test-comments.ps1
```
**Očekivani rezultat:**
- Registracija novog korisnika
- Slanje 65 komentara
- Prikaz statistike (60 prihvaćenih, 5 odbijenih)

---

## 🚀 API Primeri

### Dobijanje komentara
```bash
GET http://localhost:8080/api/comments/video/1?page=0&pageSize=10
```

### Kreiranje komentara
```bash
POST http://localhost:8080/api/comments/video/1
Content-Type: application/json

{
  "text": "Odličan video!"
}
```
**Odgovori:**
- ✅ 201 Created - Komentar je kreiran
- ⚠️ 429 Too Many Requests - Rate limit dostignut
- ❌ 401 Unauthorized - Nije ulogovan
- ❌ 400 Bad Request - Tekst je prazan ili predznak

### Rate Limit Info
```bash
GET http://localhost:8080/api/comments/rate-limit-info
```

**Odgovor:**
```json
{
  "commentsInLastHour": 45,
  "canComment": true,
  "nextAvailableAt": null
}
```

Ili kada je blokiran:
```json
{
  "commentsInLastHour": 60,
  "canComment": false,
  "nextAvailableAt": "2026-01-23T15:45:30.123456"
}
```

---

## 📝 Beleške

- **Rate limiting**: In-memory, **per korisnik**, **ne per video**
- **Keširanje**: Server-side `ConcurrentMapCache` (memorija)
- **Paginacija**: 10 komentara po stranici po defaultu (max 100)
- **Sortiranje**: Od najnovijeg prema najstarijem
- **Tekstualna ograničenja**: Max 1000 karaktera po komentaru

---

## 🐛 Debugging

Ako test ne prolazi:
1. Proverite da li je `CommentRateLimiter` pravilno injektovan
2. Proverite da li je `@EnableCaching` aktivno
3. Proverite bazu - da li se komentari čuvaju
4. Pogledajte logove za `429` ili drugr error kodove
