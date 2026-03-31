package ya.practicum.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ya.practicum.blog.BlogConstraints;

public record CommentUpsertRequestDto(
        Long id,
        Long postId,
        @NotBlank(message = "text is required")
        @Size(max = BlogConstraints.MAX_COMMENT_LENGTH, message = "text is too long")
        String text
) {
}
