package com.supermart.service;

import com.supermart.model.Setting;
import com.supermart.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SettingService {

    public static final String STORE_NAME = "store.name";
    public static final String ADDRESS = "store.address";
    public static final String PHONE = "store.phone";
    public static final String GSTIN = "store.gstin";
    public static final String LOGO = "store.logo";
    public static final String FOOTER = "invoice.footer";
    public static final String PREFIX = "invoice.prefix";
    public static final String CURRENCY = "currency.symbol";
    public static final String DEFAULT_GST = "gst.default";
    public static final String DEFAULT_REORDER = "stock.reorder.default";

    private final SettingRepository repository;
    private final String uploadDir;

    public SettingService(SettingRepository repository, @Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.repository = repository;
        this.uploadDir = uploadDir;
    }

    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return repository.findBySettingKey(key)
                .map(Setting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    @Transactional
    public void set(String key, String value) {
        Setting s = repository.findBySettingKey(key).orElseGet(() -> new Setting(key, null));
        s.setSettingValue(value == null ? "" : value);
        repository.save(s);
    }

    @Transactional
    public void ensureDefaults() {
        putIfMissing(STORE_NAME, "FreshMart Supermarket");
        putIfMissing(ADDRESS, "12 Market Road, Anna Nagar, Chennai - 600040");
        putIfMissing(PHONE, "+91 98765 43210");
        putIfMissing(GSTIN, "33ABCDE1234F1Z5");
        putIfMissing(FOOTER, "Thank you for shopping with us! Please visit again.");
        putIfMissing(PREFIX, "INV");
        putIfMissing(CURRENCY, "\u20B9");
        putIfMissing(DEFAULT_GST, "5");
        putIfMissing(DEFAULT_REORDER, "10");
    }

    private void putIfMissing(String key, String value) {
        if (repository.findBySettingKey(key).isEmpty()) {
            repository.save(new Setting(key, value));
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> storeInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", get(STORE_NAME, "Supermarket"));
        m.put("address", get(ADDRESS, ""));
        m.put("phone", get(PHONE, ""));
        m.put("gstin", get(GSTIN, ""));
        m.put("footer", get(FOOTER, "Thank you for shopping with us!"));
        m.put("prefix", get(PREFIX, "INV"));
        m.put("currency", get(CURRENCY, "\u20B9"));
        m.put("defaultGst", get(DEFAULT_GST, "5"));
        m.put("defaultReorder", get(DEFAULT_REORDER, "10"));
        return m;
    }

    public String invoicePrefix() { return get(PREFIX, "INV"); }

    public BigDecimal defaultGst() { return new BigDecimal(get(DEFAULT_GST, "5")); }

    public int defaultReorder() { return Integer.parseInt(get(DEFAULT_REORDER, "10")); }

    public Path uploadDirPath() { return Paths.get(uploadDir).toAbsolutePath().normalize(); }

    /** The uploaded logo file, if one was uploaded and still exists. */
    public Optional<Path> logoFile() {
        String name = get(LOGO, "");
        if (name.isBlank()) return Optional.empty();
        Path p = uploadDirPath().resolve(name).normalize();
        if (!p.startsWith(uploadDirPath()) || !Files.exists(p)) return Optional.empty();
        return Optional.of(p);
    }
}
