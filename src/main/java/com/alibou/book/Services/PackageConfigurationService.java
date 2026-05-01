package com.alibou.book.Services;

import com.alibou.book.Entity.InstitutionTypeVisibility;
import com.alibou.book.Entity.PackageConfiguration;
import com.alibou.book.Entity.SubscriptionType;
import com.alibou.book.Repositories.PackageConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackageConfigurationService {

    private final PackageConfigurationRepository repository;

    public List<PackageConfiguration> getAllConfigurations() {
        return repository.findAll();
    }

    public PackageConfiguration getConfigurationBySubscriptionType(SubscriptionType type) {
        return repository.findBySubscriptionType(type)
                .orElseGet(() -> getDefaultConfiguration(type));
    }

    public PackageConfiguration updateConfiguration(PackageConfiguration config) {
        log.info("Updating package configuration for {}", config.getSubscriptionType());
        PackageConfiguration existing = repository.findBySubscriptionType(config.getSubscriptionType())
                .orElse(new PackageConfiguration());
        
        existing.setSubscriptionType(config.getSubscriptionType());
        existing.setPrice(config.getPrice());
        existing.setPrivateSchoolSlots(config.getPrivateSchoolSlots());
        existing.setPublicSchoolSlots(config.getPublicSchoolSlots());
        existing.setProgramsPerPrivateUniversity(config.getProgramsPerPrivateUniversity());
        existing.setProgramsPerPublicUniversity(config.getProgramsPerPublicUniversity());
        existing.setVisibility(config.getVisibility());

        return repository.save(existing);
    }

    private PackageConfiguration getDefaultConfiguration(SubscriptionType type) {
        log.debug("No configuration found for {}, returning defaults", type);
        switch (type) {
            case PREMIUM_PLUS:
                return PackageConfiguration.builder()
                        .subscriptionType(SubscriptionType.PREMIUM_PLUS)
                        .price(100.0) // Example
                        .privateSchoolSlots(100)
                        .publicSchoolSlots(100)
                        .programsPerPrivateUniversity(20)
                        .programsPerPublicUniversity(20)
                        .maxCategorySelection(25)
                        .visibility(InstitutionTypeVisibility.BOTH)
                        .build();
            case PREMIUM:
                return PackageConfiguration.builder()
                        .subscriptionType(SubscriptionType.PREMIUM)
                        .price(50.0)
                        .privateSchoolSlots(10)
                        .publicSchoolSlots(10)
                        .programsPerPrivateUniversity(10)
                        .programsPerPublicUniversity(10)
                        .maxCategorySelection(15)
                        .visibility(InstitutionTypeVisibility.BOTH)
                        .build();
            case BASIC:
            default:
                return PackageConfiguration.builder()
                        .subscriptionType(SubscriptionType.BASIC)
                        .price(20.0)
                        .privateSchoolSlots(3)
                        .publicSchoolSlots(2)
                        .programsPerPrivateUniversity(5)
                        .programsPerPublicUniversity(4)
                        .maxCategorySelection(10)
                        .visibility(InstitutionTypeVisibility.BOTH)
                        .build();
        }
    }
}
