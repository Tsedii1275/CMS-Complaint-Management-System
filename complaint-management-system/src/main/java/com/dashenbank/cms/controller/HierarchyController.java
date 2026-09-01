package com.dashenbank.cms.controller;

import com.dashenbank.cms.model.District;
import com.dashenbank.cms.repository.DistrictRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hierarchy")
public class HierarchyController {

    private final DistrictRepository districtRepository;

    public HierarchyController(DistrictRepository districtRepository) {
        this.districtRepository = districtRepository;
    }

    @GetMapping
    public ResponseEntity<List<District>> getHierarchy() {
        List<District> districts = districtRepository.findAll();
        return ResponseEntity.ok(districts);
    }
}
