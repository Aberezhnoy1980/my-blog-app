package ya.practicum.blog.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ya.practicum.blog.dto.CommentResponseDto;
import ya.practicum.blog.service.CommentService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class CommentCompatController {
    private static final Pattern POST_ID_IN_REFERER = Pattern.compile("/posts/(\\d+)(?:$|[?#/])");

    private final CommentService commentService;

    public CommentCompatController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Frontend compatibility: during initial post details render, some bundle versions
     * request /api/posts/undefined/comments before post id is resolved.
     */
    @GetMapping("/api/posts/undefined/comments")
    public List<CommentResponseDto> getCommentsForUndefinedPostId(HttpServletRequest request) {
        Long postIdFromCookie = extractPostIdFromCookie(request.getCookies());
        if (postIdFromCookie != null) {
            return commentService.getComments(postIdFromCookie);
        }

        String referer = request.getHeader("Referer");
        if (referer != null) {
            Matcher matcher = POST_ID_IN_REFERER.matcher(referer);
            if (matcher.find()) {
                long postId = Long.parseLong(matcher.group(1));
                return commentService.getComments(postId);
            }
        }
        return List.of();
    }

    private Long extractPostIdFromCookie(Cookie[] cookies) {
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if ("last_post_id".equals(cookie.getName())) {
                try {
                    return Long.parseLong(cookie.getValue());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
