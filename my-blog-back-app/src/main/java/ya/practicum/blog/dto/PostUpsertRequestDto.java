package ya.practicum.blog.dto;

import java.util.List;

public record PostUpsertRequestDto(
        Long id,
        String title,
        String text,
        List<String> tags
) {
}
