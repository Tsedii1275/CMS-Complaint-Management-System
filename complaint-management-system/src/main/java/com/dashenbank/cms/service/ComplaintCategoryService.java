package com.dashenbank.cms.service;

import com.dashenbank.cms.model.ComplaintCategory;
import com.dashenbank.cms.repository.ComplaintCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class ComplaintCategoryService {

    static final String ALREADY_EXISTS = "This category already exists.";
    static final String TURNED_OFF = "This category is turned off. An operational admin can turn it back on.";
    static final String STANDARD_STAYS = "The standard categories stay available.";

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N}&/\\-',. ]{1,99}$");

    private final ComplaintCategoryRepository repository;

    public ComplaintCategoryService(ComplaintCategoryRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> activeCategories() {
        return repository.findByActiveTrueOrderBySortOrderAscNameAsc().stream().map(this::toMap).toList();
    }

    public List<Map<String, Object>> allCategories() {
        return repository.findAllByOrderBySortOrderAscNameAsc().stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> create(String rawName) {
        String name = displayName(rawName);
        if ("add category".equals(nameKey(name))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the new category name.");
        }
        ComplaintCategory existing = repository.findByNameKey(nameKey(name)).orElse(null);
        if (existing != null) {
            if (existing.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, ALREADY_EXISTS);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, TURNED_OFF);
        }
        int nextOrder = repository.findTopByOrderBySortOrderDesc().map(row -> row.getSortOrder() + 1).orElse(100);
        ComplaintCategory saved = repository.save(ComplaintCategory.builder()
                .name(name)
                .nameKey(nameKey(name))
                .active(true)
                .builtin(false)
                .sortOrder(nextOrder)
                .createdAt(LocalDateTime.now())
                .build());
        return toMap(saved);
    }

    @Transactional
    public Map<String, Object> setActive(Long id, Boolean active) {
        if (active == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose whether the category is on or off.");
        }
        ComplaintCategory category = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found."));
        if (category.isBuiltin() && !active) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, STANDARD_STAYS);
        }
        category.setActive(active);
        return toMap(repository.save(category));
    }

    static String displayName(String rawName) {
        String collapsed = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (!NAME_PATTERN.matcher(collapsed).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a category name using letters or numbers, up to 100 characters.");
        }
        return collapsed;
    }

    static String nameKey(String displayName) {
        return displayName.toLowerCase(Locale.ROOT);
    }

    private Map<String, Object> toMap(ComplaintCategory category) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", category.getId());
        row.put("name", category.getName());
        row.put("active", category.isActive());
        row.put("builtin", category.isBuiltin());
        row.put("sortOrder", category.getSortOrder());
        return row;
    }
}
