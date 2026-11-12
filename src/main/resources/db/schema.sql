-- Reference MySQL schema (the application creates/updates the same structure automatically
-- through Hibernate ddl-auto=update, so running this script is OPTIONAL).
CREATE DATABASE IF NOT EXISTS supermarket CHARACTER SET utf8mb4;
USE supermarket;

CREATE TABLE IF NOT EXISTS roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  enabled BIT NOT NULL DEFAULT 1,
  role_id BIGINT NOT NULL,
  created_at DATETIME(6),
  CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);
CREATE TABLE IF NOT EXISTS categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS suppliers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  contact_number VARCHAR(20),
  email VARCHAR(120),
  address VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  barcode VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(150) NOT NULL,
  category_id BIGINT NOT NULL,
  supplier_id BIGINT,
  purchase_price DECIMAL(12,2) NOT NULL,
  selling_price DECIMAL(12,2) NOT NULL,
  quantity INT NOT NULL,
  gst_percent DECIMAL(5,2) NOT NULL,
  expiry_date DATE,
  reorder_level INT NOT NULL DEFAULT 10,
  active BIT NOT NULL DEFAULT 1,
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
  CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
  INDEX idx_product_name (name),
  INDEX idx_product_category (category_id)
);
CREATE TABLE IF NOT EXISTS customers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  phone VARCHAR(20) UNIQUE,
  created_at DATETIME(6)
);
CREATE TABLE IF NOT EXISTS sales (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(30) NOT NULL UNIQUE,
  sale_date DATETIME(6) NOT NULL,
  cashier_id BIGINT NOT NULL,
  customer_id BIGINT,
  customer_name VARCHAR(120),
  customer_phone VARCHAR(20),
  subtotal DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL,
  tax_total DECIMAL(12,2) NOT NULL,
  grand_total DECIMAL(12,2) NOT NULL,
  total_cost DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(10) NOT NULL,
  CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) REFERENCES users(id),
  CONSTRAINT fk_sale_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
  INDEX idx_sale_date (sale_date)
);
CREATE TABLE IF NOT EXISTS sale_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sale_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(150) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  purchase_price DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL,
  gst_percent DECIMAL(5,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  line_total DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_item_sale FOREIGN KEY (sale_id) REFERENCES sales(id),
  CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE TABLE IF NOT EXISTS payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sale_id BIGINT NOT NULL UNIQUE,
  payment_method VARCHAR(10) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  paid_at DATETIME(6) NOT NULL,
  reference VARCHAR(100),
  CONSTRAINT fk_payment_sale FOREIGN KEY (sale_id) REFERENCES sales(id)
);
CREATE TABLE IF NOT EXISTS inventory_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  txn_type VARCHAR(20) NOT NULL,
  quantity_change INT NOT NULL,
  balance_after INT NOT NULL,
  reference VARCHAR(100),
  note VARCHAR(255),
  created_by VARCHAR(50),
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_invtxn_product FOREIGN KEY (product_id) REFERENCES products(id),
  INDEX idx_invtxn_created (created_at),
  INDEX idx_invtxn_product (product_id)
);
CREATE TABLE IF NOT EXISTS settings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  setting_key VARCHAR(80) NOT NULL UNIQUE,
  setting_value VARCHAR(500)
);
CREATE TABLE IF NOT EXISTS invoice_counters (
  date_key VARCHAR(8) PRIMARY KEY,
  last_number INT NOT NULL
);
