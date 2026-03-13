/*
Courses table
*/

/* Course information */
CREATE TABLE IF NOT EXISTS Courses (
    courseCode VARCHAR(32) PRIMARY KEY,
    courseName VARCHAR(256),
    credits INT,
    courseDescription VARCHAR(1024)
);

/* Course availability information */
CREATE TABLE IF NOT EXISTS availableSemesters (
    semester VARCHAR(32),
    courseCode VARCHAR(32),
    FOREIGN KEY (courseCode) REFERENCES Courses(courseCode)
);

/* Co-requisite information */
CREATE TABLE IF NOT EXISTS coRequisiteGroups (
    groupID SERIAL PRIMARY KEY,
    courseCode VARCHAR(32),
    FOREIGN KEY (courseCode) REFERENCES Courses(courseCode)
);

CREATE TABLE IF NOT EXISTS coRequisites (
    groupID INT NOT NULL,
    requisiteCode VARCHAR(32),
    FOREIGN KEY (requisiteCode) REFERENCES Courses(courseCode),
    FOREIGN KEY (groupID) REFERENCES coRequisiteGroups(groupID),
    PRIMARY KEY (requisiteCode, groupID)
);

/* Pre-requisite information */
CREATE TABLE IF NOT EXISTS preRequisiteGroups (
    groupID SERIAL PRIMARY KEY,
    courseCode VARCHAR(32),
    FOREIGN KEY (courseCode) REFERENCES Courses(courseCode)
);

CREATE TABLE IF NOT EXISTS preRequisites (
    groupID INT NOT NULL,
    requisiteCode VARCHAR(32),
    FOREIGN KEY (requisiteCode) REFERENCES Courses(courseCode),
    FOREIGN KEY (groupID) REFERENCES preRequisiteGroups(groupID),
    PRIMARY KEY (requisiteCode, groupID)
);