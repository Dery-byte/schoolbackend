package com.alibou.book.Repositories;

import com.alibou.book.Entity.PackageConfiguration;
import com.alibou.book.Entity.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PackageConfigurationRepository extends JpaRepository<PackageConfiguration, Long> {
    Optional<PackageConfiguration> findBySubscriptionType(SubscriptionType subscriptionType);
}
