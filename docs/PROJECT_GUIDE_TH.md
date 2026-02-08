# คู่มืออธิบายโปรเจค CryptoBoard (สำหรับผู้เริ่มต้น)

เอกสารนี้อธิบายโปรเจค CryptoBoard แบบละเอียดว่าแต่ละคลาสทำอะไร ใช้เทคนิคอะไร และไหลงานอย่างไร เหมาะสำหรับคนที่ยังไม่เคยทำ Spring WebFlux หรือ Reactive Programming

---

## 1. โปรเจคนี้คืออะไร

**CryptoBoard** คือเว็บแอปที่แสดง **ราคา Cryptocurrency แบบ Real-time** จาก Binance โดย:
- ดึงราคาจาก Binance API
- API ดึงราคาแบบ **non-blocking** (Mono/Flux) Frontend ใช้ polling อัปเดตเป็นระยะ
- มี REST API สำหรับดึงราคาแบบครั้งเดียวด้วย

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

**ทำไมใช้ WebFlux ไม่ใช้ Spring MVC?**
- WebFlux เป็น **Non-blocking** เหมาะกับงานที่รอ I/O บ่อย (เรียก API ภายนอก, Stream)
- ใช้ทรัพยากรน้อยกว่าเมื่อมี request เยอะ (non-blocking ไม่บล็อก thread)

---

## 3. โครงสร้างโปรเจค (โฟลเดอร์และความรับผิดชอบ)

```
src/main/java/.../CryptoBoard/
├── CryptoBoardApplication.java   → จุดเข้าแอป (main)
├── config/                       → การตั้งค่า Bean (WebClient, ObjectMapper)
├── controller/                   → รับ HTTP request ส่งต่อให้ Service
├── dto/                          → Data Transfer Object (รูปแบบข้อมูล)
└── service/                      → Logic การดึง/Stream ราคา
```

---

## 4. อธิบายแต่ละคลาสและเทคนิคที่ใช้

### 4.1 จุดเข้าแอปพลิเคชัน

#### `CryptoBoardApplication.java`

**ทำอะไร:**  
เป็นคลาสหลักที่รัน Spring Boot แค่ประกาศ `@SpringBootApplication` และเรียก `SpringApplication.run(...)`  

**เทคนิค:**
- **Spring Boot Auto-configuration** — Spring จะสแกน package นี้และ sub-package แล้วสร้าง Bean (Controller, Service, Config) ให้อัตโนมัติ

---

### 4.2 DTO (Data Transfer Object) — รูปแบบข้อมูล

#### `CryptoPrice.java`

**ทำอะไร:**  
เป็นโมเดลข้อมูลราคาที่ใช้ทั้งในระบบ (ไม่ยึดติดกับ Binance) เก็บ:
- `symbol` — สัญลักษณ์ เช่น BTCUSDT
- `price` — ราคา
- `change24h` — การเปลี่ยนแปลง 24 ชม. (%)
- `volume24h` — Volume 24 ชม.
- `timestamp` — เวลาที่ได้ข้อมูล

**เทคนิค:**
- **Plain DTO** — ไม่มี logic ซับซ้อน มีแค่ field + getter/setter
- **@JsonIgnoreProperties(ignoreUnknown = true)** — เวลา Jackson แปลง JSON เป็น object ถ้ามี field ที่ไม่มีในคลาสจะไม่ error (กัน API เปลี่ยนรูปแบบ)
- **หลาย Constructor** — ให้เลือกสร้างแบบสั้น (symbol+price) หรือแบบเต็ม ตาม use case

**ทำไมต้องมี DTO แยก?**  
เพื่อให้รูปแบบข้อมูลภายในแอปเป็นมาตรฐานเดียว ถ้าอนาคตเปลี่ยนไปดึงจาก CoinGecko หรือ Kraken ก็ยังส่ง `CryptoPrice` ออกไปได้ โดยไม่ให้ฝั่ง API/Frontend รู้ว่าแหล่งข้อมูลคืออะไร (ลด coupling)

---

#### `BinanceTicker.java`

