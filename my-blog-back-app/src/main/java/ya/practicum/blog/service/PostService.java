package ya.practicum.blog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ya.practicum.blog.dto.PostListResponseDto;
import ya.practicum.blog.dto.PostResponseDto;
import ya.practicum.blog.dto.PostUpsertRequestDto;
import ya.practicum.blog.exception.BadRequestException;
import ya.practicum.blog.exception.NotFoundException;
import ya.practicum.blog.model.Post;
import ya.practicum.blog.model.PostImage;
import ya.practicum.blog.repository.PostRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PostService {
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_POST_TEXT_LENGTH = 10_000;
    private static final int MAX_TAG_LENGTH = 64;
    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public PostListResponseDto getPosts(String search, int pageNumber, int pageSize) {
        validatePaging(pageNumber, pageSize);
        SearchFilter filter = parseSearch(search);
        List<Post> filtered = postRepository.findAll().stream()
                .filter(post -> matches(post, filter))
                .toList();

        int total = filtered.size();
        int lastPage = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
        int fromIndex = Math.min((pageNumber - 1) * pageSize, total);
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<PostResponseDto> items = filtered.subList(fromIndex, toIndex).stream()
                .map(post -> toPostResponse(post, true))
                .toList();
        return new PostListResponseDto(items, pageNumber > 1, pageNumber < lastPage, lastPage);
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(long id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        return toPostResponse(post, false);
    }

    @Transactional
    public PostResponseDto createPost(PostUpsertRequestDto request) {
        validatePostRequest(request, false);
        Post toCreate = new Post(
                null,
                request.title().trim(),
                request.text().trim(),
                request.tags(),
                0,
                0
        );
        return toPostResponse(postRepository.create(toCreate), false);
    }

    @Transactional
    public PostResponseDto updatePost(long id, PostUpsertRequestDto request) {
        validatePostRequest(request, true);
        if (request.id() != null && request.id() != id) {
            throw new BadRequestException("Path id and body id must be equal");
        }
        postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        Post toUpdate = new Post(
                id,
                request.title().trim(),
                request.text().trim(),
                request.tags(),
                0,
                0
        );
        return toPostResponse(postRepository.update(id, toUpdate), false);
    }

    @Transactional
    public void deletePost(long id) {
        if (postRepository.deleteById(id) == 0) {
            throw new NotFoundException("Post not found");
        }
    }

    @Transactional
    public int incrementLikes(long id) {
        postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        return postRepository.incrementLikes(id);
    }

    @Transactional
    public void updatePostImage(long id, byte[] imageData, String contentType) {
        if (imageData == null || imageData.length == 0) {
            throw new BadRequestException("Image is empty");
        }
        if (imageData.length > MAX_IMAGE_SIZE_BYTES) {
            throw new BadRequestException("Image is too large");
        }
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Unsupported image content type");
        }
        if (postRepository.updateImage(id, imageData, contentType) == 0) {
            throw new NotFoundException("Post not found");
        }
    }

    @Transactional(readOnly = true)
    public PostImage getPostImage(long id) {
        postRepository.findById(id).orElseThrow(() -> new NotFoundException("Post not found"));
        Optional<PostImage> image = postRepository.findImage(id);
        return image.orElseThrow(() -> new NotFoundException("Post image not found"));
    }

    private void validatePaging(int pageNumber, int pageSize) {
        if (pageNumber < 1 || pageSize < 1) {
            throw new BadRequestException("pageNumber and pageSize must be positive");
        }
    }

    private void validatePostRequest(PostUpsertRequestDto request, boolean idAllowed) {
        if (request == null) {
            throw new BadRequestException("Request body is required");
        }
        if (!idAllowed && request.id() != null) {
            throw new BadRequestException("id must be empty for create");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("title is required");
        }
        if (request.title().trim().length() > MAX_TITLE_LENGTH) {
            throw new BadRequestException("title is too long");
        }
        if (request.text() == null || request.text().isBlank()) {
            throw new BadRequestException("text is required");
        }
        if (request.text().trim().length() > MAX_POST_TEXT_LENGTH) {
            throw new BadRequestException("text is too long");
        }
        if (request.tags() == null) {
            throw new BadRequestException("tags is required");
        }
        if (request.tags().isEmpty()) {
            throw new BadRequestException("tags must contain at least one tag");
        }
        boolean hasInvalidTag = request.tags().stream()
                .anyMatch(tag -> tag == null || tag.isBlank() || tag.trim().length() > MAX_TAG_LENGTH);
        if (hasInvalidTag) {
            throw new BadRequestException("tags contain invalid values");
        }
    }

    private PostResponseDto toPostResponse(Post post, boolean truncateText) {
        String text = truncateText ? truncate(post.text(), 128) : post.text();
        return new PostResponseDto(
                post.id(),
                post.title(),
                text,
                post.tags(),
                post.likesCount(),
                post.commentsCount()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private SearchFilter parseSearch(String search) {
        if (search == null || search.isBlank()) {
            return new SearchFilter(List.of(), "");
        }
        String[] tokens = search.trim().split("\\s+");
        List<String> tags = new ArrayList<>();
        List<String> titleTerms = new ArrayList<>();
        for (String token : tokens) {
            if (token.startsWith("#") && token.length() > 1) {
                tags.add(token.substring(1).toLowerCase(Locale.ROOT));
            } else {
                titleTerms.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return new SearchFilter(tags, String.join(" ", titleTerms));
    }

    private boolean matches(Post post, SearchFilter filter) {
        boolean titleOk = filter.titleQuery().isBlank()
                || post.title().toLowerCase(Locale.ROOT).contains(filter.titleQuery());
        boolean tagsOk = filter.tags().isEmpty()
                || post.tags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(filter.tags());
        return titleOk && tagsOk;
    }

    private record SearchFilter(List<String> tags, String titleQuery) {
    }
}
