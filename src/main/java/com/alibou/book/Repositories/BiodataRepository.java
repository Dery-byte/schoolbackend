package com.alibou.book.Repositories;

import com.alibou.book.Entity.Biodata;
import com.alibou.book.Entity.GhanaRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BiodataRepository extends JpaRepository<Biodata, Integer> {
    // You can add custom query methods here if needed
    Optional<Biodata> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Biodata> findByRecordId(String recordId);  // Add this method

    @Query("SELECT b.region as region, COUNT(b) as count FROM Biodata b GROUP BY b.region")
    List<RegionCountProjection> countBiodataByRegion();

    public interface RegionCountProjection {
        GhanaRegion getRegion();
        Long getCount();
    }
}