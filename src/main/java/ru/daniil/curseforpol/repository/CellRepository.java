package ru.daniil.curseforpol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.curseforpol.model.Cell;

import java.util.List;
import java.util.Optional;

@Repository
public interface CellRepository extends JpaRepository<Cell, Long> {
    List<Cell> findBySheetId(Long sheetId);

    Optional<Cell> findBySheetIdAndRowAndCol(Long sheetId, int row, int col);

}

