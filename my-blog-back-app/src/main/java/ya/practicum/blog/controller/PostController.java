package ya.practicum.blog.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ya.practicum.blog.dto.PostListResponseDto;
import ya.practicum.blog.dto.PostResponseDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;
import ya.practicum.blog.model.PostImage;
import ya.practicum.blog.service.PostService;

import java.io.IOException;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public PostListResponseDto getPosts(
            @RequestParam String search,
            @RequestParam int pageNumber,
            @RequestParam int pageSize
    ) {
        return postService.getPosts(search, pageNumber, pageSize);
    }

    @PostMapping("/{id}")
    public PostResponseDto getPostCompat(@PathVariable long id) {
        return postService.getPost(id);
    }

    @GetMapping("/{id}")
    public PostResponseDto getPost(@PathVariable long id) {
        return postService.getPost(id);
    }

    @PostMapping
    public PostResponseDto createPost(@RequestBody PostUpsertRequestDto request) {
        return postService.createPost(request);
    }

    @PutMapping("/{id}")
    public PostResponseDto updatePost(@PathVariable long id, @RequestBody PostUpsertRequestDto request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable long id) {
        postService.deletePost(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/likes")
    public int incrementLikes(@PathVariable long id) {
        return postService.incrementLikes(id);
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateImage(@PathVariable long id, @RequestPart("image") MultipartFile image) throws IOException {
        postService.updatePostImage(id, image.getBytes(), image.getContentType());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable long id) {
        PostImage image = postService.getPostImage(id);
        String contentType = image.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : image.contentType();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(image.data());
    }
}
