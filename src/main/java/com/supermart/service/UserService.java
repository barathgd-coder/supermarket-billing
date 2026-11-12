package com.supermart.service;

import com.supermart.dto.UserForm;
import com.supermart.exception.BusinessException;
import com.supermart.exception.ResourceNotFoundException;
import com.supermart.model.Role;
import com.supermart.model.User;
import com.supermart.repository.RoleRepository;
import com.supermart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<User> list() { return userRepository.findAllByOrderByUsernameAsc(); }

    @Transactional(readOnly = true)
    public UserForm toForm(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        UserForm f = new UserForm();
        f.setId(u.getId());
        f.setUsername(u.getUsername());
        f.setFullName(u.getFullName());
        f.setRoleName(u.getRole().getName());
        f.setEnabled(u.isEnabled());
        return f;
    }

    @Transactional
    public User save(UserForm f, String currentUsername) {
        Role role = roleRepository.findByName(f.getRoleName())
                .orElseThrow(() -> new BusinessException("Unknown role: " + f.getRoleName()));
        String username = f.getUsername().trim();
        boolean hasPassword = f.getPassword() != null && !f.getPassword().isBlank();
        if (hasPassword && f.getPassword().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters");
        }

        User u;
        if (f.getId() == null) {
            if (userRepository.existsByUsername(username)) throw new BusinessException("Username '" + username + "' is already taken");
            if (!hasPassword) throw new BusinessException("Password is required for a new user");
            u = new User();
            u.setUsername(username);
        } else {
            u = userRepository.findById(f.getId()).orElseThrow(() -> new ResourceNotFoundException("User", f.getId()));
            if (u.getUsername().equals(currentUsername) && (!f.isEnabled() || !Role.ADMIN.equals(role.getName()))) {
                throw new BusinessException("You cannot disable or demote your own account");
            }
        }
        u.setFullName(f.getFullName().trim());
        u.setRole(role);
        u.setEnabled(f.isEnabled());
        if (hasPassword) u.setPassword(passwordEncoder.encode(f.getPassword()));
        return userRepository.save(u);
    }
}
