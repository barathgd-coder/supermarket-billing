# Supermarket Billing and Inventory Management System

Spring Boot 3 (Java 17+) - Spring MVC, Spring Data JPA, Spring Security, Thymeleaf, Bootstrap 5, Chart.js.

## Run

Requirements: JDK 17+ and Maven 3.9+ (internet needed for Maven and for the Bootstrap / Chart.js CDN files in the browser).

```bash
mvn spring-boot:run          # H2 file database in ./data  (zero setup, default)
```
Open http://localhost:8080

| Role    | Username | Password   | Access |
|---------|----------|------------|--------|
| Admin   | admin    | admin123   | everything |
| Cashier | cashier  | cashier123 | Billing, Customers, Sales History |

### Use MySQL instead
```bash
export APP_PROFILE=mysql DB_USER=root DB_PASSWORD=yourpassword   # optional DB_HOST, DB_PORT, DB_NAME
mvn spring-boot:run
```
The database `supermarket` is created automatically and the tables are created by Hibernate.
`src/main/resources/db/schema.sql` and `db/data.sql` are reference scripts (optional, MySQL syntax).
Demo data (9 categories, 8 suppliers, 61 products, 5 customers, ~20 past sales) is seeded on first start by `DataSeeder`.

### Docker / hosting
See `DEPLOY.md` (Dockerfile, docker-compose.yml, render.yaml).

### Tests
```bash
mvn test
```
`BillingCalculatorTest` (totals, GST, discount, HALF_UP rounding) and `BillingServiceTest` (stock deduction,
insufficient-stock block, invoice numbers) run on an in-memory H2 database.

## Architecture
Controller -> Service -> Repository -> Entity/DTO, with global exception handling
(`WebExceptionHandler` for pages, `ApiExceptionHandler` for the POS JSON API).

```
com.supermart
  model/      entities and enums            repository/  Spring Data repositories
  dto/        forms and request/response    service/     business logic (BillingService = the sale transaction)
  billing/    BillingCalculator (pure BigDecimal maths)
  web/        Thymeleaf controllers         api/         POS JSON endpoints
  config/     SecurityConfig, DataSeeder    exception/   exceptions + global handlers
```

### Billing rules
* `BillingService.checkoutAt` is one `@Transactional` method: sale, sale items, payment, stock reduction and inventory log commit together or not at all.
* Products are row-locked (`PESSIMISTIC_WRITE`) in id order, so two cashiers cannot oversell; stock can never go negative; expired items cannot be sold.
* Invoice numbers `INV-YYYYMMDD-0001` come from a locked per-day counter row plus a UNIQUE constraint.
* Prices are tax-exclusive. Discount (percent or amount) is spread over the lines proportionally (last line takes the rounding remainder), then GST is added per line. All money is `BigDecimal`, scale 2, `RoundingMode.HALF_UP`.
* Deleting a product is a soft delete so old invoices keep working.

### POS keyboard shortcuts
`F2` search - `Enter` add (barcode scanners end with Enter; exact barcode wins) - `Up/Down` choose result - `F9` pay - `F8` clear cart - `Esc` clear search.

## ER diagram (description)
```
roles 1---* users 1---* sales *---1 customers (optional)
categories 1---* products *---1 suppliers (optional)
sales 1---* sale_items *---1 products
sales 1---1 payments
products 1---* inventory_transactions
settings (key/value)            invoice_counters (one row per day)
```
* `users.role_id -> roles.id`; `products.category_id -> categories.id`, `products.supplier_id -> suppliers.id`
* `sales.cashier_id -> users.id`, `sales.customer_id -> customers.id` (nullable = walk-in)
* `sale_items.sale_id -> sales.id`, `sale_items.product_id -> products.id` (product name, prices and GST are copied onto the line)
* `payments.sale_id -> sales.id` (unique), `inventory_transactions.product_id -> products.id` (signed `quantity_change` + `balance_after`)
* Indexes: product name/category, sale date, inventory date/product, plus UNIQUE on barcode, invoice number, username, customer phone.

## Notes
* Logo upload (Settings) accepts PNG/JPG/GIF; files are stored in `./uploads`.
* PDF invoices (OpenPDF) print the rupee sign as "Rs." because the built-in PDF fonts have no rupee glyph; the HTML invoice shows the configured symbol.
* Reports are aggregated in Java from the selected date range - fine for a single store; for very large histories move them to SQL GROUP BY queries.


## Barcode / QR scanning

Three ways to add an item: a USB scanner (types the code and presses Enter), typing a barcode or name, or a camera.

### Using your phone as the scanner (Iriun Webcam)
Iriun turns the phone into a normal webcam on the PC, so the browser sees it as one more camera.

1. Install **Iriun Webcam** on the phone and the **Iriun Webcam for PC** app on the computer (iriun.com).
2. Connect both to the same Wi-Fi, or use a USB cable. Open Iriun on the phone, then on the PC. The PC app should show the phone's picture.
3. Use **Chrome or Edge** and open `http://localhost:8080` (camera access works on `localhost` or HTTPS only).
4. Billing > **Camera** (or press `F4`). Allow the camera when the browser asks.
5. The scanner picks **Iriun Webcam** automatically and remembers your choice. To change it, use the camera list in the panel. If the phone connected after the page opened, press the refresh button.

Tips: hold the barcode 15-25 cm from the phone, flat, filling about half the frame. A virtual webcam cannot autofocus by itself, so tap the phone screen to focus. If the picture is dark or blurry, turn on a light. Mirrored picture is fine, the decoder reads it either way.

Camera panel problems:
* "Camera is busy": close Zoom, Teams or the Camera app, then press refresh.
* Iriun not in the list: check that the phone app is open, both devices are on the same network, and the PC app shows the phone.
* Permission blocked: click the camera icon in the address bar, allow it, press refresh.

The decoder (ZXing) is bundled in `static/js/vendor/zxing.min.js`, so scanning needs no internet. Bootstrap, icons, fonts and Chart.js still load from a CDN.
