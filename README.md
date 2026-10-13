# ResLife Manager

A web app for managing residential life at a boarding school. Teachers send students to study hall, and the residential life team works through those requests, tracks each student's study hall backlog, and takes attendance.

Built as my IB Computer Science HL Internal Assessment.

## Features

- **Logins and roles.** Teachers and ResLife staff sign in with their own accounts. Passwords are stored as bcrypt hashes.
- **Requests.** Teachers submit a study hall request for a student on a given date and can follow its status (`NEW`, `ACK`, `DONE`, `CANCEL`).
- **Queue.** ResLife staff see incoming requests in one place.
- **Student directory.** Add, edit and remove students (ID, grade, dorm, room).
- **Backlog with audit log.** Each change to a student's backlog is recorded with who made it and when.
- **Study hall attendance.** Check students in and out and record lateness.
- **Notifications and release history.** Staff who opt in are notified when a student clears their backlog and is released.

## Tech stack

- Java 17 and Maven
- [Spark Java](https://sparkjava.com/) for the web server and REST API
- SQLite via `sqlite-jdbc`
- Gson for JSON, jBCrypt for password hashing
- Plain HTML, CSS and JavaScript front end (`webroot/`)

## Project structure

```
src/main/java/app/
  Main.java             Server setup, database init, route table
  AuthRoutes.java       Login, logout, current user
  Routes.java           Requests, students, backlog, attendance, notifications
  DirectoryRoutes.java  Pages available to each role
  BaseRoutes.java       Shared helpers
src/main/resources/db/
  schema.sql            Table definitions
  seed.sql              Demo accounts and a sample student
webroot/                Front-end pages
```

## Running it

You need Java 17+ and Maven. From the project root:

```bash
mvn compile exec:java
```

Then open http://localhost:4567/login.html. The server creates `reslife.db` in the working directory on first run.

Demo accounts:

| Username   | Password      | Role    |
|------------|---------------|---------|
| `teacher1` | `teacherpass` | Teacher |
| `reslife1` | `reslifepass` | ResLife |
