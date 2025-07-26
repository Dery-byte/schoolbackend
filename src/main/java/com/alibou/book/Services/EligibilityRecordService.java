package com.alibou.book.Services;

import com.alibou.book.Entity.EligibilityRecord;
import com.alibou.book.Repositories.EligibilityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EligibilityRecordService {

    private final EligibilityRecordRepository repository;
    private final UserDetailsService userDetailsService;
    public List<EligibilityRecord> getAllRecords() {
        return repository.findAll();
    }

    public List<EligibilityRecord> getRecordsByUser(String userId) {
        return repository.findByUserId(userId);  // uses eager fetch
    }
}
