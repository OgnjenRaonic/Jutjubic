# Local Trending Performance Testing

Ovaj dokument opisuje kako pokrenuti testove performansi za local trending funkcionalnost.

## Struktura Testova

### 1. **SQL Seed Skripte** (`src/main/resources/db/migration/V4__seed_trending_test_data.sql`)

Kreira:
- **1000 videa** sa raznovrsnim tagovima (action, comedy, drama, music, sports, itd.)
- **10000 video_view zapisa** raspoređenih po glavnim gradovima:
  - Beograd: 3000 zapisa
  - Novi Sad: 2500 zapisa
  - Niš: 2500 zapisa
  - Kragujevac: 2000 zapisa

### 2. **JUnit Test Klasa** (`src/test/java/com/example/demo/LocalTrendingPerformanceTest.java`)

Sadrži 4 glavna testa:

#### Test 1: `testTrendingLatencyByRadius()`
- Meri latency za radijuse: 5km, 10km, 20km, 50km
- 5 pokušaja po radijusu
- Rezultati: Avg, Min, Max, P95 (ms)

#### Test 2: `testConcurrentRequests()`
- Simulira simultane korisnike: 1, 5, 10, 20, 50
- Svaki korisnik radi 10 zahteva
- Meri kako se latency degradira sa povećanjem konkurentnosti

#### Test 3: `testMixedScenarios()`
- Kombinuje radijus i konkurentnost:
  - Small (5km, 1 korisnik)
  - Medium (20km, 1 korisnik)
  - Large (50km, 1 korisnik)
  - Small (5km, 10 korisnika)
  - Large (50km, 10 korisnika)

#### Test 4: `testTrendingFreshness()`
- Meri koliko brzo se trending videa ažurira
- Ponavlja zahtev nakon 1 sekunde
- Proverava stabilnost rezultata

### 3. **PowerShell Load Test Script** (`scripts/performance-test-trending.ps1`)

Za **API-level** performance testiranje bez JUnit:

```powershell
# Osnovna upotreba
.\scripts\performance-test-trending.ps1

# Sa prilagođenim URL-om
.\scripts\performance-test-trending.ps1 -BaseUrl "http://localhost:8080" -OutputDelay 100

# Rezultati se čuvaju u: trending_performance_results.csv
```

## Pre-Test Checklist

- [x] Backend server je pokrenut: `mvnw.cmd spring-boot:run`
- [x] PostgreSQL baza je dostupna
- [x] Seed skripte su pokrenute (Flyway migracije)
- [x] Verifikuj da su podaci učitani:
  ```sql
  SELECT COUNT(*) FROM videos;         -- Trebalo bi ~1000
  SELECT COUNT(*) FROM video_views;   -- Trebalo bi ~10000
  ```

## Pokretanje Testova

### Opcija 1: Pokretanje kroz JUnit (preporučeno za detaljne metrike)

```bash
# Pokretanje svih test scenarija
mvnw.cmd test -Dtest=LocalTrendingPerformanceTest

# Pokretanje samo jednog testa
mvnw.cmd test -Dtest=LocalTrendingPerformanceTest#testTrendingLatencyByRadius

# Pokretanje sa detaljnim izlazom
mvnw.cmd test -Dtest=LocalTrendingPerformanceTest -X
```

### Opcija 2: PowerShell Load Test (brže, jednostavnije)

```powershell
# Pokretanje testova sa rezultatima u CSV
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
.\scripts\performance-test-trending.ps1

# Otvoriti CSV rezultate u Excelu
Invoke-Item "trending_performance_results.csv"
```

### Opcija 3: DevTools Console (za brze provere)

U browser DevTools Console:

```javascript
// Jedan zahtev
(async () => {
  const t = performance.now();
  const r = await fetch('/api/trending/local?lat=45.2671&lon=19.8335&radiusKm=10');
  const ms = performance.now() - t;
  console.log('Latency:', ms, 'ms | Videos:', (await r.json()).length);
})();

// Brzi stress test (20 zahteva)
(async () => {
  const times = [];
  for (let i = 0; i < 20; i++) {
    const t = performance.now();
    await fetch('/api/trending/local?lat=45.2671&lon=19.8335&radiusKm=10');
    times.push(performance.now() - t);
  }
  const avg = times.reduce((a,b) => a+b) / times.length;
  console.log('Avg:', avg.toFixed(2), 'ms | Min:', Math.min(...times), 'ms | Max:', Math.max(...times), 'ms');
})();
```