**ทำอะไร:**  
เป็นโมเดลที่ตรงกับ **รูปแบบ JSON ที่ Binance API ส่งกลับมา** (เช่นจาก endpoint `/api/v3/ticker/24hr`) เก็บ field ตามที่ Binance ใช้

**เทคนิค:**
- **@JsonProperty("ชื่อ field ใน JSON")** — บอก Jackson ว่า field ใน JSON ชื่ออะไร map กับ field ตัวไหน (เช่น JSON เป็น `priceChangePercent` → เก็บใน `priceChangePercent`)
- **@JsonIgnoreProperties(ignoreUnknown = true)** — เหมือนใน CryptoPrice กันกรณี Binance เพิ่ม field ใหม่

**ทำไมต้องแยกจาก CryptoPrice?**  
เพราะ Binance ใช้ชื่อ field ไม่ตรงกับที่เราอยากใช้ในแอป (เช่น `priceChangePercent` ไม่ใช่ `change24h`) การมี BinanceTicker แยกไว้ทำให้การ “แปลงจาก Binance → CryptoPrice” อยู่ที่เดียว (ใน BinancePriceProvider) และถ้าเปลี่ยน Exchange ก็แค่มี DTO ใหม่ของ Exchange นั้น

---

### 4.3 Service Layer — Logic และการดึงข้อมูล

#### `CryptoPriceProvider.java` (Interface)

**ทำอะไร:**  
เป็น **interface** ที่กำหนดว่า “ผู้ให้บริการราคา” ต้องทำอะไรได้บ้าง:
- ดึงราคาเหรียญเดียว: `Mono<CryptoPrice> getPrice(String symbol)`
- ดึงราคาหลายเหรียญ: `Flux<CryptoPrice> getAllPrices()`
- บอกชื่อ Exchange: `String getExchangeName()`

**เทคนิค:**
- **Dependency Inversion (SOLID)** — โค้ดระดับสูง (เช่น GetCryptoPricesService) ขึ้นกับ interface นี้ ไม่ขึ้นกับ Binance โดยตรง จึงสลับหรือเพิ่ม Exchange ได้โดยไม่แก้โค้ดเดิม
- **Open/Closed Principle** — เพิ่ม Exchange ใหม่ = สร้าง class ใหม่ที่ implement interface นี้ ไม่ต้องแก้ไฟล์เดิม
- **Mono / Flux** — เป็น type จาก Project Reactor: Mono = ค่า 0 หรือ 1 ค่า, Flux = stream หลายค่า (ใช้กับ Reactive ทั้งหมด)

---

#### `BinancePriceProvider.java` (Implementation)

**ทำอะไร:**  
- **รับผิดชอบแค่การคุยกับ Binance API** (Single Responsibility)
- ใช้ `WebClient` เรียก `https://api.binance.com/api/v3/ticker/24hr`
- แปลง `BinanceTicker` → `CryptoPrice` แล้วส่งออกเป็น Mono/Flux

**เทคนิค:**
- **@Component** — ให้ Spring สร้าง Bean แล้ว inject ไปให้ GetCryptoPricesService (เพราะ GetCryptoPricesService รับ `CryptoPriceProvider` ซึ่งมี implementation เดียวคือ Binance)
- **Constructor Injection** — รับ `WebClient` กับ `crypto.symbols` จาก config (ไม่ใช้ new เอง) → ง่ายต่อการทดสอบและเปลี่ยน config
- **WebClient (Reactive)** — เรียก HTTP แบบ non-blocking ได้ Mono/Flux
- **Retry (reactor.util.retry.Retry)** — ถ้า request ล้มเหลว จะ retry แบบ backoff (ลองใหม่ 3 ครั้ง ห่างกัน 2 วินาที สูงสุด 10 วินาที)
- **timeout(Duration.ofSeconds(10))** — ถ้ารอเกิน 10 วินาทีถือว่าล้มเหลว
- **onErrorResume** — จับ error แล้วไม่ให้ sequence ขาด (เช่น return Mono.empty()) และ log ไว้
- **map(this::mapToCryptoPrice)** — แปลง BinanceTicker เป็น CryptoPrice ใน pipeline
- **Flux.fromIterable(symbols).flatMap(this::getPrice)** — วนดึงราคาแต่ละ symbol แบบ async แล้วรวมเป็น Flux

