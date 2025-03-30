package com.alibou.book.Services;

import com.alibou.book.Entity.University;
import com.alibou.book.Entity.UniversityType;
import com.alibou.book.Repositories.UniversityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UniversityService {

    @Autowired
    private UniversityRepository universityRepository;

    public List<University> addUniversity(List<University> university) {
        return universityRepository.saveAll(university);
    }

    public List<University> getAllUniversities() {
        return universityRepository.findAll();
    }

    public List<University> getUniversitiesByType(UniversityType type) {
        return universityRepository.findByType(type);
    }
    public University getUniversityById(Long id) {
        return universityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("University not found"));
    }
}
