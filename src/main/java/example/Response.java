package example;

public interface Response {
    record State(int counter) implements Response {}
}