**สรุป:** คลาสนี้เป็น “ตัวเชื่อมระหว่าง Binance กับรูปแบบ CryptoPrice” แค่ชั้นเดียว ใครอยากเพิ่ม Exchange อื่นก็สร้างคลาสใหม่ที่ implement CryptoPriceProvider โดยไม่ต้องแก้ BinancePriceProvider

---

#### `GetCryptoPricesUseCase.java` (Interface)

**ทำอะไร:**  
กำหนดว่า “บริการดึงราคา” ต้องมี operations อะไรบ้าง:
- ดึงราคาทุกเหรียญ / เหรียญเดียวแบบครั้งเดียว (non-blocking)
- บอกสัญลักษณ์ที่รองรับ

**เทคนิค:**
- ฝั่ง Controller ขึ้นกับ interface นี้เท่านั้น ไม่รู้ว่า inside ใช้ Binance หรือที่อื่น (DIP)

---

#### `GetCryptoPricesService.java` (Implementation)

**ทำอะไร:**  
- **Orchestration** — ไม่ไปเรียก Binance เอง แต่เรียก `CryptoPriceProvider` (ซึ่งตอนรันจริงคือ BinancePriceProvider)
- ทำหน้าที่ “ดึงราคาครั้งเดียว” แบบ non-blocking ตามที่ Controller ขอ (ไม่มี streaming)

**เทคนิค:**
- **@Service** — Bean สำหรับ business logic
- **Dependency Inversion** — รับ `CryptoPriceProvider` (interface) ใน constructor
- **getAllPrices() / getPrice(symbol)** — delegate ไปที่ port โดยตรง คืนค่า Flux/Mono (non-blocking one-shot)

**สรุป:** GetCryptoPricesService ไม่รู้ว่า Binance เป็นอย่างไร แค่รู้ว่า “มี provider ที่ให้ Mono/Flux ของ CryptoPrice” จึงทดสอบง่าย (mock CryptoPriceProvider) และเปลี่ยนแหล่งข้อมูลได้โดยไม่แตะโค้ดนี้

---

### 4.4 Controller Layer — HTTP

#### `CryptoController.java`

**ทำอะไร:**  
- รับ HTTP request จาก Frontend / Client
- ดึงราคาทุกเหรียญ / เหรียญเดียว / รายการ symbol / ประวัติ แบบ **non-blocking** (Mono/Flux) ไม่มี streaming/SSE

**Endpoint สำคัญ:**

| Method | Path | ทำอะไร |
|--------|------|--------|
| GET | `/api/crypto/prices` | ดึงราคาทุกเหรียญครั้งเดียว (Flux) |
| GET | `/api/crypto/price/{symbol}` | ดึงราคาเหรียญเดียว (Mono) |
| GET | `/api/crypto/symbols` | รายการ symbol ที่รองรับ |
| GET | `/api/crypto/history/{symbol}` | ประวัติราคา (limit) |
| GET | `/api/crypto/history/{symbol}/range` | ประวัติราคาช่วงเวลา |

**เทคนิค:**
- **@RestController, @RequestMapping("/api/crypto")** — กำหนดว่าเป็น REST API ใต้ path นี้
- **@CrossOrigin(origins = "*")** — อนุญาตให้ Frontend จาก domain อื่นเรียก API ได้ (สำหรับพัฒนา)
- **symbol.toUpperCase() + "USDT"** — บังคับให้ symbol เป็นรูปแบบ Binance (เช่น btc → BTCUSDT)
- คืนค่า **Flux / Mono** — non-blocking ไม่บล็อก thread

**สรุป:** Controller รับผิดชอบแค่ HTTP layer (path, method, format) และ delegate logic ทั้งหมดให้ GetCryptoPricesUseCase / GetPriceHistoryUseCase (SRP)

---

#### `WebController.java`

**ทำอะไร:**  
ส่งหน้าเว็บหลักเมื่อเข้า path `/` (return ชื่อ view "index" → Spring จะไปหา `index.html` ใน static)

