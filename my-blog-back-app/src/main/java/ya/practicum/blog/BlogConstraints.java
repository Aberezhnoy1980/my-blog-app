package ya.practicum.blog;

/**
 * Central limits for post/comment payloads and uploads. Shared by services and tests.
 */
public final class BlogConstraints {

    private BlogConstraints() {
    }

    public static final int MAX_TITLE_LENGTH = 255;
    public static final int MAX_POST_TEXT_LENGTH = 10_000;
    public static final int MAX_TAG_LENGTH = 64;
    public static final int MAX_COMMENT_LENGTH = 2_000;
    public static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    /** Truncated text length in post list responses (UI preview). */
    public static final int POST_LIST_TEXT_PREVIEW_LENGTH = 128;
}
