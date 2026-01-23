# Zakazani Video Streaming - Backend Implementacija

## 📋 Šta je implementirano

### 1. **Model Promena**
- `Video.java` - Dodano polje:
  - `scheduledAt: LocalDateTime` - Vreme zakazanog prikaza (nullable)

### 2. **DTOs (Data Transfer Objects)**
- `UpdateVideoDTO.java` - Za ažuriranje videa sa `scheduledAt` poljem
- `ScheduledStreamResponse.java` - Kompletan odgovor sa streaming info:
  - `videoId`, `title`, `isAvailable`, `scheduledAt`
  - `durationSeconds` - trajanje videa
  - `currentOffsetSeconds` - trenutna minutaž (svi gledalci gledaju istu!)
  - `streamStatus` - status: "NOT_STARTED", "LIVE", "FINISHED"
  - `message` - čitljiva poruka
- `VideoDTO.java` - Ažurirano da uključi:
  - `scheduledAt`, `available`, `currentOffsetSeconds`

### 3. **Servis za Zakazani Streaming**
- `ScheduledStreamingService.java` - Kompletan servis sa metodama:

| Metoda | Opis |
|--------|------|
| `isVideoAvailable(videoId)` | Proverava da li je video dostupan |
| `getCurrentStreamOffset(videoId)` | Računa trenutni offset (sve osobe gledaju istu minutažu!) |
| `getScheduledStreamInfo(videoId)` | Kompletne info o zakazanom videu |
| `scheduleVideo(videoId, scheduledAt)` | Zakaži video za određeno vreme |
| `unscheduleVideo(videoId)` | Otkaži zakazivanje (čini ga odmah dostupnim) |
| `getVideoDurationSeconds(video)` | Procenjuje trajanje videa |

### 4. **Kontroler - Novi Endpointi**
- `VideoController.java` - Dodani endpointi:

| Metoda | Endpoint | Opis |
|--------|----------|------|
| GET | `/api/videos/{id}/scheduled-info` | Info o zakazanom videu + offset |
| PUT | `/api/videos/{id}` | Ažuriranje sa zakazanim vremenom |

### 5. **Sigurnost**
- `SecurityConfig.java` - Ažuriran:
  - GET `/api/videos/**` - JAVNO (uključujući scheduled-info)
  - PUT `/api/videos/**` - SAMO AUTENTIFICIRANI (vlasnik videa)

### 6. **Testiranje**
- `ScheduledStreamingTest.java` - 6 JUnit test slučaja:
  1. Video zakazan u budućnosti nije dostupan
  2. Video zakazan u prošlosti je dostupan
  3. Offset se pravilno računa (sinhronizovani streaming)
  4. Kompletan info odgovor
  5. Otkazivanje zakazivanja
  6. Simulacija više gledalaca sa istom minutažom

### 7. **Demo Skript**
- `demo-scheduled-streaming.ps1` - PowerShell skript koji:
  1. Registruje korisnika
  2. Kreira video
  3. Zakaži video za 30 sekundi
  4. Prikaz statusa pre početka
  5. Simulira čekanje na početak
  6. Prikaz statusa tokom streaming-a
  7. Simulacija više gledalaca

---

## 🎯 Kako Funkcioniše

### Logika Zakazanog Streaming-a:

1. **Korisnik kreira video** - POST `/api/videos`
2. **Korisnik zakaži video** - PUT `/api/videos/{id}` sa `scheduledAt` vremenom
3. **Video nije dostupan** - Sve dok `trenutnoVreme < scheduledAt`
4. **U zakazano vreme** - Video postaje dostupan (`currentOffsetSeconds = 0`)
5. **Sinhronizovani streaming** - Offset se računa kao:
   ```
   currentOffsetSeconds = trenutnoVreme - scheduledAt
   ```
6. **Svi gledalci u istoj minutaži** - Svaki gledaoc koji se konektuje dobija **trenutni offset**
   - Primer: Ako je video zakazan za 08:00, a gledaoc se konektuje u 08:03 → vidi od 3. minuta
   - Drugi gledaoc se konektuje u 08:03:15 → vidi od ~3:15

### Primer Timeline-a:

```
08:00:00 - Video postaje dostupan (offset = 0)
08:00:30 - Gledaoc 1 se konektuje (vidi offset = 30s)
08:01:00 - Gledaoc 2 se konektuje (vidi offset = 60s) 
08:01:00 - Gledaoc 1 sada vidi offset = 60s (svi prate istu minutažu!)
08:10:00 - Video je završen (offset >= duration)
```

---

## 🔧 REST API

### Zakazivanje Videa
```bash
PUT /api/videos/1
Content-Type: application/json

{
  "scheduledAt": "2026-01-23T20:30:00"
}
```

**Odgovor:**
```json
{
  "id": 1,
  "title": "Moj video",
  "scheduledAt": "2026-01-23T20:30:00",
  "available": false,
  "currentOffsetSeconds": null
}
```

