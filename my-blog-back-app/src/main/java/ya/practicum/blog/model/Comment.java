package ya.practicum.blog.model;

public record Comment(
        Long id,
        Long postId,
        String text
) {
}
