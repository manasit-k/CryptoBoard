# คู่มืออธิบายโปรเจค CryptoBoard (สำหรับผู้เริ่มต้น)

เอกสารนี้อธิบายโปรเจค CryptoBoard แบบละเอียดว่าแต่ละคลาสทำอะไร ใช้เทคนิคอะไร และไหลงานอย่างไร เหมาะสำหรับคนที่ยังไม่เคยทำ Spring WebFlux หรือ Reactive Programming

---

## 1. โปรเจคนี้คืออะไร

**CryptoBoard** คือเว็บแอปที่แสดง **ราคา Cryptocurrency แบบ Real-time** จาก Binance โดย:
- ดึงราคาจาก Binance API
- API ดึงราคาแบบ **non-blocking** (Mono/Flux) Frontend ใช้ polling อัปเดตเป็นระยะ
- มี REST API สำหรับดึงราคาแบบครั้งเดียวด้วย
- เก็บ snapshot ราคาลง H2 Database เพื่อดูประวัติ
- **ระบบ Login แบบ JWT** — API ราคา (/api/crypto/*) ต้องส่ง Header `Authorization: Bearer <token>` ถึงจะเข้าได้

---

## 2. เทคโนโลยีที่ใช้ (Tech Stack)

| ส่วน | เทคโนโลยี | ใช้ทำอะไร |
|------|-----------|-----------|
| Backend | **Spring Boot 4 + WebFlux** | สร้าง API แบบ Reactive (ไม่บล็อก thread) |
| HTTP Client | **WebClient** | เรียก Binance API แบบ non-blocking |
| Reactive | **Project Reactor** (Mono, Flux) | จัดการข้อมูลแบบ Stream / Async |
| Database | **JDBC + H2** (JdbcTemplate) | เขียน/อ่าน DB ด้วย SQL ตรงๆ wrap Mono.fromCallable ไม่บล็อก event loop |
| JSON | **Jackson** | แปลง JSON ↔ Java Object |
| Frontend | HTML + CSS + JavaScript | หน้า Dashboard + polling ราคา |
| Auth | **Spring Security (Reactive) + JWT (jjwt)** | Login/Register, ป้องกัน /api/crypto/* ด้วย Bearer token |

**ทำไมใช้ WebFlux ไม่ใช่ Spring MVC?**
- WebFlux เป็น **Non-blocking** เหมาะกับงานที่รอ I/O บ่อย (เรียก API ภายนอก, Stream)
- ใช้ทรัพยากรน้อยกว่าเมื่อมี request เยอะ (non-blocking ไม่บล็อก thread)

---

## 3. สถาปัตยกรรม (Hexagonal Architecture)

โปรเจคใช้สถาปัตยกรรมแบบ **Hexagonal (Ports & Adapters)** แบ่งชั้นชัดเจน:

```
src/main/java/com/demo/crypto/CryptoBoard/
├── CryptoBoardApplication.java              → จุดเข้าแอป (main)
├── domain/                                  → Domain Entity (pure POJO ไม่พึ่ง framework)
│   └── PriceSnapshot.java
├── application/                             → Application Layer (use case + port + dto)
│   ├── dto/
│   │   └── CryptoPrice.java                → DTO ราคาภายในระบบ
│   ├── port/
│   │   ├── in/                             → Inbound Ports (use case interface)
│   │   │   ├── GetCryptoPricesUseCase.java
│   │   │   └── GetPriceHistoryUseCase.java
│   │   └── out/                            → Outbound Ports (infrastructure interface)
│   │       ├── CryptoPriceProviderPort.java
│   │       ├── LoadPriceHistoryPort.java
│   │       └── SavePriceHistoryPort.java
│   └── service/                            → Use Case Implementation
│       ├── GetCryptoPricesService.java
│       └── GetPriceHistoryService.java
├── infrastructure/                          → Adapter สำหรับเชื่อมต่อภายนอก
│   ├── external/                           → External API Adapter
│   │   ├── BinancePriceProvider.java
│   │   └── BinanceTicker.java
│   └── persistence/                        → Database Adapter
│       └── PriceHistoryPersistenceAdapter.java
├── interfaces/                              → Inbound Adapter (HTTP)
│   └── web/
│       └── CryptoController.java
└── config/                                  → Configuration
    ├── WebClientConfig.java
    └── PriceStreamPersistence.java
```

**หลักการ:**
- **Domain** อยู่ตรงกลาง ไม่พึ่ง framework ใดๆ
- **Application** กำหนด use case (port in) และ port out สำหรับเรียกใช้ infrastructure
- **Infrastructure** implement port out (เช่น Binance adapter, DB adapter)
- **Interfaces** เป็น inbound adapter รับ HTTP request แล้วเรียก use case

---

## 4. อธิบายแต่ละคลาสและเทคนิคที่ใช้

### 4.1 จุดเข้าแอปพลิเคชัน

#### `CryptoBoardApplication.java`

**ทำอะไร:**  
เป็นคลาสหลักที่รัน Spring Boot ประกาศ `@SpringBootApplication` + `@EnableScheduling` และเรียก `SpringApplication.run(...)`

**เทคนิค:**
- **Spring Boot Auto-configuration** — Spring จะสแกน package นี้และ sub-package แล้วสร้าง Bean ให้อัตโนมัติ
- **@EnableScheduling** — เปิดใช้งาน `@Scheduled` สำหรับบันทึกราคาลง DB เป็นระยะ

---

### 4.2 Domain Layer

#### `PriceSnapshot.java`

**ทำอะไร:**  
เป็น Domain Entity สำหรับ snapshot ราคา Crypto ที่เก็บลง Database — เป็น pure POJO ไม่พึ่ง framework

**Field:**
- `id` (Long) — primary key จาก DB
- `symbol` (String) — สัญลักษณ์ เช่น BTCUSDT
- `price` (String) — ราคา
- `change24h` (String) — การเปลี่ยนแปลง 24 ชม. (%)
- `volume24h` (String) — Volume 24 ชม.
- `recordedAt` (Instant) — เวลาที่บันทึก

**เทคนิค:**
- **Pure POJO** — ไม่มี annotation ของ framework ใดๆ ใช้ได้ทุกชั้น
- **Static Factory Method** — `PriceSnapshot.withId(...)` สำหรับโหลดจาก DB (มี id)
- **Immutable fields** — field หลัก (symbol, price, etc.) เป็น `final` ไม่เปลี่ยนหลังสร้าง

---

### 4.3 Application Layer — DTO

#### `CryptoPrice.java`

**ทำอะไร:**  
เป็น DTO ราคาที่ใช้ภายในระบบ (ไม่ยึดติดกับ Binance) เก็บ:
- `symbol` — สัญลักษณ์ เช่น BTCUSDT
- `price` — ราคา
- `change24h` — การเปลี่ยนแปลง 24 ชม. (%)
- `volume24h` — Volume 24 ชม.
- `timestamp` — เวลาที่ได้ข้อมูล

**เทคนิค:**
- **@JsonIgnoreProperties(ignoreUnknown = true)** — เวลา Jackson แปลง JSON ถ้ามี field ที่ไม่มีในคลาสจะไม่ error
- **หลาย Constructor** — ให้เลือกสร้างแบบสั้น (symbol+price) หรือแบบเต็ม

**ทำไมต้องมี DTO แยก?**  
เพื่อให้รูปแบบข้อมูลภายในแอปเป็นมาตรฐานเดียว ถ้าอนาคตเปลี่ยนไปดึงจาก Exchange อื่น ก็ยังส่ง `CryptoPrice` ออกไปได้ (ลด coupling)

---

### 4.4 Application Layer — Inbound Ports (Use Case Interface)

#### `GetCryptoPricesUseCase.java`

**ทำอะไร:**  
กำหนดว่า "บริการดึงราคา" ต้องมี operations อะไรบ้าง:
- `Flux<CryptoPrice> getAllPrices()` — ดึงราคาทุกเหรียญ
- `Mono<CryptoPrice> getPrice(String symbol)` — ดึงราคาเหรียญเดียว
- `List<String> getAvailableSymbols()` — บอกสัญลักษณ์ที่รองรับ

**เทคนิค:**
- **Dependency Inversion** — Controller ขึ้นกับ interface นี้เท่านั้น ไม่รู้ implementation

---

#### `GetPriceHistoryUseCase.java`

**ทำอะไร:**  
กำหนดว่า "บริการประวัติราคา" ต้องมี operations อะไรบ้าง:
- `Flux<PriceSnapshot> findHistoryBySymbol(String symbol, int limit)` — ดึงประวัติราคาล่าสุด
- `Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to)` — ดึงประวัติในช่วงเวลา

---

### 4.5 Application Layer — Outbound Ports (Infrastructure Interface)

#### `CryptoPriceProviderPort.java`

**ทำอะไร:**  
กำหนดว่า "ผู้ให้บริการราคา" (Exchange adapter) ต้องทำอะไรได้บ้าง:
- `Mono<CryptoPrice> getPrice(String symbol)`
- `Flux<CryptoPrice> getAllPrices()`
- `String getExchangeName()`

**เทคนิค:**
- **Dependency Inversion (SOLID)** — Service ขึ้นกับ interface นี้ ไม่ขึ้นกับ Binance โดยตรง
- **Open/Closed Principle** — เพิ่ม Exchange ใหม่ = สร้าง class ใหม่ที่ implement port นี้

---

#### `SavePriceHistoryPort.java`

**ทำอะไร:**  
กำหนด interface สำหรับ **เขียน** ประวัติราคาลง DB:
- `Mono<PriceSnapshot> save(PriceSnapshot snapshot)`
- `Flux<PriceSnapshot> saveAll(Flux<PriceSnapshot> snapshots)`

---

#### `LoadPriceHistoryPort.java`

**ทำอะไร:**  
กำหนด interface สำหรับ **อ่าน** ประวัติราคาจาก DB:
- `Flux<PriceSnapshot> findHistoryBySymbol(String symbol, int limit)`
- `Flux<PriceSnapshot> findHistoryBySymbolBetween(String symbol, Instant from, Instant to)`

**เทคนิค (ISP):**  
แยก Save กับ Load เป็นคนละ interface — client ใช้แค่ interface ที่ต้องการ (Interface Segregation Principle)

---

### 4.6 Application Layer — Service (Use Case Implementation)

#### `GetCryptoPricesService.java`

**ทำอะไร:**  
implement `GetCryptoPricesUseCase` — ทำหน้าที่ orchestration โดยเรียก `CryptoPriceProviderPort` (ตอนรันจริงคือ BinancePriceProvider)

**เทคนิค:**
- **@Service** — Bean สำหรับ business logic
- **Dependency Inversion** — รับ `CryptoPriceProviderPort` (interface) ใน constructor
- **Constructor Injection** — รับ provider + symbols จาก config

---

#### `GetPriceHistoryService.java`

**ทำอะไร:**  
implement `GetPriceHistoryUseCase` — ดึงประวัติราคาจาก `LoadPriceHistoryPort`

**เทคนิค:**
- **Validation** — จำกัด limit สูงสุด 1000 และ default 100
- **Dependency Inversion** — รับ `LoadPriceHistoryPort` (interface) ใน constructor

---

### 4.7 Infrastructure Layer — External Adapter

#### `BinanceTicker.java`

**ทำอะไร:**  
เป็น DTO ที่ตรงกับ **รูปแบบ JSON ที่ Binance API ส่งกลับ** (จาก endpoint `/api/v3/ticker/24hr`)

**เทคนิค:**
- **@JsonProperty("ชื่อ field ใน JSON")** — บอก Jackson ว่า field ใน JSON map กับ field ตัวไหน
- **@JsonIgnoreProperties(ignoreUnknown = true)** — กัน Binance เพิ่ม field ใหม่แล้ว error

**ทำไมต้องแยกจาก CryptoPrice?**  
เพราะ Binance ใช้ชื่อ field ไม่ตรงกับที่เราอยากใช้ (เช่น `priceChangePercent` ไม่ใช่ `change24h`) การแยกทำให้ "แปลงจาก Binance → CryptoPrice" อยู่ที่เดียว

---

#### `BinancePriceProvider.java`

**ทำอะไร:**  
implement `CryptoPriceProviderPort` — รับผิดชอบแค่การคุยกับ Binance API (Single Responsibility)
- ใช้ `WebClient` เรียก `https://api.binance.com/api/v3/ticker/24hr`
- แปลง `BinanceTicker` → `CryptoPrice` แล้วส่งออกเป็น Mono/Flux

**เทคนิค:**
- **@Component** — ให้ Spring สร้าง Bean แล้ว inject ให้ Service
- **Constructor Injection** — รับ `WebClient` กับ `crypto.symbols` จาก config
- **WebClient (Reactive)** — เรียก HTTP แบบ non-blocking
- **Retry (reactor.util.retry.Retry)** — retry แบบ backoff (3 ครั้ง ห่างกัน 2 วินาที สูงสุด 10 วินาที)
- **timeout(Duration.ofSeconds(10))** — ถ้ารอเกิน 10 วินาทีถือว่าล้มเหลว
- **onErrorResume** — จับ error แล้วไม่ให้ sequence ขาด (return Mono.empty())
- **map(this::mapToCryptoPrice)** — แปลง BinanceTicker เป็น CryptoPrice ใน pipeline
- **Flux.fromIterable(symbols).flatMap(this::getPrice)** — วนดึงราคาแต่ละ symbol แบบ async

---

### 4.8 Infrastructure Layer — Persistence Adapter

#### `PriceHistoryPersistenceAdapter.java`

**ทำอะไร:**  
implement ทั้ง `SavePriceHistoryPort` และ `LoadPriceHistoryPort` — ใช้ JdbcTemplate เขียน SQL ตรงๆ เพื่อบันทึก/อ่าน snapshot จาก DB

**เทคนิค:**
- **@Component** — ให้ Spring สร้าง Bean
- **JdbcTemplate** — เขียน SQL ตรงๆ (INSERT/SELECT) ไม่ใช้ ORM
- **Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())** — wrap JDBC blocking call ไม่ให้บล็อก Netty event loop (offload ไปรันบน elastic thread pool)
- **onErrorResume** — จับ error ตอน save แล้วไม่ให้ flux ทั้งหมดล้มเหลว

---

### 4.9 Interfaces Layer — HTTP Adapter

#### `CryptoController.java`

**ทำอะไร:**  
รับ HTTP request จาก Frontend / Client แล้วเรียก use case — ไม่มี business logic

**Endpoint:**

| Method | Path | ทำอะไร |
|--------|------|--------|
| GET | `/api/crypto/prices` | ดึงราคาทุกเหรียญครั้งเดียว (Flux) |
| GET | `/api/crypto/price/{symbol}` | ดึงราคาเหรียญเดียว (Mono) |
| GET | `/api/crypto/symbols` | รายการ symbol ที่รองรับ |
| GET | `/api/crypto/history/{symbol}?limit=100` | ประวัติราคาล่าสุด |
| GET | `/api/crypto/history/{symbol}/range?from=...&to=...` | ประวัติราคาช่วงเวลา (ISO-8601) |

**เทคนิค:**
- **@RestController, @RequestMapping("/api/crypto")** — กำหนดว่าเป็น REST API
- **@CrossOrigin(origins = "*")** — อนุญาตให้ Frontend จาก domain อื่นเรียก API ได้
- **symbol.toUpperCase() + "USDT"** — บังคับให้ symbol เป็นรูปแบบ Binance (เช่น btc → BTCUSDT)
- คืนค่า **Flux / Mono** — non-blocking ไม่บล็อก thread

---

### 4.10 Config

#### `WebClientConfig.java`

**ทำอะไร:**  
สร้าง Bean สองตัว:
1. **WebClient** — ใช้เรียก HTTP ภายนอก (Binance)
   - ตั้ง response timeout 10 วินาที
   - ใช้ Reactor Netty เป็น connector
   - ตั้ง max in-memory size 16MB สำหรับ response ใหญ่
2. **ObjectMapper** — ใช้ serialize/deserialize JSON

**เทคนิค:**
- **@Configuration** — class นี้เป็นแหล่งกำหนด Bean
- **@Bean** — method ที่ return object ที่ Spring จะจัดการให้ (inject ได้ทุกที่)

---

#### `PriceStreamPersistence.java`

**ทำอะไร:**  
ใช้ `@Scheduled` เรียก `getAllPrices()` เป็นระยะ แล้วแปลง CryptoPrice → PriceSnapshot แล้วบันทึกลง DB ผ่าน `SavePriceHistoryPort`

**เทคนิค:**
- **@Component** — ให้ Spring สร้าง Bean
- **@Scheduled(fixedDelayString = "${crypto.persistence.interval-ms:5000}")** — รันทุก N มิลลิวินาที (ค่า default 5000)
- **เปิด/ปิดได้** ด้วย `crypto.history.enabled` (ถ้า false จะข้าม)
- ขึ้นกับ `GetCryptoPricesUseCase` + `SavePriceHistoryPort` (interface ทั้งคู่ = DIP)

---

#### Database Config (Auto-configuration)

Spring Boot จัดการเชื่อมต่อ H2 ให้อัตโนมัติจาก `application.properties`:
- **DataSource (HikariCP)** — สร้าง connection pool ไปยัง H2 file database
- **schema.sql** — Spring Boot รัน `schema.sql` ตอนเริ่มแอปอัตโนมัติ สร้างตาราง `price_snapshot`
- Spring Boot สร้าง `JdbcTemplate` จาก DataSource ให้อัตโนมัติ

---

### 4.11 การตั้งค่า

#### `application.properties`

| การตั้งค่า | ค่า | คำอธิบาย |
|------------|-----|----------|
| `server.port` | `8080` | พอร์ตแอป |
| `crypto.symbols` | `BTCUSDT,ETHUSDT,...` | รายการคู่เงินที่ดึง (เพิ่ม/ลดได้โดยไม่แก้โค้ด) |
| `crypto.history.enabled` | `true` | เปิด/ปิดการบันทึกราคาลง DB |
| `crypto.persistence.interval-ms` | `5000` | ช่วงเวลาบันทึกราคาลง DB (มิลลิวินาที) |
| `spring.datasource.url` | `jdbc:h2:file:./data/cryptoboard;AUTO_SERVER=TRUE` | H2 file database ในโฟลเดอร์ `data/` |
| `spring.sql.init.mode` | `always` | รัน schema.sql ทุกครั้งที่เริ่มแอป |

---

### 4.12 Frontend

#### `static/index.html`

**ทำอะไร:**  
- หน้า Dashboard แสดงการ์ดราคาแต่ละเหรียญ
- เรียก **fetch('/api/crypto/prices')** แล้ว **setInterval** poll ซ้ำทุก N วินาที
- เมื่อได้ JSON array ของราคา ก็อัปเดตการ์ด (ราคา, % change, volume)

**เทคนิค:**
- **fetch + setInterval** — ดึงราคาครั้งเดียวต่อ request (server เป็น non-blocking)
- **formatPrice / formatNumber** — แสดงตัวเลขให้อ่านง่าย (เช่น K, M, B และทศนิยมตามขนาดราคา)

---

## 5. การไหลของข้อมูล (Data Flow)

### 5.1 ดึงราคา (Non-blocking, One-shot)

```
Client → CryptoController → GetCryptoPricesUseCase → GetCryptoPricesService → CryptoPriceProviderPort → BinancePriceProvider → Binance API
                                                                                                                                    ↓
Client ← CryptoController ← GetCryptoPricesService ←───────────────────────── BinancePriceProvider ← BinanceTicker → CryptoPrice (map)
```

1. Client เรียก GET `/api/crypto/prices` หรือ `/api/crypto/price/{symbol}`
2. **CryptoController** เรียก use case
3. **GetCryptoPricesService** เรียก `priceProvider.getAllPrices()` หรือ `getPrice(symbol)`
4. **BinancePriceProvider** เรียก Binance API (non-blocking) → แปลง BinanceTicker → CryptoPrice
5. Spring WebFlux แปลง Flux/Mono เป็น JSON response ให้ Client

Frontend ใช้ **polling** (fetch + setInterval) เพื่ออัปเดต UI เป็นระยะ

### 5.2 บันทึกประวัติราคาลง DB (Scheduled)

```
@Scheduled (PriceStreamPersistence) → GetCryptoPricesUseCase.getAllPrices()
    ↓
CryptoPrice → PriceSnapshot (map) → SavePriceHistoryPort → PriceHistoryPersistenceAdapter → H2 DB
```

1. `PriceStreamPersistence.persistPrices()` ถูกเรียกทุก 5 วินาที
2. ดึงราคาทั้งหมดผ่าน `GetCryptoPricesUseCase`
3. แปลง CryptoPrice → PriceSnapshot
4. บันทึกลง DB ผ่าน `SavePriceHistoryPort` (PriceHistoryPersistenceAdapter ใช้ JdbcTemplate + Mono.fromCallable)

### 5.3 ดึงประวัติราคา

```
Client → CryptoController → GetPriceHistoryUseCase → GetPriceHistoryService → LoadPriceHistoryPort → PriceHistoryPersistenceAdapter → H2 DB
```

---

## 6. สรุปเทคนิคที่ใช้ในโปรเจค

| เทคนิค | ใช้ที่ไหน | ประโยชน์ |
|--------|-----------|----------|
| **Hexagonal Architecture** | โครงสร้างทั้งโปรเจค | แยกชั้นชัดเจน เปลี่ยน adapter ได้โดยไม่กระทบ business logic |
| **SOLID – SRP** | แยก Controller / Service / Provider / Adapter / DTO ตามหน้าที่เดียว | แก้ไขและทดสอบง่าย |
| **SOLID – OCP** | เพิ่ม Exchange = สร้าง Provider ใหม่ + config symbols | ไม่ต้องแก้โค้ดเดิม |
| **SOLID – LSP** | ทุก implementation ใช้แทน interface ได้ | พฤติกรรมคงเส้นคงวา |
| **SOLID – ISP** | แยก LoadPriceHistoryPort (อ่าน) กับ SavePriceHistoryPort (เขียน) | client ใช้แค่ interface ที่ต้องการ |
| **SOLID – DIP** | Controller ขึ้นกับ UseCase; Service ขึ้นกับ Port (ทั้งหมดเป็น interface) | เปลี่ยน implementation ได้ ไม่ผูกกับ Binance |
| **Reactive (Mono/Flux)** | ทุกชั้นที่เกี่ยวกับ I/O | Non-blocking ไม่บล็อก thread |
| **WebClient + Retry + Timeout** | BinancePriceProvider | ดึงข้อมูลภายนอกอย่างปลอดภัยและทนต่อความล้มเหลว |
| **JDBC + Mono.fromCallable** | PriceHistoryPersistenceAdapter | เขียน/อ่าน DB ด้วย JdbcTemplate + ไม่บล็อก event loop |
| **Polling (fetch + setInterval)** | index.html | Frontend ดึงราคาซ้ำเป็นระยะ |
| **DTO แยก (CryptoPrice vs BinanceTicker)** | application.dto / infrastructure.external | รูปแบบข้อมูลภายในไม่ยึดติดกับ Binance |
| **Constructor Injection** | ทุก Service / Controller / Adapter | ทดสอบด้วย mock ได้ง่าย dependencies ชัดเจน |

---

## 7. SOLID Checklist

| หลัก | สถานะ | รายละเอียด |
|------|--------|------------|
| **S – Single Responsibility** | ผ่าน | แต่ละคลาสมีหน้าที่เดียว: Controller=HTTP, Service=orchestration, Provider=ดึงข้อมูล, PersistenceAdapter=DB, PriceStreamPersistence=scheduled บันทึก |
| **O – Open/Closed** | ผ่าน | เพิ่ม Exchange = สร้าง Provider ใหม่; เพิ่ม persistence = สร้าง Adapter ใหม่ ไม่แก้โค้ดเดิม |
| **L – Liskov Substitution** | ผ่าน | ทุก implementation ใช้แทน interface ได้ ไม่มี subtype ที่ทำพฤติกรรมผิด |
| **I – Interface Segregation** | ผ่าน | แยก LoadPriceHistoryPort (อ่าน) กับ SavePriceHistoryPort (เขียน) |
| **D – Dependency Inversion** | ผ่าน | Controller ขึ้นกับ UseCase (port in); Service ขึ้นกับ Port (port out) — ทั้งหมดเป็น interface |

---

## 8. การเก็บ Database (ประวัติราคา)

### ทำไมใช้ JDBC แทน R2DBC?

R2DBC บน Spring Boot 4.x มีปัญหา **dialect resolution** — ระบบ resolve dialect ผิด ทำให้เกิด error แก้ยากเพราะ auto-configuration หลายชั้น

การใช้ JDBC + `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`:
- **ไม่บล็อก event loop** — JDBC blocking ถูก offload ไปรันบน elastic thread pool แยก
- ง่าย dependency น้อย ไม่มีปัญหา dialect
- ควบคุม SQL ได้เต็มที่ ไม่ต้องพึ่ง ORM

### ตาราง `price_snapshot` (schema.sql)

| Column | Type | คำอธิบาย |
|--------|------|----------|
| `id` | BIGINT AUTO_INCREMENT | Primary Key |
| `symbol` | VARCHAR | สัญลักษณ์ เช่น BTCUSDT |
| `price` | VARCHAR | ราคา |
| `change_24h` | VARCHAR | % เปลี่ยนแปลง 24 ชม. |
| `volume_24h` | VARCHAR | Volume 24 ชม. |
| `recorded_at` | TIMESTAMP | เวลาที่บันทึก |

Index: `idx_price_snapshot_symbol`, `idx_price_snapshot_recorded_at`

---

## 9. ถ้าอยากขยายโปรเจค

- **เพิ่ม Exchange อื่น (เช่น CoinGecko):**  
  สร้าง `CoinGeckoPriceProvider` implement `CryptoPriceProviderPort` + สร้าง DTO ตรงกับ API ของ CoinGecko จากนั้นเลือกใช้ provider ไหนผ่าน config (@Primary / @Qualifier)

- **เพิ่มเหรียญ:**  
  แก้ใน `application.properties` ที่ `crypto.symbols` เท่านั้น ไม่ต้องแก้โค้ด

- **เปลี่ยนช่วงเวลาบันทึกราคาลง DB:**  
  แก้ `crypto.persistence.interval-ms` ใน application.properties

- **ปิดการบันทึกประวัติ:**  
  ตั้ง `crypto.history.enabled=false`

---

## 9.1 ระบบ Login (JWT)

- **ลงทะเบียน:** `POST /api/auth/register` body `{"username":"...", "password":"..."}`
- **ล็อกอิน:** `POST /api/auth/login` body `{"username":"...", "password":"..."}` → ได้ `token` และ `type: "Bearer"`
- **เรียก API ราคา:** ส่ง Header `Authorization: Bearer <token>` ทุก request ไปที่ `/api/crypto/*`
- ผู้ใช้ทดสอบ (สร้างอัตโนมัติเมื่อ start): **demo** / **demo123**
- ตั้งค่า JWT ใน `application.properties`: `jwt.secret`, `jwt.expiration-ms`

---

## 10. Test

โปรเจคมี unit test ครอบคลุมชั้นหลัก:

| Test Class | ทดสอบอะไร |
|------------|-----------|
| `GetCryptoPricesServiceTest` | Service orchestration (mock CryptoPriceProviderPort) |
| `CryptoControllerTest` | HTTP layer (mock UseCase) |
| `BinancePriceProviderTest` | Binance adapter (mock WebClient) |
| `CryptoPriceTest` | DTO constructor, getter/setter, toString |
| `BinanceTickerTest` | DTO deserialization จาก JSON |
| `CryptoBoardApplicationTests` | Context loading |

ใช้ **Mockito** + **JUnit 5** + **@ParameterizedTest** สำหรับทดสอบหลาย case
