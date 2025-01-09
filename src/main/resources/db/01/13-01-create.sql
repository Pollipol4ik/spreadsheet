-- Таблица: event_type
CREATE TABLE event_type
(
    type_id SERIAL PRIMARY KEY,
    name    VARCHAR(100) NOT NULL
);

-- Таблица: edu_material_type
CREATE TABLE edu_material_type
(
    edu_material_type_id SERIAL PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL
);

-- Таблица: student
CREATE TABLE student
(
    student_id SERIAL PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    edu_group  VARCHAR(20)
);

-- Таблица: university_event
CREATE TABLE university_event
(
    event_id      SERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   VARCHAR(300),
    time_spending TIMESTAMP,
    type_id       INT          REFERENCES event_type (type_id) ON DELETE SET NULL,
    visit_count INT default 0
);


-- Таблица: event_visit
CREATE TABLE event_visit
(
    visit_id     SERIAL PRIMARY KEY,
    visit_status INTEGER,
    event_id     INT REFERENCES university_event (event_id) ON DELETE CASCADE,
    student_id   INT REFERENCES student (student_id) ON DELETE CASCADE
);

-- Таблица: comment
CREATE TABLE comment
(
    comment_id  SERIAL PRIMARY KEY,
    event_id    INT REFERENCES university_event (event_id) ON DELETE SET NULL,
    author      VARCHAR(50),
    grade       INT,
    description VARCHAR(300)
);

-- Таблица: edu_material
CREATE TABLE edu_material
(
    edu_material_id      SERIAL PRIMARY KEY,
    name                 VARCHAR(100) NOT NULL,
    content              TEXT,
    student_id           INT          REFERENCES student (student_id) ON DELETE SET NULL,
    edu_material_type_id INT          REFERENCES edu_material_type (edu_material_type_id) ON DELETE SET NULL
);

CREATE FUNCTION CountMaterials(studentId INT)
RETURNS INT AS $$
BEGIN
    return (SELECT COUNT(*)
    from edu_material
    where student_id = studentId and  edu_material_type_id >= 2);
end;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE FUNCTION update_visit_count()
    RETURNS TRIGGER AS $$
DECLARE
    eventCount INT;
BEGIN
    SELECT COUNT(*) INTO eventCount
                    FROM event_visit
                    WHERE event_id = NEW.event_id AND visit_status = 1;
    UPDATE university_event
    SET visit_count = eventCount
    WHERE event_id = NEW.event_id;
    RETURN NEW;
end;
$$ LANGUAGE plpgsql;
CREATE TRIGGER update_visit_count_trigger AFTER INSERT ON event_visit
    FOR EACH ROW
EXECUTE FUNCTION update_visit_count();

CREATE OR REPLACE FUNCTION CalculateAttendance (eventId INT)
RETURNS DECIMAL (5, 2) AS $$
DECLARE
    totalVisits INT;
    attendedVisits INT;
    attendancePercentage DECIMAL(5, 2);
BEGIN
    SELECT COUNT(*) INTO totalVisits
                    FROM event_visit
                    WHERE event_id = eventId;

    SELECT COUNT(*) INTO attendedVisits
                    FROM event_visit
                    WHERE event_id = eventId AND visit_status = 1;
    IF totalVisits > 0 THEN
        attendancePercentage := (attendedVisits::DECIMAL / totalVisits::DECIMAL) * 100;
    ELSE
        attendancePercentage := 0;
    END IF;

    RETURN attendancePercentage;
END;
$$ LANGUAGE plpgsql;


-- Заполнение таблицы event_type
INSERT INTO event_type (name)
VALUES ('Lecture'),
       ('Workshop'),
       ('Seminar'),
       ('Conference');

-- Заполнение таблицы university_event
INSERT INTO university_event (name, description, time_spending, type_id)
VALUES ('Computer Science Lecture', 'Introduction to Computer Science concepts', '2024-01-15 10:00:00', 1),
       ('Data Science Workshop', 'Hands-on data analysis session', '2024-02-10 14:00:00', 2),
       ('AI Seminar', 'Exploring Artificial Intelligence', '2024-03-05 09:30:00', 3),
       ('Networking Conference', 'Meet industry professionals', '2024-04-12 12:00:00', 4);

-- Заполнение таблицы student
INSERT INTO student (full_name, edu_group)
VALUES ('Alice Johnson', 'CS101'),
       ('Bob Smith', 'CS101'),
       ('Charlie Brown', 'DS201'),
       ('Diana Prince', 'AI301');

-- Заполнение таблицы event_visit
INSERT INTO event_visit (visit_status, event_id, student_id)
VALUES (1, 1, 1),
       (0, 1, 2),
       (1, 2, 3),
       (1, 3, 4);

-- Заполнение таблицы comment
INSERT INTO comment (event_id, author, grade, description)
VALUES (1, 'Alice Johnson', 5, 'Very informative lecture!'),
       (2, 'Charlie Brown', 4, 'Good workshop with practical exercises.'),
       (3, 'Diana Prince', 5, 'Loved the insights on AI.');

-- Заполнение таблицы edu_material_type
INSERT INTO edu_material_type (name)
VALUES ('Lecture Notes'),
       ('Workshop Handouts'),
       ('Seminar Slides'),
       ('Conference Booklet');

-- Заполнение таблицы edu_material
INSERT INTO edu_material (name, content, student_id, edu_material_type_id)
VALUES ('CS101 Lecture Notes', 'Content of Computer Science Lecture', 1, 1),
       ('DS201 Workshop Materials', 'Data Science practical materials', 3, 2),
       ('AI Seminar Slides', 'Slides on AI topics', 4, 3),
       ('Networking Booklet', 'Details about the Networking conference', 1, 4);