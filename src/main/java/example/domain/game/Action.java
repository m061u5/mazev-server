package example.domain.game;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

public record Action(Entity.Player player, Entity.Player.Direction direction) {
}


