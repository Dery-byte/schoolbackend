package com.alibou.book.Services;

import com.alibou.book.Entity.SystemSetting;
import com.alibou.book.Repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public String getSetting(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public void updateSetting(String key, String value) {
        SystemSetting setting = repository.findById(key)
                .orElse(new SystemSetting(key, value));
        setting.setSettingValue(value);
        repository.save(setting);
    }
}
