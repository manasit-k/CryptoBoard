# 🚀 Real-time Crypto Dashboard

โปรเจค Real-time Crypto Dashboard ที่ใช้ Spring WebFlux, WebClient, และ Server-Sent Events (SSE) เพื่อแสดงข้อมูลราคา Cryptocurrency แบบ Real-time โดยไม่ต้อง Refresh หน้าจอ

## ✨ คุณสมบัติ

- **Real-time Updates**: ข้อมูลราคาอัปเดตอัตโนมัติทุก 2 วินาทีผ่าน SSE
- **Non-blocking**: ใช้ WebClient และ Reactive Streams (Flux/Mono) สำหรับการทำงานแบบ Non-blocking
- **Server-Sent Events (SSE)**: ส่งข้อมูลจาก Server ไปหา Client แบบ Stream
- **Responsive UI**: หน้าจอที่สวยงามและใช้งานง่าย พร้อม Animation
- **Multiple Cryptocurrencies**: รองรับการติดตามหลายเหรียญพร้อมกัน (BTC, ETH, BNB, SOL, ADA, XRP, DOGE, DOT)

## 🛠️ เทคโนโลยีที่ใช้

- **Spring Boot 4.0.2** - Framework หลัก
- **Spring WebFlux** - Reactive Web Framework
- **WebClient** - Non-blocking HTTP Client สำหรับดึงข้อมูลจาก API
- **Server-Sent Events (SSE)** - สำหรับ Real-time Streaming
- **Reactor (Flux/Mono)** - Reactive Streams สำหรับจัดการการไหลของข้อมูล
- **Binance API** - Public API สำหรับดึงข้อมูลราคา Crypto

## 📋 ความต้องการของระบบ

- Java 17 หรือสูงกว่า
- Maven 3.6+ หรือสูงกว่า
- Internet Connection (สำหรับดึงข้อมูลจาก Binance API)

## 🚀 วิธีการรันโปรเจค

### 1. Clone หรือ Download โปรเจค

```bash
cd CryptoBoard
```

### 2. Build โปรเจคด้วย Maven

```bash
./mvnw clean install
```

### 3. Run แอปพลิเคชัน

```bash
./mvnw spring-boot:run
```

หรือ

```bash
java -jar target/CryptoBoard-0.0.1-SNAPSHOT.jar
```

### 4. เปิดเบราว์เซอร์

ไปที่: `http://localhost:8080`

## 📡 API Endpoints

### SSE Endpoints (Real-time Streaming)

- **GET** `/api/crypto/stream` - Stream ข้อมูลราคาทุกเหรียญที่ติดตาม
- **GET** `/api/crypto/stream/{symbol}` - Stream ข้อมูลราคาเหรียญเดียว (เช่น `/api/crypto/stream/BTC`)

### REST Endpoints

- **GET** `/api/crypto/prices` - ดึงข้อมูลราคาทุกเหรียญแบบ One-time
- **GET** `/api/crypto/price/{symbol}` - ดึงข้อมูลราคาเหรียญเดียว (เช่น `/api/crypto/price/BTC`)
- **GET** `/api/crypto/symbols` - ดูรายการสัญลักษณ์ที่รองรับ

## 🏗️ โครงสร้างโปรเจค

```
CryptoBoard/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/demo/crypto/CryptoBoard/
│   │   │       ├── CryptoBoardApplication.java
│   │   │       ├── config/
│   │   │       │   └── WebClientConfig.java      # WebClient Configuration
│   │   │       ├── controller/
│   │   │       │   ├── CryptoController.java     # REST & SSE Controllers
│   │   │       │   └── WebController.java        # Web Controller
│   │   │       ├── dto/
│   │   │       │   ├── CryptoPrice.java          # DTO สำหรับข้อมูลราคา
│   │   │       │   └── BinanceTicker.java        # DTO สำหรับ Binance API
│   │   │       └── service/
│   │   │           └── CryptoService.java        # Service สำหรับดึงข้อมูล
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html                    # Frontend Dashboard
│   │       └── application.properties
│   └── test/
└── pom.xml
```

## 🔧 การทำงานของระบบ

