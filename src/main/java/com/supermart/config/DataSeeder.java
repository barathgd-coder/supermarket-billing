package com.supermart.config;

import com.supermart.dto.CartItemRequest;
import com.supermart.dto.CheckoutRequest;
import com.supermart.model.*;
import com.supermart.repository.*;
import com.supermart.service.BillingService;
import com.supermart.service.InventoryService;
import com.supermart.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Seeds roles, users, categories, suppliers, products, customers and a few past sales on first start. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingService settingService;
    private final InventoryService inventoryService;
    private final BillingService billingService;

    @Override
    @Transactional
    public void run(String... args) {
        settingService.ensureDefaults();
        seedRolesAndUsers();
        if (categoryRepository.count() == 0) seedCatalog();
        if (customerRepository.count() == 0) seedCustomers();
        if (saleRepository.count() == 0 && productRepository.count() > 0) seedSales();
    }

    private void seedRolesAndUsers() {
        Role admin = roleRepository.findByName(Role.ADMIN).orElseGet(() -> roleRepository.save(new Role(Role.ADMIN)));
        Role cashier = roleRepository.findByName(Role.CASHIER).orElseGet(() -> roleRepository.save(new Role(Role.CASHIER)));
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(user("admin", "admin123", "Store Administrator", admin));
        }
        if (!userRepository.existsByUsername("cashier")) {
            userRepository.save(user("cashier", "cashier123", "Counter Cashier", cashier));
        }
    }

    private User user(String username, String rawPassword, String fullName, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName);
        u.setRole(role);
        u.setEnabled(true);
        return u;
    }

    // ------------------------------------------------------------------ catalog

    private static final String[] CATEGORIES = {
            "Fruits & Vegetables", "Dairy & Eggs", "Bakery", "Beverages", "Snacks",
            "Staples & Grains", "Household", "Personal Care", "Frozen Foods"
    };

    private static final String[][] SUPPLIERS = {
            {"GreenLeaf Produce Co.", "9840011001", "orders@greenleaf.example", "Koyambedu Market, Chennai"},
            {"Sunrise Dairy Distributors", "9840011002", "sales@sunrisedairy.example", "Ambattur Industrial Estate, Chennai"},
            {"Golden Crust Bakers", "9840011003", "hello@goldencrust.example", "T. Nagar, Chennai"},
            {"Metro Beverages Pvt Ltd", "9840011004", "supply@metrobev.example", "Guindy, Chennai"},
            {"Savor Snacks Wholesale", "9840011005", "wholesale@savor.example", "Sowcarpet, Chennai"},
            {"National Grains & Staples", "9840011006", "trade@nationalgrains.example", "Vellore Road, Katpadi"},
            {"HomeCare Supplies Ltd", "9840011007", "b2b@homecare.example", "Perungudi, Chennai"},
            {"PureCare Personal Products", "9840011008", "care@purecare.example", "Porur, Chennai"}
    };

    /** name, category idx, purchase, selling, gst %, supplier idx, quantity, days to expiry (-1000 = none) */
    private static final Object[][] PRODUCTS = {
            // Fruits & Vegetables
            {"Bananas (1 kg)", 0, 30, 40, 0, 0, 80, 5},
            {"Apples Shimla (1 kg)", 0, 110, 140, 0, 0, 60, 10},
            {"Tomatoes (1 kg)", 0, 18, 28, 0, 0, 90, 6},
            {"Onions (1 kg)", 0, 22, 32, 0, 0, 120, 30},
            {"Potatoes (1 kg)", 0, 20, 30, 0, 0, 130, 30},
            {"Carrots (500 g)", 0, 16, 25, 0, 0, 3, 7},
            // Dairy & Eggs
            {"Full Cream Milk (1 L)", 1, 52, 60, 0, 1, 100, 3},
            {"Fresh Curd (400 g)", 1, 28, 35, 5, 1, 70, 8},
            {"Salted Butter (100 g)", 1, 52, 58, 12, 1, 45, 90},
            {"Cheese Slices (200 g)", 1, 120, 140, 12, 1, 35, 120},
            {"Paneer (200 g)", 1, 70, 85, 5, 1, 4, 10},
            {"Eggs (Pack of 12)", 1, 66, 78, 0, 1, 60, 14},
            {"Fruit Yoghurt Cup (100 g)", 1, 18, 25, 5, 1, 6, -3},
            {"Fresh Cream (200 ml)", 1, 48, 60, 12, 1, 5, -2},
            // Bakery
            {"Sandwich Bread (400 g)", 2, 32, 40, 5, 2, 50, 4},
            {"Whole Wheat Bread (400 g)", 2, 45, 55, 5, 2, 40, 4},
            {"Chocolate Muffin (Pack of 2)", 2, 30, 40, 18, 2, 30, 5},
            {"Butter Cookies (300 g)", 2, 55, 70, 18, 2, 60, 120},
            {"Rusk (300 g)", 2, 35, 45, 18, 2, 55, 150},
            {"Burger Buns (Pack of 4)", 2, 28, 38, 5, 2, 2, 3},
            // Beverages
            {"Cola (750 ml)", 3, 32, 40, 28, 3, 120, 240},
            {"Orange Juice (1 L)", 3, 85, 105, 12, 3, 40, 180},
            {"Packaged Drinking Water (1 L)", 3, 12, 20, 18, 3, 200, 365},
            {"Green Tea (25 bags)", 3, 105, 130, 5, 3, 50, 400},
            {"Instant Coffee (100 g)", 3, 190, 230, 18, 3, 45, 500},
            {"Energy Drink (250 ml)", 3, 80, 100, 28, 3, 8, 300},
            {"Lemon Soda (600 ml)", 3, 20, 30, 28, 3, 70, 200},
            // Snacks
            {"Potato Chips Salted (52 g)", 4, 17, 20, 12, 4, 150, 120},
            {"Masala Peanuts (150 g)", 4, 30, 40, 12, 4, 80, 150},
            {"Milk Chocolate Bar (40 g)", 4, 32, 40, 18, 4, 100, 200},
            {"Instant Noodles (70 g)", 4, 11, 14, 12, 4, 180, 240},
            {"Salted Biscuits (200 g)", 4, 24, 30, 18, 4, 90, 180},
            {"Roasted Almonds (200 g)", 4, 190, 240, 12, 4, 25, 250},
            {"Butter Popcorn (90 g)", 4, 28, 35, 12, 4, 5, 150},
            // Staples & Grains
            {"Basmati Rice (5 kg)", 5, 420, 499, 5, 5, 40, 500},
            {"Wheat Flour / Atta (5 kg)", 5, 210, 249, 0, 5, 55, 200},
            {"Toor Dal (1 kg)", 5, 135, 160, 0, 5, 60, 300},
            {"Sugar (1 kg)", 5, 42, 50, 0, 5, 100, -1000},
            {"Iodised Salt (1 kg)", 5, 18, 24, 0, 5, 110, -1000},
            {"Sunflower Oil (1 L)", 5, 120, 145, 5, 5, 70, 300},
            {"Turmeric Powder (200 g)", 5, 52, 70, 5, 5, 45, 400},
            {"Red Chilli Powder (200 g)", 5, 70, 90, 5, 5, 40, 400},
            {"Tea Powder (500 g)", 5, 190, 230, 5, 5, 38, 400},
            // Household
            {"Dishwash Liquid (500 ml)", 6, 78, 99, 18, 6, 50, -1000},
            {"Laundry Detergent (1 kg)", 6, 105, 130, 18, 6, 45, -1000},
            {"Floor Cleaner (1 L)", 6, 120, 155, 18, 6, 30, -1000},
            {"Toilet Cleaner (500 ml)", 6, 72, 95, 18, 6, 35, -1000},
            {"Garbage Bags (Pack of 30)", 6, 60, 80, 18, 6, 4, -1000},
            {"Kitchen Tissue Roll", 6, 70, 95, 18, 6, 28, -1000},
            // Personal Care
            {"Bath Soap (100 g)", 7, 28, 38, 18, 7, 150, -1000},
            {"Shampoo (180 ml)", 7, 95, 125, 18, 7, 50, 700},
            {"Toothpaste (150 g)", 7, 62, 80, 18, 7, 80, 700},
            {"Toothbrush Medium", 7, 22, 30, 18, 7, 90, -1000},
            {"Hand Wash (250 ml)", 7, 55, 75, 18, 7, 60, 600},
            {"Face Wash (100 ml)", 7, 110, 145, 18, 7, 35, 600},
            {"Deodorant (150 ml)", 7, 135, 175, 18, 7, 28, 800},
            // Frozen Foods
            {"Frozen Green Peas (500 g)", 8, 70, 90, 5, 1, 40, 200},
            {"French Fries (420 g)", 8, 95, 125, 12, 1, 30, 180},
            {"Chicken Nuggets (500 g)", 8, 210, 265, 12, 1, 25, 150},
            {"Vanilla Ice Cream (1 L)", 8, 140, 180, 18, 1, 20, 200},
            {"Frozen Parathas (Pack of 5)", 8, 75, 100, 18, 1, 30, 150}
    };

    /**
     * Builds a valid EAN-13 from a 12-digit body by appending the GS1 check digit.
     * Phone cameras, BarcodeDetector, ZXing and USB scanners all validate this digit, so a
     * made-up 13-digit number without a correct check digit is rejected and never scans.
     */
    static String ean13(String body12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int d = body12.charAt(i) - '0';
            sum += (i % 2 == 0) ? d : d * 3;
        }
        return body12 + ((10 - sum % 10) % 10);
    }

    private final List<String> sellableBarcodes = new ArrayList<>();

    private void seedCatalog() {
        List<Category> categories = new ArrayList<>();
        for (String c : CATEGORIES) categories.add(categoryRepository.save(new Category(c)));

        List<Supplier> suppliers = new ArrayList<>();
        for (String[] s : SUPPLIERS) {
            Supplier sup = new Supplier();
            sup.setName(s[0]);
            sup.setContactNumber(s[1]);
            sup.setEmail(s[2]);
            sup.setAddress(s[3]);
            suppliers.add(supplierRepository.save(sup));
        }

        int seq = 1;
        for (Object[] row : PRODUCTS) {
            Product p = new Product();
            p.setBarcode(ean13("8904100" + String.format("%05d", seq++)));
            p.setName((String) row[0]);
            p.setCategory(categories.get((Integer) row[1]));
            p.setPurchasePrice(new BigDecimal((Integer) row[2]).setScale(2));
            p.setSellingPrice(new BigDecimal((Integer) row[3]).setScale(2));
            p.setGstPercent(new BigDecimal((Integer) row[4]).setScale(2));
            p.setSupplier(suppliers.get((Integer) row[5]));
            p.setReorderLevel(10);
            int days = (Integer) row[7];
            p.setExpiryDate(days == -1000 ? null : LocalDate.now().plusDays(days));
            p.setQuantity(0);
            p = productRepository.save(p);

            int qty = (Integer) row[6];
            inventoryService.record(p, TransactionType.STOCK_IN, qty, "Opening stock", "Seed data", "system",
                    LocalDateTime.now().minusDays(8));
            if (qty >= 40 && (days == -1000 || days > 20)) sellableBarcodes.add(p.getBarcode());
        }
        log.info("Seeded {} categories, {} suppliers, {} products", categories.size(), suppliers.size(), PRODUCTS.length);
    }

    private void seedCustomers() {
        String[][] customers = {
                {"Priya Sharma", "9876500001"}, {"Rahul Verma", "9876500002"}, {"Anita Desai", "9876500003"},
                {"Mohammed Irfan", "9876500004"}, {"Lakshmi Narayanan", "9876500005"}
        };
        for (String[] c : customers) {
            Customer cu = new Customer();
            cu.setName(c[0]);
            cu.setPhone(c[1]);
            customerRepository.save(cu);
        }
    }

    // ------------------------------------------------------------------ sample sales

    private void seedSales() {
        if (sellableBarcodes.isEmpty()) {
            productRepository.findAll().stream()
                    .filter(p -> p.getQuantity() >= 40 && !p.isExpired())
                    .forEach(p -> sellableBarcodes.add(p.getBarcode()));
        }
        List<Product> sellable = new ArrayList<>();
        for (String b : sellableBarcodes) productRepository.findByBarcode(b).ifPresent(sellable::add);
        if (sellable.isEmpty()) return;

        List<Customer> customers = customerRepository.findAll();
        PaymentMethod[] methods = PaymentMethod.values();
        Random random = new Random(7);
        int created = 0;

        for (int daysAgo = 6; daysAgo >= 0; daysAgo--) {
            int bills = daysAgo == 0 ? 2 : 3;
            for (int b = 0; b < bills; b++) {
                CheckoutRequest req = new CheckoutRequest();
                req.setPaymentMethod(methods[random.nextInt(methods.length)]);
                if (random.nextInt(3) > 0) {
                    Customer c = customers.get(random.nextInt(customers.size()));
                    req.setCustomerName(c.getName());
                    req.setCustomerPhone(c.getPhone());
                }
                if (random.nextInt(4) == 0) {
                    req.setDiscountType(DiscountType.PERCENT);
                    req.setDiscountValue(new BigDecimal(5));
                }
                int lines = 2 + random.nextInt(4);
                List<Product> chosen = new ArrayList<>(sellable);
                java.util.Collections.shuffle(chosen, random);
                for (int i = 0; i < Math.min(lines, chosen.size()); i++) {
                    CartItemRequest item = new CartItemRequest();
                    item.setProductId(chosen.get(i).getId());
                    item.setQuantity(1 + random.nextInt(4));
                    req.getItems().add(item);
                }
                LocalDateTime when = LocalDateTime.now().minusDays(daysAgo)
                        .withHour(9 + random.nextInt(10)).withMinute(random.nextInt(60)).withSecond(0).withNano(0);
                if (when.isAfter(LocalDateTime.now())) when = LocalDateTime.now().minusMinutes(5 + b);
                billingService.checkoutAt(req, b % 2 == 0 ? "cashier" : "admin", when);
                created++;
            }
        }
        log.info("Seeded {} sample sales", created);
    }
}
