# Spawn-App-Back-End

Back-End for the [Spawn Mobile App](https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI). The tech stack is Java Springboot for a REST API, that connects to a MySQL database.

- [Spawn-App-Back-End](#spawn-app-back-end)
- [Diagrams](#diagrams)
- [Entity Relationship Diagram](#entity-relationship-diagram)
- [Folder Structure Explanation](#folder-structure-explanation)
  - [Controllers](#controllers)
  - [Models](#models)
  - [Repositories](#repositories)
  - [Services](#services)
  - [DTOs](#dtos)

# Diagrams

![diagrams-architecture-dependency-injection-dtos](diagrams-architecture-dependency-injection-dtos.png)

# Entity Relationship Diagram

![entity-relationship-diagram](entity-relationship-diagram.png)

# Folder Structure Explanation

## Controllers

Controllers contain endpoints for the API to GET, POST, PUT, and DELETE data from the database, using HTTP requests.

## Models

Models, otherwise called "entities," are the classes that represent the database tables. They're what are retrieved and put into the database.

## Repositories

These classes deal with data access, typically using an ORM (Object-Relational Mapping) framework or JPA (Java Persistence API) to interact with the database.

## Services

These classes implement business logic. Controllers use these services to perform operations on data.

## DTOs

DTOs (Data Transfer Object) are used to encapsulate which data needs to be sent around, from database models.

Read up: https://www.baeldung.com/java-dto-pattern

From my understanding, through using them a lot at work (in C# .NET), they might conceal some of the complexity of the database models, and also allow for more flexibility in the future, if the database models change.

That way, you can separate the actual models (or entities) from what you send around.

An example:

`public record UserDTO (Long id, String name){}`

Using a record, because class functionality isn't needed. Therefore, it's more concise and readable.
