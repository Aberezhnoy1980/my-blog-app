package ya.practicum.blog.dto;

public record CommentUpsertRequestDto(
        Long id,
        Long postId,
        String text
) {
}
