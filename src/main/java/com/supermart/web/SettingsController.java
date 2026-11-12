package com.supermart.web;

import com.supermart.dto.SettingsForm;
import com.supermart.dto.UserForm;
import com.supermart.exception.BusinessException;
import com.supermart.model.Role;
import com.supermart.service.CategoryService;
import com.supermart.service.SettingService;
import com.supermart.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private static final Map<String, String> LOGO_TYPES = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/gif", "gif");

    private final SettingService settingService;
    private final CategoryService categoryService;
    private final UserService userService;

    // ------------------------------------------------------------------ store settings

    @GetMapping("/settings")
    public String index(Model model) {
        return settingsView(model, currentSettings());
    }

    @PostMapping("/settings/store")
    public String saveStore(@Valid @ModelAttribute("form") SettingsForm form, BindingResult result,
                            Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return settingsView(model, form);
        }
        settingService.set(SettingService.STORE_NAME, form.getStoreName().trim());
        settingService.set(SettingService.ADDRESS, form.getAddress());
        settingService.set(SettingService.PHONE, form.getPhone());
        settingService.set(SettingService.GSTIN, form.getGstin());
        settingService.set(SettingService.DEFAULT_GST, form.getDefaultGst().stripTrailingZeros().toPlainString());
        settingService.set(SettingService.DEFAULT_REORDER, String.valueOf(form.getDefaultReorderLevel()));
        settingService.set(SettingService.PREFIX, form.getInvoicePrefix().trim().toUpperCase());
        settingService.set(SettingService.FOOTER, form.getInvoiceFooter());
        settingService.set(SettingService.CURRENCY, form.getCurrency().trim());
        redirect.addFlashAttribute("success", "Settings saved");
        return "redirect:/settings";
    }

    @PostMapping("/settings/logo")
    public String uploadLogo(@RequestParam("file") MultipartFile file, RedirectAttributes redirect) {
        String ext = LOGO_TYPES.get(file.getContentType());
        if (file.isEmpty() || ext == null) {
            redirect.addFlashAttribute("error", "Please choose a PNG, JPG or GIF image.");
            return "redirect:/settings";
        }
        try {
            Path dir = settingService.uploadDirPath();
            Files.createDirectories(dir);
            for (String e : LOGO_TYPES.values()) Files.deleteIfExists(dir.resolve("logo." + e));
            Files.copy(file.getInputStream(), dir.resolve("logo." + ext), StandardCopyOption.REPLACE_EXISTING);
            settingService.set(SettingService.LOGO, "logo." + ext);
            redirect.addFlashAttribute("success", "Logo updated");
        } catch (IOException ex) {
            redirect.addFlashAttribute("error", "Could not store the logo: " + ex.getMessage());
        }
        return "redirect:/settings";
    }

    /** Public: the login page and invoices show the logo. */
    @GetMapping("/logo")
    public ResponseEntity<Resource> logo() {
        Path file = settingService.logoFile().orElse(null);
        if (file == null) {
            return ResponseEntity.ok().contentType(MediaType.valueOf("image/svg+xml"))
                    .cacheControl(CacheControl.noCache())
                    .body(new ClassPathResource("static/img/logo.svg"));
        }
        String name = file.getFileName().toString();
        MediaType type = name.endsWith(".png") ? MediaType.IMAGE_PNG
                : name.endsWith(".gif") ? MediaType.IMAGE_GIF : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(type).cacheControl(CacheControl.noCache())
                .body(new FileSystemResource(file));
    }

    // ------------------------------------------------------------------ categories

    @PostMapping("/settings/categories")
    public String addCategory(@RequestParam String name, RedirectAttributes redirect) {
        try {
            categoryService.add(name);
            redirect.addFlashAttribute("success", "Category added");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/settings/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            categoryService.delete(id);
            redirect.addFlashAttribute("success", "Category deleted");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/settings";
    }

    // ------------------------------------------------------------------ users (Admin only)

    @GetMapping("/settings/users")
    public String users(@RequestParam(required = false) Long edit, Model model) {
        return usersView(model, edit != null ? userService.toForm(edit) : new UserForm());
    }

    @PostMapping("/settings/users/save")
    public String saveUser(@Valid @ModelAttribute("form") UserForm form, BindingResult result,
                           Model model, RedirectAttributes redirect, Principal principal) {
        if (result.hasErrors()) {
            return usersView(model, form);
        }
        try {
            userService.save(form, principal.getName());
        } catch (BusinessException ex) {
            result.reject("business", ex.getMessage());
            return usersView(model, form);
        }
        redirect.addFlashAttribute("success", "User saved");
        return "redirect:/settings/users";
    }

    // ------------------------------------------------------------------ helpers

    private SettingsForm currentSettings() {
        SettingsForm f = new SettingsForm();
        f.setStoreName(settingService.get(SettingService.STORE_NAME, ""));
        f.setAddress(settingService.get(SettingService.ADDRESS, ""));
        f.setPhone(settingService.get(SettingService.PHONE, ""));
        f.setGstin(settingService.get(SettingService.GSTIN, ""));
        f.setDefaultGst(settingService.defaultGst());
        f.setDefaultReorderLevel(settingService.defaultReorder());
        f.setInvoicePrefix(settingService.invoicePrefix());
        f.setInvoiceFooter(settingService.get(SettingService.FOOTER, ""));
        f.setCurrency(settingService.get(SettingService.CURRENCY, "\u20B9"));
        return f;
    }

    private String settingsView(Model model, SettingsForm form) {
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.list());
        return "settings/index";
    }

    private String usersView(Model model, UserForm form) {
        model.addAttribute("form", form);
        model.addAttribute("users", userService.list());
        model.addAttribute("roles", new String[]{Role.ADMIN, Role.CASHIER});
        return "settings/users";
    }
}
