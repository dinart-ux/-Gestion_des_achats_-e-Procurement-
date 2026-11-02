
package com.example.backend.Repository;

import com.example.backend.Persistence.FournisseurCategorieEntity;
import com.example.backend.Persistence.FournisseurCategorieId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FournisseurCategorieRepository
        extends JpaRepository<FournisseurCategorieEntity, FournisseurCategorieId> {
    List<FournisseurCategorieEntity> findAllByFournisseurId(Integer fournisseurId);
    void deleteAllByFournisseurId(Integer fournisseurId);
}
