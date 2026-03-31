package ya.practicum.blog.model;

public record PostImage(
        byte[] data,
        String contentType
) {
}
