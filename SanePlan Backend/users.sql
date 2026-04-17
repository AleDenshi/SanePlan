CREATE TABLE IF NOT EXISTS Users (
	username VARCHAR(64) PRIMARY KEY,
	passwordHash VARCHAR(128) NOT NULL,
	isAdmin BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS Plans (
	username VARCHAR(64) NOT NULL,
	planID INT PRIMARY KEY,
	planName TEXT NOT NULL,
	FOREIGN KEY (username) REFERENCES Users(username)
);

CREATE TABLE IF NOT EXISTS Semesters (
	semesterID INT PRIMARY KEY,
	planID INT,
	type VARCHAR(32) NOT NULL,
	FOREIGN KEY (planID) REFERENCES Plans(planID)
);

CREATE TABLE IF NOT EXISTS SemesterHasCourse (
	semesterID INT,
	code VARCHAR(32),
	FOREIGN KEY (semesterID) REFERENCES Semesters(semesterID)
);