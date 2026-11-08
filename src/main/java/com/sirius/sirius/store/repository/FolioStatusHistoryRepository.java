
package com.sirius.sirius.store.repository;

import com.sirius.sirius.store.entity.biling.FolioStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolioStatusHistoryRepository
        extends JpaRepository<FolioStatusHistoryEntity, Long> {

    List<FolioStatusHistoryEntity> findAllByFolioIdOrderByChangedAtDesc(
            Long folioId
    );
}
