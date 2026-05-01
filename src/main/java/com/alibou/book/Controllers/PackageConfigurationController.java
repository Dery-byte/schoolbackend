package com.alibou.book.Controllers;

import com.alibou.book.Entity.PackageConfiguration;
import com.alibou.book.Services.PackageConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/admin/packages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PackageConfigurationController {

    private final PackageConfigurationService service;

    @GetMapping
    public ResponseEntity<List<PackageConfiguration>> getAll() {
        return ResponseEntity.ok(service.getAllConfigurations());
    }

    @PutMapping
    public ResponseEntity<PackageConfiguration> update(@RequestBody PackageConfiguration config) {
        return ResponseEntity.ok(service.updateConfiguration(config));
    }
}
