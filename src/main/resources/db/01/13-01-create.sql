-- ChangeSet 1: Create table `sheet`
CREATE TABLE sheet (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ChangeSet 2: Create table `cell` and add foreign key constraint
CREATE TABLE cell (
                      id BIGSERIAL PRIMARY KEY,
                      row INT NOT NULL,
                      col INT NOT NULL,
                      value TEXT,
                      sheet_id BIGINT NOT NULL,
                      CONSTRAINT fk_cell_sheet FOREIGN KEY (sheet_id) REFERENCES sheet (id)
);
