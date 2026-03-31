package ya.practicum.blog.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ya.practicum.blog.dto.PostListResponseDto;
import ya.practicum.blog.dto.PostResponseDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;
import ya.practicum.blog.exception.NotFoundException;
import ya.practicum.blog.model.PostImage;
import ya.practicum.blog.service.PostService;

import java.io.IOException;
import java.util.Objects;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * Returns paginated post feed with search filters.
     */
    @GetMapping
    public PostListResponseDto getPosts(
            @RequestParam("search") String search,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    ) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    /**
     * Compatibility endpoint for frontend contract that uses POST for post details.
     */
    @PostMapping("/{id}")
    public PostResponseDto getPostCompat(@PathVariable("id") long id, HttpServletResponse response) {
        rememberLastPostId(response, id);
        return postService.getPost(id);
    }

    /**
     * Returns full post details by id.
     */
    @GetMapping("/{id}")
    public PostResponseDto getPost(@PathVariable("id") long id, HttpServletResponse response) {
        rememberLastPostId(response, id);
        return postService.getPost(id);
    }

    /**
     * Creates a new post.
     */
    @PostMapping
    public PostResponseDto createPost(@Valid @RequestBody PostUpsertRequestDto request) {
        return postService.createPost(request);
    }

    /**
     * Updates an existing post by id.
     */
    @PutMapping("/{id}")
    public PostResponseDto updatePost(@PathVariable("id") long id, @Valid @RequestBody PostUpsertRequestDto request) {
        return postService.updatePost(id, request);
    }

    /**
     * Deletes post and its comments.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable("id") long id) {
        postService.deletePost(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Increments post likes count and returns current value.
     */
    @PostMapping("/{id}/likes")
    public int incrementLikes(@PathVariable("id") long id) {
        return postService.incrementLikes(id);
    }

    /**
     * Updates post image.
     */
    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateImage(@PathVariable("id") long id, @RequestParam("image") MultipartFile image) throws IOException {
        postService.updatePostImage(id, image.getBytes(), image.getContentType());
        return ResponseEntity.ok().build();
    }

    /**
     * Returns post image binary content.
     */
    @GetMapping(value = "/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") long id) {
        PostImage image;
        try {
            image = postService.getPostImage(id);
        } catch (NotFoundException ex) {
            return ResponseEntity.noContent().build();
        }
        MediaType mediaType = image.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(Objects.requireNonNull(image.contentType()));
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(mediaType))
                .body(image.data());
    }

    private void rememberLastPostId(HttpServletResponse response, long id) {
        Cookie cookie = new Cookie("last_post_id", String.valueOf(id));
        cookie.setPath("/");
        // Keep short-lived compatibility state for frontend details page flow.
        cookie.setMaxAge(10 * 60);
        response.addCookie(cookie);
    }
}
