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
        @JsonSubTypes.Type(value = Increment.class, name = "Increment"),
        @JsonSubTypes.Type(value = Decrement.class, name = "Decrement")
})
public sealed interface Command permits Increment, Decrement {
    // Command variants as records
}


