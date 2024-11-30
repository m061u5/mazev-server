package example.domain.game;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Entity.Gold.class, name = "G"),
        @JsonSubTypes.Type(value = Entity.Player.class, name = "P"),
        @JsonSubTypes.Type(value = Entity.Dragon.class, name = "D"),
        @JsonSubTypes.Type(value = Entity.Health.class, name = "H"),
})

public sealed interface Entity {
    record Gold(int id, int value) implements Entity {}
    record Health(int id, int value) implements Entity {}
    record Player(String name) implements Entity {
        public enum Direction {
            Up,
            Down,
            Left,
            Right
        }
    }
    record Dragon(Dragon.Size size) implements Entity {
        public enum Size {
            Small,
            Medium,
            Large
        }
    }
}
