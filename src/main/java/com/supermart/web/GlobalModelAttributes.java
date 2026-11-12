package com.supermart.web;

import com.supermart.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

/** Makes the store branding (name, logo, currency...) available to every page as ${store}. */
@ControllerAdvice(basePackages = "com.supermart.web")
@RequiredArgsConstructor
public class GlobalModelAttributes {

    private final SettingService settingService;

    @ModelAttribute("store")
    public Map<String, Object> store() {
        Map<String, Object> store = settingService.storeInfo();
        store.put("logoVersion", settingService.logoFile().map(p -> p.toFile().lastModified()).orElse(0L));
        return store;
    }
}
