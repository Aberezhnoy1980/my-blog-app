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
}
