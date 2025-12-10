# 🦗 SmartSearch - Volume Testing z Locust

Sistemsko testiranje SmartSearch aplikacije z uporabo Locust za **Volume Testing**.

## 📋 Kazalo

- [Predpogoji](#predpogoji)
- [Namestitev](#namestitev)
- [Izvajanje testov](#izvajanje-testov)
- [Razlaga rezultatov](#razlaga-rezultatov)
- [Čiščenje testnih podatkov](#čiščenje-testnih-podatkov)

---

## 🔧 Predpogoji

### 1. Aplikacija mora teči

**POMEMBNO:** Pred testiranjem morate omogočiti MongoDB porte!

#### Korak 1: Dodajte MongoDB porte v docker-compose.prod.yml

Odprite `02_Backend/docker-compose.prod.yml` in dodajte naslednje vrstice pod MongoDB servis:

```yaml
mongodb:
  image: mongo:latest
  container_name: smartsearch-mongodb
  ports:
    - "27017:27017"  # ← Dodajte to vrstico za testiranje
  environment:
    MONGO_INITDB_DATABASE: smartsearch
```

#### Korak 2: Zaženite aplikacijo

```bash
# V mapi 02_Backend/
docker-compose -f docker-compose.prod.yml up -d
```

#### Korak 3: Preverite, ali backend deluje

```bash
curl http://localhost:8080/api/management/status
```

Pričakovan odgovor: `{"status":"OK","totalListings":...}`

#### ⚠️ POMEMBNO: Po testiranju odstranite porte!

Po zaključku testiranja **odstranite** linijo `- "27017:27017"` iz `docker-compose.prod.yml` in ponovno zaženite:

```bash
docker-compose -f docker-compose.prod.yml down
# Odstranite ports iz docker-compose.prod.yml
docker-compose -f docker-compose.prod.yml up -d
```

To je potrebno iz **varnostnih razlogov** - MongoDB ne sme biti javno dostopen v produkciji.

### 2. Potrebni programi

- **Python 3.8+** - [Prenesite tukaj](https://www.python.org/downloads/)
- **pip** - Package manager (vključen s Python)
- **curl** - Za preverjanje API-jev (opcijsko)

---

## 📦 Namestitev

### Windows:

```bash
# 1. Premaknite se v mapo testiranja
cd D:\00_School\03_Letnik\01_Zagotavljanje_Kakovosti\SmartSearch\04_SystemTesting

# 2. Kreirajte Python virtual environment (priporočeno)
python -m venv venv

# 3. Aktivirajte virtual environment
venv\Scripts\activate

# 4. Namestite dependence
pip install -r requirements.txt

# 5. Preverite namestitev
locust --version
python -c "import bs4; print('BeautifulSoup4 installed')"
```

Pričakovan izpis:
- `locust 2.20.0` (ali novejša verzija)
- `BeautifulSoup4 installed`

---

## 🚀 Quick Start - Kompletni workflow

**Za testiranje z 30,000 ciljnimi razpisi in max 30 uporabnikov:**

```bash
# 1. Priprava - dodajte MongoDB porte v docker-compose.prod.yml
# (Glej sekcijo Predpogoji)

# 2. Zaženite backend
cd ..\02_Backend
docker-compose -f docker-compose.prod.yml up -d
cd ..\04_SystemTesting

# 3. Namestite dependencies (enkrat)
pip install -r requirements.txt

# 4. Izvedite vse 4 teste (skupaj ~20 minut)
run_volume_test_ui.bat 10000 1 1   # 5 min - Baseline
run_volume_test_ui.bat 20000 1 1   # 5 min - Medium
run_volume_test_ui.bat 30000 1 1   # 5 min - Target volume
run_volume_test_ui.bat 50000 1 1   # 5 min - Stress test

# 5. Generirajte poročilo (avtomatsko!)
generate_report.bat

# 6. Preglejte generirano poročilo
# Odprite Volume_Testing_Report_*.md

# 7. Pretvorite v PDF (opcijsko)
pandoc Volume_Testing_Report_*.md -o Volume_Testing_Report.pdf

# 8. Cleanup - odstranite testne podatke
python cleanup_test_data.py

# 9. POMEMBNO - odstranite MongoDB porte iz docker-compose.prod.yml
```

**Čas:** ~25 minut (4x5min testi + 5min setup/cleanup)

---

## 🚀 Izvajanje testov

### Možnost 1: Avtomatski test (Headless mode)

**Enostaven test z defaultnimi vrednostmi:**

```bash
run_volume_test.bat
```

To bo:
- Generiralo **10,000** testnih listings
- Simuliralo **5** uporabnikov (realno za interno aplikacijo)
- Trajalo **5 minut**

**Test z custom parametri:**

```bash
run_volume_test.bat [volume] [users] [spawn_rate] [duration]
```

Primeri:

```bash
# Baseline: 10,000 listings, 5 uporabnikov, 1 user/sec, 5 minut
run_volume_test.bat 10000 5 1 5m

# Normal usage: 25,000 listings, 10 uporabnikov, 2 users/sec, 5 minut
run_volume_test.bat 25000 10 2 5m

# Maximum expected: 50,000 listings, 20 uporabnikov, 5 users/sec, 5 minut
run_volume_test.bat 50000 20 5 5m

# Stress test: 75,000 listings, 30 uporabnikov, 5 users/sec, 10 minut
run_volume_test.bat 75000 30 5 10m
```

---

### Možnost 2: Interaktivni test (Web UI mode) - **PRIPOROČENO**

**Zaženite Locust Web UI:**

```bash
run_volume_test_ui.bat 10000
```

To bo:
1. Generiralo 10,000 testnih listings
2. Odprlo Locust Web UI na `http://localhost:8089`

**V Web UI nastavite:**
- **Number of users**: 5-20 (realno število sočasnih uporabnikov za interno aplikacijo)
- **Spawn rate**: 1-5 (uporabniki/sekundo)
- **Host**: `http://localhost:8080` (že nastavljen)

Kliknite **Start swarming** in opazujte rezultate v realnem času!

---

### Možnost 3: Ročno izvajanje

```bash
# 1. Generirajte testne podatke
python VolumeData\generate_test_data.py 10000

# 2. Zaženite Locust
cd Locust
locust -f locustfile_volume.py --host=http://localhost:8080
```

Nato odprite browser: `http://localhost:8089`

---

## 📊 Volume Testing Scenariji

**POMEMBNO:** Scenariji so prilagojeni za interno aplikacijo z max **20 hkratnimi uporabniki** in max **50,000 razpisi**.

### Test 1: Baseline Volume (10,000 listings)

```bash
run_volume_test.bat 10000 5 1 5m
```

**Cilj:** Baseline performance z nizkim volumnom
**Uporabniki:** 5 (nizka obremenitev)
**Pričakovani rezultati:**
- Median odzivni čas: < 300ms
- 95th percentile: < 600ms
- Error rate: < 1%
- RPS: > 5

---

### Test 2: Normal Usage (25,000 listings)

```bash
run_volume_test.bat 25000 10 2 5m
```

**Cilj:** Normalna produkcijska obremenitev
**Uporabniki:** 10 (povprečna obremenitev)
**Pričakovani rezultati:**
- Median odzivni čas: < 500ms
- 95th percentile: < 1000ms
- Error rate: < 1%
- RPS: > 8

---

### Test 3: Maximum Expected Volume (50,000 listings)

```bash
run_volume_test.bat 50000 20 5 5m
```

**Cilj:** Maksimalen pričakovan volumen podatkov
**Uporabniki:** 20 (maksimalna realna obremenitev)
**Pričakovani rezultati:**
- Median odzivni čas: < 800ms
- 95th percentile: < 1500ms
- Error rate: < 1%
- RPS: > 10

---

### Test 4: Stress Test (75,000 listings)

```bash
run_volume_test.bat 75000 30 5 10m
```

**Cilj:** Testiranje sistema zunaj normalnih parametrov
**Uporabniki:** 30 (presežek kapacitete)
**Pričakovani rezultati:**
- Median odzivni čas: < 1500ms
- 95th percentile: < 3000ms
- Error rate: < 5%
- RPS: > 8
- Sistem ne sme odpasti

---

### Test 5: Izoliran Volume Test (brez load-a)

**Namen:** Izmeriti vpliv SAMO volumna podatkov brez vpliva sočasnosti

```bash
# 1 uporabnik, različni volumni
run_volume_test.bat 10000 1 1 3m
run_volume_test.bat 25000 1 1 3m
run_volume_test.bat 50000 1 1 3m
run_volume_test.bat 100000 1 1 3m
```

**Analiza:** Primerjajte odzivne čase med različnimi volumni - to pokaže, ali imate probleme z indeksi v MongoDB ali s performance iskanja.

---

## 📈 Razlaga rezultatov

### Web UI metrike:

Po zagonu testa boste videli:

#### 1. **Statistics Table**

| Type | Name | Requests | Fails | Median | 95%ile | Avg | RPS |
|------|------|----------|-------|--------|--------|-----|-----|
| GET | /api/listings/show/all | 1000 | 0 | 450ms | 890ms | 520ms | 10 |

**Razlaga:**
- **Requests**: Število izvedenih zahtevkov
- **Fails**: Število neuspelih zahtevkov (cilj: 0 ali < 1%)
- **Median**: Srednji odzivni čas (50% zahtevkov)
- **95%ile**: 95% zahtevkov je hitrejših od tega časa
- **Avg**: Povprečni odzivni čas
- **RPS**: Zahtevki na sekundo (throughput)

---

#### 2. **Charts (Grafi)**

- **Total Requests per Second**: Throughput skozi čas
- **Response Times**: Odzivni časi (median, 95th percentile)
- **Number of Users**: Število aktivnih uporabnikov

---

#### 3. **Response Time Distribution**

Graf prikazuje distribucijo odzivnih časov:
- **Zelena cona** (0-500ms): Odlično
- **Rumena cona** (500-1000ms): Dobro
- **Rdeča cona** (1000ms+): Počasno

---

### HTML Report

Po zaključku testa se avtomatsko generira HTML report v mapi `Results/`:

```
Results/
└── volume_test_report_10000_20250110_143022.html
```

Odprite ga v browserju za podrobno analizo.

---

## 🎯 Kriteriji uspešnosti

### ✅ Test je USPEŠEN, če:

1. **Error rate < 1%**
   - Skoraj vsi API klici so uspešni

2. **Median response time (prilagojeno za interno aplikacijo):**
   - 10,000 listings: < 300ms
   - 25,000 listings: < 500ms
   - 50,000 listings: < 800ms
   - 75,000 listings: < 1500ms

3. **95th percentile:**
   - Ne več kot 2x median

4. **Throughput (RPS):**
   - 5 users: > 5 RPS
   - 10 users: > 8 RPS
   - 20 users: > 10 RPS
   - Sistem vzdržuje konstanten throughput

5. **Sistem ne odpade:**
   - Docker containerji ostanejo aktivni
   - Ni Out-of-Memory errorjev
   - MongoDB povezave ostanejo stabilne

---

### ❌ Test je NEUSPEŠEN, če:

1. Error rate > 5%
2. Response time kontinuirano raste
3. Sistem odpade (crash)
4. Out of memory exceptions
5. Database connection timeouts

---

## 📄 Generiranje poročila

### Avtomatska generacija (PRIPOROČENO)

Po izvedbi vseh testov avtomatsko generirajte poročilo:

```bash
generate_report.bat
```

**Kaj naredi:**
1. Prebere vse HTML reporte iz mape `Results/`
2. Ekstrahira metrike (median RT, RPS, error rate, itd.)
3. Izračuna primerjave in odstotke
4. Generira markdown poročilo z vsemi podatki
5. Identificira najslabše endpointe
6. Analizira scalability

**Output:** `Volume_Testing_Report_YYYYMMDD_HHMMSS.md`

### Pretvorba v PDF

```bash
# Namestite pandoc: https://pandoc.org/installing.html
pandoc Volume_Testing_Report_*.md -o Volume_Testing_Report.pdf
```

---

## 🧹 Čiščenje testnih podatkov

Po testiranju odstranite testne podatke:

```bash
python cleanup_test_data.py
```

To bo izbrisalo vse listings z ID-ji, ki se začnejo s `TEST-`.

---

## 📊 Primerjava rezultatov

### Priporočena struktura za poročanje:

**Tabela 1: Realni scenariji (prilagojeno za interno uporabo)**

| Volume | Users | Median RT | 95th % | RPS | Error % | Status |
|--------|-------|-----------|--------|-----|---------|--------|
| 10,000 | 5 | 250ms | 480ms | 5.2 | 0% | ✅ PASS |
| 25,000 | 10 | 420ms | 850ms | 8.5 | 0% | ✅ PASS |
| 50,000 | 20 | 720ms | 1400ms | 10.8 | 0.5% | ✅ PASS |
| 75,000 | 30 | 1350ms | 2800ms | 8.9 | 2.1% | ⚠️ DEGRADED |

**Tabela 2: Izoliran volume test (1 uporabnik)**

| Volume | Median RT | 95th % | Razlika vs. 10k | Status |
|--------|-----------|--------|-----------------|--------|
| 10,000 | 180ms | 320ms | baseline | ✅ PASS |
| 25,000 | 280ms | 520ms | +55% | ✅ PASS |
| 50,000 | 450ms | 890ms | +150% | ✅ PASS |
| 100,000 | 950ms | 1850ms | +428% | ⚠️ SLOW |

---

## 🔍 Troubleshooting

### Problem: "Backend not running"

**Rešitev:**
```bash
cd ..\02_Backend
docker-compose up -d
```

---

### Problem: "ModuleNotFoundError: No module named 'locust'"

**Rešitev:**
```bash
pip install -r requirements.txt
```

---

### Problem: "Connection refused" med testom

**Možne vzroke:**
1. Backend se je sesul (preverite: `docker ps`)
2. MongoDB je prenehal delovati
3. Preveč sočasnih povezav

**Rešitev:**
```bash
docker-compose restart
```

---

### Problem: Zelo počasni odzivni časi

**Možni vzroki:**
1. Preveč testnih podatkov v MongoDB
2. Pomanjkanje RAM-a
3. Docker resource limits

**Rešitev:**
- Zmanjšajte število uporabnikov
- Povečajte Docker memory limit
- Očistite testne podatke

---

## 📚 Dodatni viri

- [Locust dokumentacija](https://docs.locust.io/)
- [MongoDB Performance Tips](https://www.mongodb.com/docs/manual/administration/analyzing-mongodb-performance/)
- [API Load Testing Best Practices](https://www.blazemeter.com/blog/api-load-testing)

---

## 🎓 Za študentski projekt

### Avtomatska generacija poročila

**PRIPOROČENO:** Uporabite avtomatski generator poročil!

```bash
# Po izvedbi vseh testov, avtomatsko generirajte poročilo
generate_report.bat
```

Script bo:
- ✅ Avtomatsko prebral vse HTML reporte iz `Results/` mape
- ✅ Izvlekel vse metrike (median RT, RPS, error rate, itd.)
- ✅ Izračunal primerjave in odstotke
- ✅ Generiral popolno markdown poročilo z vnešenimi podatki
- ✅ Identificiral najslabše endpointe
- ✅ Analiziral scalability

**Output:** `Volume_Testing_Report_YYYYMMDD_HHMMSS.md`

---

### Ročna predloga poročila (opcijsko)

Če želite ročno vnašati podatke, uporabite:
```
Volume_Testing_Report_Template.md
```

Predloga vsebuje:
- Strukturirano poročilo za volume testing
- Tabele za vnos rezultatov
- Prostor za grafe in screenshot-e
- Vnaprej pripravljene sekcije za analizo
- Priporočila za izboljšave

### Kaj vključiti v poročilo:

1. **Testni scenariji**: Katere volume level ste testirali
2. **Rezultati**: Tabela z metrikami
3. **Grafi**: Screenshots iz Locust Web UI
4. **Analiza**: Kje so bottlenecki?
5. **Priporočila**: Kako izboljšati performance?

### Primeri vprašanj za analizo:

- Ali sistem linearno skalira z volumnom podatkov? (primerjaj izoliran volume test)
- Pri katerem volumnu se performance začne degradirati?
- Kateri API endpoint je najpočasnejši? (GET /all vs. /open vs. /closed)
- Kako MongoDB indices vplivajo na performance?
- Ali je potrebno implementirati caching?
- Kakšna je razlika med odzivnimi časi pri 5 vs. 20 uporabnikih?
- Ali je razlika med volumni večja kot razlika med številom uporabnikov?

### Dodatne analize za poročilo:

1. **Volume vs. Performance graf**: Narišite graf median response time glede na volumen podatkov
2. **Concurrent users impact**: Primerjajte iste volume teste z različnim številom uporabnikov
3. **Bottleneck analiza**: Kateri del sistema je ozko grlo? (MongoDB queries, network, backend processing)
4. **Scalability factor**: Kolikokrat se upočasni sistem pri 5x večjem volumnu?

---

**Avtor:** Claude Code
**Verzija:** 1.0
**Datum:** 2025-01-10
