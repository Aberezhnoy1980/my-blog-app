package ya.practicum.blog.dto;

public record CommentResponseDto(
        Long id,
        String text,
        Long postId
) {
}
