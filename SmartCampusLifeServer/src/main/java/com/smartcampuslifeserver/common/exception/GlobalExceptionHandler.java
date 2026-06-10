package com.smartcampuslifeserver.common.exception;

import com.smartcampuslifeserver.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage(), e.getSubCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null
                        ? error.getDefaultMessage()
                        : error.getField() + " 参数校验失败")
                .orElse("请求参数错误");
        return Result.error(400, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                        HttpServletRequest request) {
        if (isUploadRequest(request.getServletPath())) {
            return Result.error(400, resolveMissingUploadMessage(request.getServletPath()));
        }
        return Result.error(400, "请求 Content-Type 不支持");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                     HttpServletRequest request) {
        if (isUploadRequest(request.getServletPath())) {
            return Result.error(400, resolveMissingUploadMessage(request.getServletPath()));
        }
        return Result.error(400, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(404, "请求的资源不存在");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        return Result.error(404, "请求的资源不存在");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e,
                                                    HttpServletRequest request) {
        return Result.error(400, resolveUploadSizeMessage(request.getServletPath()));
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipartException(MultipartException e, HttpServletRequest request) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof MaxUploadSizeExceededException) {
                return Result.error(400, resolveUploadSizeMessage(request.getServletPath()));
            }
            cause = cause.getCause();
        }
        log.warn("文件上传解析失败: {}", e.getMessage());
        return Result.error(400, "文件上传失败，请检查文件格式与大小");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器内部错误");
    }

    private boolean isUploadRequest(String path) {
        return path != null && (path.contains("/avatar") || path.contains("/import"));
    }

    private String resolveMissingUploadMessage(String path) {
        if (path != null && path.contains("/avatar")) {
            return "请上传头像文件";
        }
        if (path != null && path.contains("/import")) {
            return "请上传 Excel 文件";
        }
        return "请上传文件";
    }

    private String resolveUploadSizeMessage(String path) {
        if (path != null && path.contains("/avatar")) {
            return "头像文件大小不能超过5MB";
        }
        if (path != null && path.contains("/import")) {
            return "文件大小不能超过 2MB";
        }
        return "上传文件过大，头像最大5MB，课表最大2MB";
    }
}
