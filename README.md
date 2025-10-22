# Spawn-App-Back-End

Back-end REST API for the [Spawn Mobile App](https://github.com/Daggerpov/Spawn-App-iOS-SwiftUI), written in Java Spring Boot, and connected to a MySQL database. Plus, Redis caching, JWTs for authorization, and OAuth Google & Apple sign-in.

Table of contents:

  - [Architecture, Dependency Injection, Testing, \& Entities vs. DTOs (+ Mappers)](#architecture-dependency-injection-testing--entities-vs-dtos--mappers)
  
- [Relationship Diagrams](#relationship-diagrams)
  - [Entity Relationship Diagram](#entity-relationship-diagram)
  - [User DTO Relationships](#user-dto-relationships)
- [Code Explanations](#code-explanations)
  - [Spring Boot](#spring-boot)
  - [JPA (Java Persistence API) \& Hibernate](#jpa-java-persistence-api--hibernate)

## Architecture, Dependency Injection, Testing, & Entities vs. DTOs (+ Mappers)

![diagrams-architecture-dependency-injection-dtos](diagrams/diagrams-architecture-dependency-injection-dtos.png)

# Relationship Diagrams

## Entity Relationship Diagram

![entity-relationship-diagram-Nov-20-v4-location-db-table](diagrams/entity-relationship-diagram.png)

## User DTO Relationships

![user-dto-relationships.png](diagrams/user-dto-relationships.png)

# Code Explanations

## Spring Boot

Spring Boot is our back-end framework with the Java language. It handles the API requests and responses, between our controller, service, and repository layers (see above in [here](#architecture-dependency-injection-testing--entities-vs-dtos--mappers)).

- Spring Annotations
    - `@RestController` tells Spring that this class is a controller, and that it should handle incoming HTTP requests (GET, POST, PUT, DELETE, etc.)
        - We have various mappings for these types of requests, like `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, etc.
            - This is also where we specify the path of these requests, along with:
                - `@PathVariable` for URL parameters
                - `@RequestParam` for query parameters
                - `@RequestBody` for the request body (in a POST request, for example)
    - `@Service` tells Spring that this class is a service, and that it should be managed by Spring
    - `@Autowired` tells Spring to inject the dependency (e.g. `Activitieservice` into `ActivityController`)
    - `@Repository` tells Spring that this class is a repository
- Beans
    - Beans refer to the instantiations of our classes, which get managed by Spring. For example, our service classes, since they are concrete implementations of our interfaces (e.g. `UserService`, being the implementation of `IUserService`), are beans.
    - So, we don't have to manage circular dependencies between classes. For example, since `UserService` takes in `Activitieservice` as a dependency, and vice versa, `Activitieservice` takes in `UserService`, we can annotate them with `@Autowired` to let Spring handle that issue

## JPA (Java Persistence API) & Hibernate

- `@Entity` tells JPA that this class is an entity, and that it should be mapped to a table in the database
- `@Id` tells JPA that this field is the primary key of the table
    - `@GeneratedValue` tells JPA that this field is auto-generated
    - There are also other strategies for generating primary keys, like `GenerationType.IDENTITY`, `GenerationType.SEQUENCE`, etc.
    - For ids of a table, there are also `@EmbeddedId` for composite primary keys, for example in the `ActivityParticipants` table
- `@Column` is used to specify the column name, length, nullable, etc.
- `@OneToMany` and `@ManyToOne` are used to specify the relationships between entities
- `JpaRepository` is an interface that extends `CrudRepository`, which provides CRUD operations for the entity
    - So, there are pre-defined generic methods like `save`, `findById`, `findAll`, `delete`, etc.
    - We can also define custom queries in the repository interface, by using the `@Query` annotation
        - An example of a custom query is in the `UserRepository` interface, where we find users by their username
        - Also, with custom queries, they can be generated for us by simply naming the method according to JPA standards, like in the `ActivityRepository` interface, where we find Activities by their start time using the method, `List<Activity> findByCreatorId(UUID creatorId);`