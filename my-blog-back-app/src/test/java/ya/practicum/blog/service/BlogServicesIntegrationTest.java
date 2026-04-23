package ya.practicum.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ya.practicum.blog.dto.CommentResponseDto;
import ya.practicum.blog.dto.CommentUpsertRequestDto;
import ya.practicum.blog.dto.PostListResponseDto;
import ya.practicum.blog.dto.PostResponseDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;
import ya.practicum.blog.exception.BadRequestException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class BlogServicesIntegrationTest {
    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM posts");
    }

    @Test
    void shouldCreateAndGetPost() {
        PostResponseDto created = postService.createPost(
                new PostUpsertRequestDto(null, "First post", "Hello markdown", List.of("java", "spring"))
        );

        PostResponseDto fetched = postService.getPost(created.id());
        assertEquals("First post", fetched.title());
        assertEquals("Hello markdown", fetched.text());
        assertEquals(List.of("java", "spring"), fetched.tags());
    }

    @Test
    void shouldTruncateTextOnFeedResponse() {
        String longText = "a".repeat(160);
        postService.createPost(new PostUpsertRequestDto(null, "Title", longText, List.of("tag")));

        PostListResponseDto feed = postService.getPosts("", 1, 10);
        assertEquals(1, feed.posts().size());
        assertEquals(131, feed.posts().getFirst().text().length());
        assertTrue(feed.posts().getFirst().text().endsWith("..."));
    }

    @Test
    void shouldSearchAndPaginatePostsInDatabase() {
        for (int i = 0; i < 5; i++) {
            postService.createPost(new PostUpsertRequestDto(null, "Series " + i, "body", List.of("shared")));
        }
        postService.createPost(new PostUpsertRequestDto(null, "Unique marker title", "body", List.of("other")));

        PostListResponseDto byTitle = postService.getPosts("marker", 1, 10);
        assertEquals(1, byTitle.posts().size());
        assertEquals("Unique marker title", byTitle.posts().getFirst().title());

        PostListResponseDto byTag = postService.getPosts("#shared", 1, 10);
        assertEquals(5, byTag.posts().size());

        PostListResponseDto page1 = postService.getPosts("", 1, 2);
        assertEquals(2, page1.posts().size());
        assertEquals(3, page1.lastPage());

        PostListResponseDto page3 = postService.getPosts("", 3, 2);
        assertEquals(2, page3.posts().size());
    }

    @Test
    void shouldReturnNoPostsForUnknownHashtag() {
        postService.createPost(new PostUpsertRequestDto(null, "Tagged", "body", List.of("real")));
        PostListResponseDto feed = postService.getPosts("#nonexistent", 1, 10);
        assertTrue(feed.posts().isEmpty());
        assertEquals(1, feed.lastPage());
    }

    @Test
    void shouldRejectInvalidPostPayload() {
        assertThrows(BadRequestException.class, () -> postService.createPost(
                new PostUpsertRequestDto(null, " ", "text", List.of("tag"))
        ));

        assertThrows(BadRequestException.class, () -> postService.createPost(
                new PostUpsertRequestDto(null, "Title", "text", List.of())
        ));
    }

    @Test
    void shouldUpdateCommentWithBrokenFrontendPathFallback() {
        PostResponseDto firstPost = postService.createPost(
                new PostUpsertRequestDto(null, "Post 1", "Body 1", List.of("tag"))
        );
        postService.createPost(new PostUpsertRequestDto(null, "Post 2", "Body 2", List.of("tag")));

        CommentResponseDto createdComment = commentService.createComment(
                firstPost.id(),
                new CommentUpsertRequestDto(null, firstPost.id(), "Comment body")
        );

        long brokenPathPostId = createdComment.id();
        CommentResponseDto updated = commentService.updateComment(
                brokenPathPostId,
                createdComment.id(),
                new CommentUpsertRequestDto(createdComment.id(), firstPost.id(), "Updated body")
        );

        assertEquals("Updated body", updated.text());
        assertEquals(firstPost.id(), updated.postId());
    }
}
