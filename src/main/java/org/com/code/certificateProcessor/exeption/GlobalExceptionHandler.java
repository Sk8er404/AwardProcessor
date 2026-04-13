package org.com.code.certificateProcessor.exeption;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * MethodArgumentNotValidException
     *
     * 触发条件：@RequestBody 对象校验失败（DTO 对象上的注解，如 @NotNull、@Size）。
     *
     * 可以通过 getBindingResult().getAllErrors() 获取每个字段的错误信息。
     *
     * ConstraintViolationException
     *
     * 触发条件：@Validated 注解在方法参数上（非 @RequestBody，例如 @RequestParam、@PathVariable）或者自定义注解校验失败。
     *
     * 可以通过 ex.getConstraintViolations() 遍历获取具体的参数错误。
     * @param ex
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        // 遍历所有校验错误
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));

        // 类级别错误（比如你的 @AtLeastOneIsValid）
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> errors.put("global", e.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // 获取参数路径，例如方法名+参数名
            String field = violation.getPropertyPath().toString();
            errors.put(field, violation.getMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }


    @ExceptionHandler(StudentTableException.class)
    public ResponseEntity<Object> handleDatabaseException(StudentTableException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 数据库 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
            // System.out.println("【业务提示】" + ex.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("数据库异常：" + ex.getMessage());
    }

    @ExceptionHandler(AwardSubmissionException.class)
    public ResponseEntity<Object> handleAwardSubmissionException(AwardSubmissionException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 奖状提交 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("奖状提交模块异常：" + ex.getMessage());
    }

    @ExceptionHandler(AdminTableException.class)
    public ResponseEntity<Object> handleAdminException(AdminTableException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 数据库 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("数据库异常：" + ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Object> handleResourceNotFoundException(ResourceNotFoundException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】资源未找到，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("资源未找到：" + ex.getMessage());
    }

    @ExceptionHandler(OSSException.class)
    public ResponseEntity<Object> handleOSSException(OSSException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 OSS对象存储 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("对象存储服务异常：" + ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 上传 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("上传文件大小超出限制：" + ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorizedException(UnauthorizedException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 认证 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ex.getMessage());
    }



    @ExceptionHandler(CredentialsException.class)
    public ResponseEntity<Object> handleCredentialsException(CredentialsException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 登录类型 异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
            // 可以选择打印一条简单的 INFO 日志，或者什么都不做
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ex.getMessage());
    }


    @ExceptionHandler(AIModelException.class)
    public void handleAIModelException(AIModelException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 AI 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
        }
    }

    @ExceptionHandler(ElasticSearchException.class)
    public void handleElasticSearchException(ElasticSearchException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 Elastic 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
        }
    }

    @ExceptionHandler(RocketmqException.class)
    public void handleRocketmqException(RocketmqException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 Rocketmq 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
        }
    }


    @ExceptionHandler(StandardAwardException.class)
    public void handleStandardAwardException(StandardAwardException ex) {
        if (ex.getCause() != null) {
            // 这是一个系统级错误，需要记录详细堆栈以便排查
            System.err.println("【系统错误】检测到 StandardAward 服务异常，打印堆栈:");
            ex.printStackTrace();
        } else {
            // 这是一个纯业务错误 (cause == null)，不需要打印堆栈，保持日志干净
        }
    }

}