**เทคนิค:**
- **@Controller** (ไม่ใช่ RestController) — return เป็นชื่อ view สำหรับ render หน้า HTML
- กับ Spring Boot ที่มี static resources อยู่ที่ `static/` การ return "index" จะไปที่ `index.html` ได้ (ขึ้นกับ config)

---

### 4.5 Config

#### `WebClientConfig.java`

**ทำอะไร:**  
สร้าง Bean สองตัว:
1. **WebClient** — ใช้เรียก HTTP ภายนอก (Binance)
   - ตั้ง response timeout 10 วินาที
   - ใช้ Reactor Netty เป็น connector
   - ตั้ง max in-memory size 16MB สำหรับ response ใหญ่
2. **ObjectMapper** — ใช้ serialize/deserialize JSON (ถ้ามีที่อื่นในแอป)

**เทคนิค:**
- **@Configuration** — class นี้เป็นแหล่งกำหนด Bean
- **@Bean** — method ที่ return object ที่ Spring จะจัดการให้ (inject ได้ทุกที่ที่ต้องการ)

---

#### Database Config (ไม่มี config class แยก)

**ทำอะไร:**  
Spring Boot จัดการเชื่อมต่อ H2 ให้อัตโนมัติจาก `application.properties`:
- **DataSource (HikariCP)** — สร้าง connection pool ไปยัง H2 file database
- **schema.sql** — Spring Boot รัน `schema.sql` ตอนเริ่มแอปอัตโนมัติ (ตั้ง `spring.sql.init.mode=always`)

**เทคนิค:**
- **Spring Boot Auto-configuration** — ไม่ต้องเขียน config class เอง แค่ตั้งค่าใน `application.properties`
- **HikariCP** — connection pool ที่ Spring Boot ใช้เป็น default
- Spring Boot สร้าง `JdbcTemplate` จาก DataSource ให้อัตโนมัติ

---

### 4.6 การตั้งค่า

#### `application.properties`

**ทำอะไร:**  
- กำหนด port, ชื่อแอป, log
- **crypto.symbols** — รายการคู่เงินที่ต้องการดึง (เช่น BTCUSDT, ETHUSDT) **เพิ่ม/ลดได้โดยไม่ต้องแก้โค้ด (OCP)**
- **crypto.persistence.interval-ms** — ช่วงเวลาบันทึกราคาลง DB (มิลลิวินาที เช่น 5000 = ทุก 5 วินาที)

---

### 4.7 Frontend

#### `static/index.html`

**ทำอะไร:**  
- หน้า Dashboard แสดงการ์ดราคาแต่ละเหรียญ
- เรียก **fetch('/api/crypto/prices')** แล้ว **setInterval** poll ซ้ำทุก N วินาที (ไม่มี SSE)
- เมื่อได้ JSON array ของราคา ก็อัปเดตการ์ด (ราคา, % change, volume)

**เทคนิค:**
- **fetch + setInterval** — ดึงราคาครั้งเดียวต่อ request (server เป็น non-blocking)
- **formatPrice / formatNumber** — แสดงตัวเลขให้อ่านง่าย (เช่น K, M, B และทศนิยมตามขนาดราคา)

---

## 5. การไหลของข้อมูล (Data Flow)

ทุก endpoint เป็น **non-blocking** (ดึงครั้งเดียวต่อ request ไม่มี streaming):

1. Client เรียก GET `/api/crypto/prices` หรือ `/api/crypto/price/BTC`
2. **CryptoController** เรียก `getAllPrices()` หรือ `getPrice(symbol)` ของ GetCryptoPricesUseCase
3. **GetCryptoPricesService** เรียก `priceProvider.getAllPrices()` หรือ `getPrice(symbol)` (ไปที่ BinancePriceProvider)
4. BinancePriceProvider เรียก Binance API (non-blocking) → แปลงเป็น CryptoPrice → ส่งกลับเป็น Flux/Mono
5. Spring WebFlux แปลง Flux/Mono เป็น JSON response ให้ Client

Frontend ใช้ **polling** (fetch แล้ว setInterval) เพื่ออัปเดต UI เป็นระยะ

---

## 6. สรุปเทคนิคที่ใช้ในโปรเจค

