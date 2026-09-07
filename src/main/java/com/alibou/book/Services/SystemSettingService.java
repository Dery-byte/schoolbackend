package com.alibou.book.Services;

import com.alibou.book.Entity.SystemSetting;
import com.alibou.book.Repositories.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository repository;

    public String getSetting(String key, String defaultValue) {
        return repository.findById(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    @Transactional
    public void updateSetting(String key, String value) {
        SystemSetting setting = repository.findById(key)
                .orElseGet(() -> new SystemSetting(key, value));
        setting.setSettingValue(value);
        repository.saveAndFlush(setting);
        log.info("System setting saved: {} = {}", key, value);
    }
}
