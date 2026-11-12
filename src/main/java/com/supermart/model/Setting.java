package com.supermart.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "settings")
@Getter @Setter @NoArgsConstructor
public class Setting {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 80)
    private String settingKey;

    @Column(name = "setting_value", length = 500)
    private String settingValue;

    public Setting(String key, String value) {
        this.settingKey = key;
        this.settingValue = value;
    }
}