| เทคนิค | ใช้ที่ไหน | ประโยชน์ |
|--------|-----------|----------|
| **SOLID – SRP** | แยก Controller / Service / Provider / DTO ตามหน้าที่เดียว | แก้ไขและทดสอบง่าย |
| **SOLID – OCP** | เพิ่ม Exchange = สร้าง Provider ใหม่ + config symbols | ไม่ต้องแก้โค้ดเดิม |
| **SOLID – DIP** | Controller ขึ้นกับ GetCryptoPricesUseCase; GetCryptoPricesService ขึ้นกับ CryptoPriceProvider (interface) | เปลี่ยน implementation ได้ ไม่ผูกกับ Binance |
| **Reactive (Mono/Flux)** | ทุกชั้นที่เกี่ยวกับ I/O | Non-blocking ไม่บล็อก thread |
| **WebClient + Retry + Timeout** | BinancePriceProvider | ดึงข้อมูลภายนอกอย่างปลอดภัยและทนต่อความล้มเหลว |
| **JDBC + Mono.fromCallable** | PriceHistoryPersistenceAdapter | เขียน/อ่าน DB ด้วย JdbcTemplate + wrap Mono.fromCallable ไม่บล็อก event loop |
| **Polling (fetch + setInterval)** | index.html | Frontend ดึงราคาซ้ำเป็นระยะ ไม่ใช้ SSE |
| **DTO แยก (CryptoPrice vs BinanceTicker)** | dto package | รูปแบบข้อมูลภายในไม่ยึดติดกับ Binance |
| **Constructor Injection** | ทุก Service / Controller | ทดสอบด้วย mock ได้ง่าย และ dependencies ชัดเจน |

### ตรวจ SOLID (หลัง refactor ประวัติราคา)

| หลัก | สถานะ | รายละเอียด |
|------|--------|------------|
| **S – Single Responsibility** | ผ่าน | แต่ละคลาสมีหน้าที่เดียว: Controller=HTTP, GetCryptoPricesService=orchestration, Provider=ดึงข้อมูล, GetPriceHistoryService=query, PriceStreamPersistence=scheduled บันทึก |
| **O – Open/Closed** | ผ่าน | เพิ่ม Exchange = สร้าง Provider ใหม่; เพิ่ม persistence = สร้าง Writer/Reader implementation ไม่แก้โค้ดเดิม |
| **L – Liskov Substitution** | ผ่าน | ไม่มี subtype ที่แทนที่แล้วทำพฤติกรรมผิด (ทุก implementation ใช้แทน interface ได้) |
| **I – Interface Segregation** | ผ่าน | แยก LoadPriceHistoryPort (อ่าน) กับ SavePriceHistoryPort (เขียน) — client ใช้แค่ interface ที่ต้องการ |
| **D – Dependency Inversion** | ผ่าน | Controller ขึ้นกับ GetCryptoPricesUseCase + GetPriceHistoryUseCase; PriceStreamPersistence ขึ้นกับ GetCryptoPricesUseCase + SavePriceHistoryPort; GetCryptoPricesService ขึ้นกับ CryptoPriceProvider (ทั้งหมดเป็น interface) |

---

## 7. ถ้าอยากขยายโปรเจค

- **เพิ่ม Exchange อื่น (เช่น CoinGecko):**  
  สร้าง `CoinGeckoPriceProvider` implement `CryptoPriceProvider` แล้วสร้าง DTO ตรงกับ API ของ CoinGecko จากนั้นเลือกใช้ provider ไหนผ่าน config หรือ Profile (เช่น @Primary / @Qualifier)

- **เพิ่มเหรียญ:**  
  แก้ใน `application.properties` ที่ `crypto.symbols` เท่านั้น ไม่ต้องแก้โค้ด

- **เปลี่ยนช่วงเวลาบันทึกราคาลง DB:**  
  แก้ `crypto.persistence.interval-ms` ใน application.properties

ถ้าอ่านครบแล้ว จะเห็นว่าโปรเจคออกแบบให้แต่ละชั้นรับผิดชอบชัดเจน ใช้ interface เป็นสัญญา และใช้ Reactive (Mono/Flux) แบบ non-blocking ตลอดสาย ดังนั้นถ้าเข้าใจแต่ละคลาสและ flow นี้ จะต่อยอดหรือสอนคนอื่นได้ง่ายครับ

