package ru.daniil.curseforpol.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.curseforpol.dto.CellDTO;
import ru.daniil.curseforpol.evalutor.FormulaEvaluator;
import ru.daniil.curseforpol.model.Cell;
import ru.daniil.curseforpol.model.Sheet;
import ru.daniil.curseforpol.repository.CellRepository;
import ru.daniil.curseforpol.repository.SheetRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/sheets")
@RequiredArgsConstructor
public class SheetController {
    private final SheetRepository sheetRepository;
    private final CellRepository cellRepository;
    private final FormulaEvaluator formulaEvaluator;

    @GetMapping
    public String listSheets(Model model) {
        model.addAttribute("sheets", sheetRepository.findAll());
        return "list";
    }

    @PostMapping
    public String createSheet(@RequestParam String name) {
        if (name == null || name.isBlank()) {
            return "redirect:/sheets?error=invalidName";
        }
        Sheet sheet = new Sheet();
        sheet.setName(name);
        sheetRepository.save(sheet);
        return "redirect:/sheets";
    }

    @GetMapping("/{id}/export")
    public void exportSheet(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        if (sheet == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Sheet not found");
            return;
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + sheet.getName() + ".xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet excelSheet = workbook.createSheet(sheet.getName());

            for (Cell cell : sheet.getCells()) {
                Row row = excelSheet.getRow(cell.getRow());
                if (row == null) {
                    row = excelSheet.createRow(cell.getRow());
                }
                org.apache.poi.ss.usermodel.Cell excelCell = row.createCell(cell.getCol());
                excelCell.setCellValue(cell.getValue());
            }
            workbook.write(response.getOutputStream());
        }
    }

    @PostMapping("/{id}/import")
    public String importSheet(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        if (sheet == null) {
            return "redirect:/sheets?error=sheetNotFound";
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet excelSheet = workbook.getSheetAt(0);
            List<Cell> cells = new ArrayList<>();

            for (Row row : excelSheet) {
                for (org.apache.poi.ss.usermodel.Cell excelCell : row) {
                    Cell cell = new Cell();
                    cell.setRow(row.getRowNum());
                    cell.setCol(excelCell.getColumnIndex());
                    cell.setValue(excelCell.toString());
                    cell.setSheet(sheet);
                    cells.add(cell);
                }
            }

            cellRepository.deleteAll(sheet.getCells());
            cellRepository.saveAll(cells);
            sheetRepository.save(sheet);
        }

        return "redirect:/sheets/" + id;
    }

    @GetMapping("/{id}")
    public String viewSheet(@PathVariable Long id, Model model) {
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        if (sheet == null) {
            return "redirect:/sheets?error=sheetNotFound";
        }

        List<Cell> cells = cellRepository.findBySheetId(id);

        // Вычисление максимальных индексов строки и столбца
        int maxRow = cells.stream().mapToInt(Cell::getRow).max().orElse(0);
        int maxCol = cells.stream().mapToInt(Cell::getCol).max().orElse(0);

        // Применение вычисления формул к ячейкам
        for (Cell cell : cells) {
            if (cell.getValue().startsWith("=")) {
                String evaluatedValue = formulaEvaluator.evaluate(cell.getValue(), cells);
                cell.setValue(evaluatedValue); // Обновляем значение ячейки
            }
        }

        model.addAttribute("sheet", sheet);
        model.addAttribute("cells", cells);
        model.addAttribute("maxRow", maxRow);
        model.addAttribute("maxCol", maxCol);

        return "view";
    }



    @PostMapping("/{id}/save")
    @ResponseBody
    public ResponseEntity<?> saveSheet(@PathVariable Long id, @RequestBody List<CellDTO> cellDTOs) {
        Sheet sheet = sheetRepository.findById(id).orElse(null);
        if (sheet == null) {
            return ResponseEntity.notFound().build();
        }

        List<Cell> cells = cellDTOs.stream().map(cellDTO -> {
            Cell cell = cellRepository.findBySheetIdAndRowAndCol(id, cellDTO.row(), cellDTO.column())
                    .orElse(new Cell());
            cell.setRow(cellDTO.row());
            cell.setCol(cellDTO.column());
            cell.setValue(cellDTO.value());
            cell.setSheet(sheet);
            return cell;
        }).collect(Collectors.toList());

        cellRepository.saveAll(cells);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/addRow")
    public String addRow(@PathVariable Long id) {
        Sheet sheet = sheetRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Sheet not found"));

        // Определяем максимальный индекс строки и столбца
        List<Cell> existingCells = cellRepository.findBySheetId(id);
        int maxRow = existingCells.stream().mapToInt(Cell::getRow).max().orElse(-1);
        int maxCol = existingCells.stream().mapToInt(Cell::getCol).max().orElse(0);

        // Создаем новую строку
        List<Cell> newRow = new ArrayList<>();
        for (int col = 0; col <= maxCol; col++) {
            Cell cell = new Cell();
            cell.setRow(maxRow + 1);
            cell.setCol(col);
            cell.setValue(""); // Пустое значение по умолчанию
            cell.setSheet(sheet);
            newRow.add(cell);
        }

        // Сохраняем новую строку
        cellRepository.saveAll(newRow);

        return "redirect:/sheets/" + id;
    }


    @PostMapping("/{id}/addColumn")
    public String addColumn(@PathVariable Long id) {
        Sheet sheet = sheetRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Sheet not found"));

        // Определяем максимальный индекс строки и столбца
        List<Cell> existingCells = cellRepository.findBySheetId(id);
        int maxRow = existingCells.stream().mapToInt(Cell::getRow).max().orElse(0);
        int maxCol = existingCells.stream().mapToInt(Cell::getCol).max().orElse(-1);

        // Создаем новый столбец
        List<Cell> newColumn = new ArrayList<>();
        for (int row = 0; row <= maxRow; row++) {
            Cell cell = new Cell();
            cell.setRow(row);
            cell.setCol(maxCol + 1);
            cell.setValue(""); // Пустое значение по умолчанию
            cell.setSheet(sheet);
            newColumn.add(cell);
        }

        // Сохраняем новый столбец
        cellRepository.saveAll(newColumn);

        return "redirect:/sheets/" + id;
    }

}