## Očekivani Rezultati

Primer tabele rezultata koju trebate dobiti:

```
┌──────────────────────────┬─────────┬─────────┬─────────┬─────────┐
│ Scenario                 │ Avg (ms)│ Min (ms)│ Max (ms)│ P95 (ms)│
├──────────────────────────┼─────────┼─────────┼─────────┼─────────┤
│ 5km                      │    45.2 │      12 │     156 │      89 │
│ 10km                     │    67.8 │      28 │     234 │     145 │
│ 20km                     │   112.3 │      56 │     456 │     234 │
│ 50km                     │   234.5 │     123 │    1200 │     567 │
└──────────────────────────┴─────────┴─────────┴─────────┴─────────┘

Zaključak: Performanse rastu linerano sa radijusom. 
Geohash indeksiranje omogućava brzu pretragu čak i za velike radijuse.
```

## Analiza Rezultata

### Što očekujete vidjeti:

1. **Latency raste sa radijusom** - veći radijus = više geohash ćelija = više podataka za pretragu
2. **Concurrent zahtevi imaju veću latency** - ali throughput je veći
3. **P95 obično 2-3x veće od average** - normalno za DB sisteme
4. **Freshness** trebala bi biti konzistentna (ne dolazi do cache-a)

### Performance praga (ciljevi):

- **Idealno**: < 100ms (P95)
- **Dobro**: < 200ms (P95)
- **Prihvatljivo**: < 500ms (P95)
- **Loše**: > 500ms

## CSV Export i Grafički Prikazi

PowerShell test automatski pravi `trending_performance_results.csv`:

```csv
timestamp,scenario,label,time_ms,status,result_count
2024-02-01 10:30:00.123,Small Radius (5km),Beograd,42.5,OK,18
2024-02-01 10:30:01.234,Small Radius (5km),Autokomanda,48.3,OK,15
...
```

### Grafički prikazi (Excel/Google Sheets):

1. **Line Chart**: Response time trend kroz test
2. **Bar Chart**: Poređenje prosečne latencije između scenarija
3. **Scatter Plot**: Latency vs Radius
4. **Box Plot**: Min/Q1/Median/Q3/Max distribucija

## Troubleshooting

### Problem: "Trebalo bi da ima bar 100 videa"
- Seed skripte nisu pokrenute ili DB je resetovan
- Rešenje: Restartuj aplikaciju da se Flyway migracije ponovo izvrše

### Problem: Jako spora latency (> 1000ms)
- DB indeksi nisu kreirani
- Velik broj konkurentnih korisnika se testira
- Rešenje: Proverite `idx_views_geohash_time` indeks u bazi

### Problem: "Connection timeout"
- Backend nije pokrenut
- Pogrešan URL
- Rešenje: Verifikuj da je `http://localhost:8080` dostupan

## Kako prezentovati profesoru

1. **Metodologija**: Opisite kako ste testirali (JUnit, concurrent load, itd.)
2. **Tabele**: Prikaži rezultate u tabelama (Avg, P95, throughput)
3. **Grafici**: 2-3 grafikona (latency vs radius, concurrent impact, freshness)
4. **Zaključak**: "Sistem je optimalan jer:
   - P95 latency je < 200ms
   - Linearno skalira sa radiusom
   - Geohash indeksiranje efikasno"
5. **CSV Export**: Prikaži CSV sa svim merenjima kao dokaz

## Dodatni Resursi

- GeohashUtil.java: `src/main/java/com/example/demo/util/GeohashUtil.java`
- TrendingService: `src/main/java/com/example/demo/service/impl/TrendingServiceImpl.java`
- LocationTrendingController: `src/main/java/com/example/demo/controller/LocationTrendingController.java`
- DB Schema: `src/main/resources/db/migration/`

---

**Gotovo!** Sada imate kompletan test setup za performanse testiranje.
