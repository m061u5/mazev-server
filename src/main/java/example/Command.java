package example;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// Define Command as a sealed interface with variants
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Command.Increment.class, name = "Increment"),
        @JsonSubTypes.Type(value = Command.Decrement.class, name = "Decrement")
})
public sealed interface Command {
    // Command variants as records
    record Increment(String clientId) implements Command {}

    record Decrement(String clientId) implements Command {}
}


