-- Reference seed for MySQL (OPTIONAL: the application seeds the full demo data - 61 products,
-- suppliers, customers and sample sales - automatically on first start via DataSeeder).
-- Passwords below are BCrypt hashes: admin / admin123, cashier / cashier123
USE supermarket;
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_CASHIER');
INSERT IGNORE INTO users (username, password, full_name, enabled, role_id, created_at)
  SELECT 'admin', '$2a$10$7lmbX6Gq304GUlp7sv4kPOSPo31rDRF5Nt7Oio/tqDDTHDvZsjLYm', 'Store Administrator', 1, id, NOW(6) FROM roles WHERE name = 'ROLE_ADMIN';
INSERT IGNORE INTO users (username, password, full_name, enabled, role_id, created_at)
  SELECT 'cashier', '$2a$10$zDYEe3gixIJKK.aJQfigUO80UOCIU/icgfak8FU/MD50tkCaZdjI2', 'Counter Cashier', 1, id, NOW(6) FROM roles WHERE name = 'ROLE_CASHIER';
INSERT IGNORE INTO categories (name) VALUES ('Fruits & Vegetables'), ('Dairy & Eggs'), ('Bakery'), ('Beverages'),
  ('Snacks'), ('Staples & Grains'), ('Household'), ('Personal Care'), ('Frozen Foods');
