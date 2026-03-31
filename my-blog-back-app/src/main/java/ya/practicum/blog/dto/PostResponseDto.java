package ya.practicum.blog.dto;

import java.util.List;

public record PostResponseDto(
        Long id,
        String title,
        String text,
        List<String> tags,
        int likesCount,
        int commentsCount
) {
    /**
     * Compatibility alias for frontend variants that read postId instead of id.
     */
    public Long getPostId() {
        return id;
    }
}