### 1. WebClient Configuration
- ตั้งค่า WebClient สำหรับดึงข้อมูลจาก Binance API แบบ Non-blocking
- กำหนด Timeout และ Retry Logic

### 2. CryptoService
- ใช้ WebClient ดึงข้อมูลราคาจาก Binance API
- สร้าง Flux สำหรับ Stream ข้อมูลราคาแบบ Real-time
- รองรับการดึงข้อมูลหลายเหรียญพร้อมกัน

### 3. CryptoController
- สร้าง SSE Endpoint สำหรับ Stream ข้อมูลไปหา Client
- แปลงข้อมูลเป็น JSON และส่งผ่าน ServerSentEvent
- รองรับ REST Endpoints สำหรับดึงข้อมูลแบบ One-time

### 4. Frontend (index.html)
- ใช้ EventSource API เชื่อมต่อกับ SSE Endpoint
- แสดงข้อมูลราคาแบบ Real-time พร้อม Animation
- Auto-reconnect เมื่อการเชื่อมต่อขาด

## 🎨 ฟีเจอร์ Frontend

- **Real-time Updates**: ข้อมูลอัปเดตอัตโนมัติทุก 2 วินาที
- **Connection Status**: แสดงสถานะการเชื่อมต่อ (Connected/Disconnected)
- **Price Cards**: แสดงข้อมูลราคาในรูปแบบ Card พร้อม Animation
- **Color Coding**: สีเขียวสำหรับราคาขึ้น สีแดงสำหรับราคาลง
- **Auto Reconnect**: เชื่อมต่อใหม่อัตโนมัติเมื่อการเชื่อมต่อขาด
- **Responsive Design**: รองรับหน้าจอทุกขนาด

## 🔍 การทดสอบ

### ทดสอบ SSE Endpoint ด้วย curl

```bash
curl -N http://localhost:8080/api/crypto/stream
```

### ทดสอบ REST Endpoint

```bash
# ดึงข้อมูลราคาทุกเหรียญ
curl http://localhost:8080/api/crypto/prices

# ดึงข้อมูลราคา BTC
curl http://localhost:8080/api/crypto/price/BTC

# ดูรายการสัญลักษณ์ที่รองรับ
curl http://localhost:8080/api/crypto/symbols
```

## 📝 การปรับแต่ง

### เปลี่ยนช่วงเวลาการอัปเดต

แก้ไขใน `CryptoController.java`:

```java
// เปลี่ยนจาก 2 วินาทีเป็น 5 วินาที
return cryptoService.streamCryptoPrices(Duration.ofSeconds(5));
```

### เพิ่มเหรียญใหม่ที่ต้องการติดตาม

แก้ไขใน `CryptoService.java`:

```java
private final List<String> symbols = Arrays.asList(
    "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", 
    "ADAUSDT", "XRPUSDT", "DOGEUSDT", "DOTUSDT",
    "MATICUSDT", "AVAXUSDT"  // เพิ่มเหรียญใหม่
);
```

## 🐛 Troubleshooting

### ปัญหา: ไม่สามารถเชื่อมต่อกับ Binance API ได้

**วิธีแก้**: ตรวจสอบ Internet Connection และ Firewall Settings

### ปัญหา: ข้อมูลไม่แสดงผล

**วิธีแก้**: 
1. ตรวจสอบ Console ใน Browser (F12) สำหรับ Error Messages
2. ตรวจสอบ Logs ของ Spring Boot Application
3. ทดสอบ API Endpoint ด้วย curl หรือ Postman

### ปัญหา: SSE Connection ถูกตัดบ่อย

**วิธีแก้**: 
- ตรวจสอบ Network Stability
- เพิ่ม Timeout ใน WebClient Configuration
- ตรวจสอบ Proxy/Firewall Settings

## 📚 เอกสารเพิ่มเติม

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Server-Sent Events (SSE) Specification](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [Binance API Documentation](https://binance-docs.github.io/apidocs/spot/en/)
- [Project Reactor Documentation](https://projectreactor.io/docs/core/release/reference/)

## 📄 License

This project is open source and available under the MIT License.

## 👨‍💻 Author

Created with ❤️ using Spring Boot and WebFlux
