package ru.daniil.curseforpol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.daniil.curseforpol.model.Sheet;

@Repository
public interface SheetRepository extends JpaRepository<Sheet, Long> {
}
