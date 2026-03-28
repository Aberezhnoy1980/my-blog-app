package ya.practicum.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ya.practicum.blog.BlogConstraints;

import java.util.List;

public record PostUpsertRequestDto(
        Long id,
        @NotBlank(message = "title is required")
        @Size(max = BlogConstraints.MAX_TITLE_LENGTH, message = "title is too long")
        String title,
        @NotBlank(message = "text is required")
        @Size(max = BlogConstraints.MAX_POST_TEXT_LENGTH, message = "text is too long")
        String text,
        @NotNull(message = "tags is required")
        List<
                @NotBlank(message = "tags contain invalid values")
                @Size(max = BlogConstraints.MAX_TAG_LENGTH, message = "tags contain invalid values")
                        String
                > tags
) {
}
