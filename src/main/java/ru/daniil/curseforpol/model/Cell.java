package ru.daniil.curseforpol.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Cell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int row;
    private int col;
    private String value;

    private String alignment = "left";
    private String backgroundColor = "#ffffff";

    @ManyToOne
    @JoinColumn(name = "sheet_id", nullable = false)
    private Sheet sheet;

    public boolean isFormula() {
        return value != null && value.startsWith("=");
    }

}

