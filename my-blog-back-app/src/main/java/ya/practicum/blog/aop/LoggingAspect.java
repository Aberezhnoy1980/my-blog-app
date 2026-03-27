package ya.practicum.blog.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("within(ya.practicum.blog.controller..*) || within(ya.practicum.blog.service..*)")
    public Object logMethodInvocation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        String args = formatArgs(joinPoint.getArgs());
        long startedAt = System.currentTimeMillis();

        log.info("-> {} args=[{}]", method, args);
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - startedAt;
            log.info("<- {} ok ({} ms)", method, elapsed);
            return result;
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - startedAt;
            log.error("<- {} failed ({} ms): {}", method, elapsed, ex.getMessage(), ex);
            throw ex;
        }
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        return Arrays.stream(args)
                .map(this::sanitizeArg)
                .collect(Collectors.joining(", "));
    }

    private String sanitizeArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof byte[] bytes) {
            return "byte[" + bytes.length + "]";
        }
        if (arg instanceof MultipartFile file) {
            return "MultipartFile{name=" + file.getOriginalFilename() + ", size=" + file.getSize() + "}";
        }
        String value = String.valueOf(arg);
        return value.length() > 250 ? value.substring(0, 250) + "..." : value;
    }
}