### Otkazivanje Zakazivanja
```bash
PUT /api/videos/1
Content-Type: application/json

{
  "scheduledAt": ""
}
```

### Dobijanje Streaming Info
```bash
GET /api/videos/1/scheduled-info
```

**Odgovor (pre početka):**
```json
{
  "videoId": 1,
  "title": "Moj video",
  "isAvailable": false,
  "scheduledAt": "2026-01-23T20:30:00",
  "durationSeconds": 600,
  "currentOffsetSeconds": null,
  "streamStatus": "NOT_STARTED",
  "message": "Video počinje za 15 m 30 s"
}
```

**Odgovor (tokom streaming-a):**
```json
{
  "videoId": 1,
  "title": "Moj video",
  "isAvailable": true,
  "scheduledAt": "2026-01-23T20:30:00",
  "durationSeconds": 600,
  "currentOffsetSeconds": 125,
  "streamStatus": "LIVE",
  "message": "Streaming je u toku"
}
```

### Dobijanje Videa sa Offset Info
```bash
GET /api/videos/1
```

**Odgovor:**
```json
{
  "id": 1,
  "title": "Moj video",
  "scheduledAt": "2026-01-23T20:30:00",
  "available": true,
  "currentOffsetSeconds": 125
}
```

---

## 🧪 Pokretanje Testova

### JUnit Testovi
```bash
# Svi testovi
mvn test -Dtest=ScheduledStreamingTest

# Specifičan test
mvn test -Dtest=ScheduledStreamingTest#testSynchronizedStreamingOffset
```

### Demo Skript (PowerShell)
```powershell
# Izvršavanje demo skripte
.\scripts\demo-scheduled-streaming.ps1
```

---

## 📊 Streaming Status-i

| Status | Vreme | Dostupan | Offset | Opis |
|--------|-------|----------|--------|------|
| `NOT_STARTED` | Sada < zakazano | ❌ | null | Čeka se zakazano vreme |
| `LIVE` | zakazano ≤ sada < zakazano + duration | ✅ | > 0 | Streaming je u toku |
| `FINISHED` | Sada ≥ zakazano + duration | ✅ | = duration | Streaming je završen |

---

## 🚀 Integracija sa Frontend-om

Frontend bi trebalo da:

1. **Prikaže UI za zakazivanje** - Datum/vreme picker pri kreiranju videa
2. **Prikaze status videa**:
   - Ako nije zakazan → "Dostupan sada"
   - Ako je zakazan u budućnosti → "Dostupan [datum/vreme]"
   - Ako je zakazan u prošlosti → "Zakazan [datum/vreme], trenutni offset: Xs"

3. **Streaming player**:
   - Inicijalizuj video sa `currentOffsetSeconds` offset-om
   - Periodno osvežavaj offset (npr. svakih 5 sekundi)
   - Sve osobe gledaju istu minutažu

4. **Poziva API**:
   - GET `/api/videos/{id}` - Osnovne info
   - GET `/api/videos/{id}/scheduled-info` - Detalje i offset
   - PUT `/api/videos/{id}` - Za zakazivanje/otkazivanje

---

## 🔍 Kako Radi Offset Kalkulacija

### In-Memory Čuvanje Vremena
```java
LocalDateTime scheduledAt = video.getScheduledAt();  // npr. 08:00:00
LocalDateTime now = LocalDateTime.now();              // npr. 08:03:45
Duration duration = Duration.between(scheduledAt, now);
int offsetSeconds = duration.getSeconds();            // 225 sekundi
```

### Sve Osobe Vide Stu Minutažu
```
Korisnik A pita server u 08:03:45 → offset = 225s
Korisnik B pita server u 08:03:46 → offset = 226s
Korisnik A osvežava u 08:03:46  → offset = 226s (svi prate istu!)
```

---

## 🎬 Mogućnosti Proširenja

1. **HLS Streaming** - Umesto offset-a, vratiti HLS manifest sa timeout-om
2. **DASH Streaming** - Slično kao HLS, ali sa DASH formatom
3. **Rekording** - Sačuvaj live stream za kasnije preglede
4. **Chat** - Live chat tokom streaming-a (komentari su već implementirani!)
5. **Maksimalan broj gledalaca** - Limitiraj broj gledalaca po videu
6. **Analytics** - Prati kada se gledaoci konektuju/diskonektuju

---

## 🐛 Debugging

Ako greške:
1. Proverite da li je `scheduledAt` ISO format
2. Proverite database - da li je field pravilno kreiran
3. Proverite timezone - server koristi sistem timezone
4. Proverite duration - ako je preskupo, može vratiti 0 sekundi

---

## 📝 Napomene

- Offset se računa **u memoriji** - nije nužna baza podataka
- Svi gledalci automatski dobijaju **iste minutaže** - bez dodatne konfiguracije
- **Bez FFprobe-a** - Trajanje se procenjuje po veličini fajla
- Za produkciju: Koristi FFmpeg/FFprobe za precizno trajanje
