AssignmentService
Version: 1.0.0Release Date: May 25, 2025Deployed At: https://assignmentservice-2a8o.onrender.com
Overview
AssignmentService is a Spring Boot application designed to manage assignments for educational purposes. It provides a REST API to create, update, delete, and retrieve assignments, with support for file uploads and downloads using MongoDB GridFS. The application uses MongoDB as its database to store assignment metadata and associated files.
This project is ideal for educational institutions, teachers, or students who need a simple API to manage assignments, including uploading assignment files and retrieving them later.
Features

Assignment Management:
Create new assignments with file uploads.
Update existing assignments, including replacing files.
Delete assignments by ID.
Retrieve a single assignment by ID.
Retrieve all assignments for a specific course.
Retrieve all assignments.


File Handling:
Upload files with assignments, stored in MongoDB GridFS.
Download files associated with an assignment.


Data Validation:
Ensures required fields (CourseId, title, description, dueDate) are provided.


Logging:
Comprehensive logging for debugging and monitoring.


CORS:
Configured to allow requests from all origins (*).


Prerequisites

Java: 17 or higher
Maven: For dependency management and building the project
MongoDB: A running MongoDB instance (with GridFS support)
Git: To clone the repository
Postman (optional): For testing API endpoints

Build the Project:
mvn clean install


Run the Application:
mvn spring-boot:run

The application will start on http://localhost:8085 by default.


Deployment
The application is deployed on Render at https://assignmentservice-2a8o.onrender.com.
Deploying on Render

Fork or clone this repository to your GitHub account.
Create a new Web Service on Render.
Connect your GitHub repository.

Deploy the app.
Note: The Render free tier may cause the app to spin down due to inactivity, leading to a 10-30 second delay on the first request.



