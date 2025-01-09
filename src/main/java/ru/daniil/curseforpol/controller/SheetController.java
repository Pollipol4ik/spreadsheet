package ru.daniil.curseforpol.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ru.daniil.curseforpol.model.Cell;
import ru.daniil.curseforpol.model.Sheet;
import ru.daniil.curseforpol.repository.SheetRepository;

import java.io.IOException;
import java.util.Optional;

@Controller
@RequestMapping("/sheets")
@RequiredArgsConstructor
public class SheetController {
    private final SheetRepository sheetRepository;

    @GetMapping
    public String listSheets(Model model) {
        model.addAttribute("sheets", sheetRepository.findAll());
        return "list";
    }

    @PostMapping
    public String createSheet(@RequestParam String name) {
        Sheet sheet = new Sheet();
        sheet.setName(name);
        sheetRepository.save(sheet);
        return "redirect:/sheets";
    }

    @GetMapping("/{id}/export")
    public void exportSheet(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Optional<Sheet> sheetOptional = sheetRepository.findById(id);
        if (sheetOptional.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Sheet sheet = sheetOptional.get();

        // Устанавливаем имя файла и заголовки ответа
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + sheet.getName() + ".xlsx");

        // Создаем файл Excel
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet excelSheet = workbook.createSheet(sheet.getName());

            // Заполняем ячейки данными
            for (Cell cell : sheet.getCells()) {
                // Получаем или создаем строку
                org.apache.poi.ss.usermodel.Row row = excelSheet.getRow(cell.getRow());
                if (row == null) {
                    row = excelSheet.createRow(cell.getRow());
                }

                // Создаем ячейку и задаем значение
                org.apache.poi.ss.usermodel.Cell excelCell = row.createCell(cell.getCol());
                excelCell.setCellValue(cell.getValue());
            }

            // Пишем данные в ответ
            workbook.write(response.getOutputStream());
        }
    }

    @PostMapping("/{id}/import")
    public String importSheet(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        Optional<Sheet> sheetOptional = sheetRepository.findById(id);
        if (sheetOptional.isEmpty()) {
            return "redirect:/sheets";
        }

        Sheet sheet = sheetOptional.get();

        // Читаем файл Excel
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            XSSFSheet excelSheet = workbook.getSheetAt(0);

            // Удаляем старые ячейки
            sheet.getCells().clear();

            // Читаем данные из Excel
            for (org.apache.poi.ss.usermodel.Row row : excelSheet) {
                for (org.apache.poi.ss.usermodel.Cell excelCell : row) {
                    Cell cell = new Cell();
                    cell.setRow(row.getRowNum());
                    cell.setCol(excelCell.getColumnIndex());
                    cell.setValue(excelCell.toString());
                    cell.setSheet(sheet);

                    sheet.getCells().add(cell);
                }
            }

            sheetRepository.save(sheet);
        }

        return "redirect:/sheets";
    }


}

