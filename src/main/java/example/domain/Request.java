package example.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import example.domain.game.Entity;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Request.Command.class, name = "C"),
        @JsonSubTypes.Type(value = Request.Authorize.class, name = "A"),
})
public sealed interface Request {
    record Authorize(String key) implements Request {
    }

    record Command(Entity.Player.Direction direction) implements Request {
    }
}
