package example.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import example.domain.game.Entity;
import example.domain.game.Location;
import example.domain.game.Cave;

import java.util.Collection;
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Response.StateCave.class, name = "C"),
        @JsonSubTypes.Type(value = Response.StateLocations.class, name = "L"),
        @JsonSubTypes.Type(value = Response.Authorized.class, name = "A"),
        @JsonSubTypes.Type(value = Response.Unauthorized.class, name = "U"),
})
public sealed interface Response {
    record StateCave(Cave cave) implements Response {
    }

    record StateLocations(Collection<EntityLocation> entityLocations) implements Response {
        public record EntityLocation(Entity entity, Location location) {
        }
    }

    record Authorized(Entity.Player player) implements Response {
    }

    record Unauthorized() implements Response {
    }
}