---

## 8. การเก็บ Database (ประวัติราคา)

โปรเจครองรับการเก็บ snapshot ราคาลงฐานข้อมูลเพื่อใช้ดูประวัติหรือทำกราฟ

### สิ่งที่ใช้

| สิ่งที่ใช้ | หน้าที่ |
|-------------|--------|
| **spring-boot-starter-jdbc** + **h2** | JDBC + H2 file database เก็บข้อมูลในโฟลเดอร์ `data/` |
| **JdbcTemplate** | เขียน SQL ตรงๆ (INSERT/SELECT) wrap ด้วย `Mono.fromCallable` ไม่บล็อก event loop |
| **Domain: PriceSnapshot** | Domain entity สำหรับ snapshot ราคา (symbol, price, change_24h, volume_24h, recorded_at) |
| **Adapter: PriceHistoryPersistenceAdapter** | ใช้ JdbcTemplate เขียน SQL ตรงๆ + wrap Mono.fromCallable เพื่อบันทึก/อ่าน snapshot จาก DB — implement ทั้ง SavePriceHistoryPort และ LoadPriceHistoryPort |
| **Config: PriceStreamPersistence** | ใช้ @Scheduled เรียก getAllPrices() เป็นระยะ แล้วบันทึก snapshot ลง DB แบบ non-blocking (ปิดได้ด้วย `crypto.history.enabled=false`) |
| **schema.sql** | Spring Boot รันอัตโนมัติตอนเริ่มแอป สร้างตาราง `price_snapshot` ถ้ายังไม่มี |
| **API: GET /api/crypto/history/{symbol}?limit=100** | ดึงประวัติราคาล่าสุด (สำหรับกราฟ) |
| **API: GET /api/crypto/history/{symbol}/range?from=...&to=...** | ดึงประวัติในช่วงเวลา (from/to เป็น ISO-8601) |

### ทำไมใช้ JDBC แทน R2DBC?

R2DBC บน Spring Boot 4.x มีปัญหา **dialect resolution** — ระบบ resolve dialect ผิด ทำให้เกิด "bad SQL grammar" error แก้ยากเพราะ auto-configuration หลายชั้น

การใช้ JDBC + `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`:
- **ไม่บล็อก event loop** — JDBC blocking ถูก offload ไปรันบน elastic thread pool แยก
- ง่าย dependency น้อย ไม่มีปัญหา dialect
- ควบคุม SQL ได้เต็มที่ ไม่ต้องพึ่ง ORM

### Config ที่เกี่ยวข้อง (application.properties)

- `spring.datasource.url=jdbc:h2:file:./data/cryptoboard;AUTO_SERVER=TRUE` — เก็บข้อมูลในไฟล์ใต้โฟลเดอร์ `data/`
- `spring.sql.init.mode=always` — รัน schema.sql ทุกครั้งที่เริ่มแอป
- `crypto.history.enabled=true` — เปิด/ปิดการบันทึกราคาลง DB (scheduled)

### เทคนิคที่ใช้ (รวม SOLID)

- **JDBC + Mono.fromCallable** — ใช้ JdbcTemplate เขียน SQL ตรงๆ แล้ว wrap ด้วย `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` เพื่อไม่บล็อก Netty event loop  
- **แยกชั้น persistence** — การบันทึกอยู่ที่ PriceHistoryPersistenceAdapter และ PriceStreamPersistence ไม่ปนกับ GetCryptoPricesService  
- **บันทึกแบบ scheduled** — ใช้ `@Scheduled` เรียก getAllPrices() เป็นระยะ แล้ว saveAll() ลง DB แบบ non-blocking  
- **DIP + ISP** — Controller ขึ้นกับ `LoadPriceHistoryPort` (อ่านประวัติ), PriceStreamPersistence ขึ้นกับ `SavePriceHistoryPort` (เขียนประวัติ) ไม่ขึ้นกับ concrete PriceHistoryPersistenceAdapter โดยตรง
