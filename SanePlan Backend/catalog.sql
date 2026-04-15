/*
SanePlan
Catalog SQL Schema version 2.0
*/

/* Course information */
CREATE TABLE IF NOT EXISTS Courses (
    code VARCHAR(32) PRIMARY KEY,
    name TEXT NOT NULL,
    credits INT NOT NULL,
    description TEXT NOT NULL
);

/* Course availability information */
CREATE TABLE IF NOT EXISTS Availability (
	code VARCHAR(32) NOT NULL,
    semester VARCHAR(32) NOT NULL,
    FOREIGN KEY (code) REFERENCES Courses(code)
);

/* Pre-requisite information */
CREATE TABLE IF NOT EXISTS preRequisiteGroups (
    parentCode VARCHAR(32) NOT NULL,
    groupID INTEGER PRIMARY KEY,
    FOREIGN KEY (parentCode) REFERENCES Courses(code)
);

CREATE TABLE IF NOT EXISTS preRequisites (
    groupID INT NOT NULL,
    requisiteCode VARCHAR(32),
    FOREIGN KEY (requisiteCode) REFERENCES Courses(code),
    FOREIGN KEY (groupID) REFERENCES preRequisiteGroups(groupID),
    PRIMARY KEY (requisiteCode, groupID)
);

/* Co-requisite information */
CREATE TABLE IF NOT EXISTS coRequisiteGroups (
    parentCode VARCHAR(32) NOT NULL,
    groupID INTEGER PRIMARY KEY,
    FOREIGN KEY (parentCode) REFERENCES Courses(code)
);

CREATE TABLE IF NOT EXISTS coRequisites (
    groupID INT NOT NULL,
    requisiteCode VARCHAR(32),
    FOREIGN KEY (requisiteCode) REFERENCES Courses(code),
    FOREIGN KEY (groupID) REFERENCES coRequisiteGroups(groupID),
    PRIMARY KEY (requisiteCode, groupID)
);