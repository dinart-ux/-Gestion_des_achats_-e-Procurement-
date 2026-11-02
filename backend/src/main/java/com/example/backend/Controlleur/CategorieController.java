
package com.example.backend.Controlleur;

import com.example.backend.Repository.CategorieRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/categories")
public class CategorieController {

    private final CategorieRepository categories;

    public CategorieController(CategorieRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return categories.findAll().stream()
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "nom", c.getNom(),
                        "description", c.getDescription() == null
                                ? "" : c.getDescription()
                ))
                .toList();
    }
}
