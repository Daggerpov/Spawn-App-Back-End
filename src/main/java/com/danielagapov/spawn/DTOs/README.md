DTOs (Data Transfer Object) are used to encapsulate which data needs to be sent around, from database models.

From my understanding, through using them a lot at work (in C# .NET), they might conceal some of the complexity of the database models, and also allow for more flexibility in the future, if the database models change.

That way, you can separate the actual models (or entities) from what you send around.

An example:

`
public record UserDTO (Long id, String name){}
`

Using a record, because class functionality isn't needed. Therefore, it's more concise and readable.