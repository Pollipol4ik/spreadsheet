package ru.daniil.curseforpol.evalutor;

import org.springframework.stereotype.Component;
import ru.daniil.curseforpol.model.Cell;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

@Component
public class FormulaEvaluator {

    public String evaluate(String formula, List<Cell> cells) {
        if (!formula.startsWith("=")) {
            return formula;
        }

        try {
            String expression = formula.substring(1); // Убираем знак "="
            for (Cell cell : cells) {
                String cellRef = getCellReference(cell.getRow(), cell.getCol());
                if (expression.contains(cellRef)) {
                    expression = expression.replace(cellRef, cell.getValue() != null ? cell.getValue() : "0");
                }
            }
            // Вычисляем выражение
            return String.valueOf(eval(expression));
        } catch (Exception e) {
            return "ERROR";
        }
    }

    private String getCellReference(int row, int col) {
        // Столбцы - это латинские буквы (A, B, C, ...)
        char colChar = (char) ('A' + col);
        // Строки - индексация с 1 (1, 2, 3, ...)
        return colChar + String.valueOf(row + 1);
    }

    private double eval(String expression) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        try {
            return ((Number) engine.eval(expression)).doubleValue();
        } catch (ScriptException e) {
            throw new RuntimeException("Invalid formula");
        }
    }
}

