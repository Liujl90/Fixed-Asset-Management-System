package com.fixedasset.common.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixedasset.security.AuthenticatedUser;
import com.fixedasset.security.SecurityUtils;
import com.fixedasset.system.entity.SysOperationLog;
import com.fixedasset.system.mapper.SysOperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

@Aspect
@Component
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(SysOperationLogMapper operationLogMapper, ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(operationLog)")
    public Object record(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        // 切面只记录业务方法结果和耗时，不修改业务返回值。
        long startedAt = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable exception) {
            success = false;
            errorMessage = exception.getMessage();
            throw exception;
        } finally {
            saveLog(joinPoint, operationLog, success, errorMessage, System.currentTimeMillis() - startedAt);
        }
    }

    private void saveLog(
            ProceedingJoinPoint joinPoint,
            OperationLog annotation,
            boolean success,
            String errorMessage,
            long duration
    ) {
        try {
            // 日志失败不能反向影响已经成功的业务事务，因此保存日志时独立捕获异常。
            SysOperationLog log = new SysOperationLog();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
                log.setUserId(user.userId());
                log.setUsername(user.username());
            }
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                log.setMethod(attributes.getRequest().getMethod());
                log.setPath(attributes.getRequest().getRequestURI());
            }
            log.setModule(annotation.module());
            log.setAction(annotation.action());
            log.setParams(sanitize(Arrays.toString(joinPoint.getArgs())));
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);
            log.setDurationMs(duration);
            log.setCreatedAt(LocalDateTime.now());
            operationLogMapper.insert(log);
        } catch (Exception ignored) {
            // Logging must never break the business operation.
        }
    }

    private String sanitize(Object value) {
        String text;
        try {
            text = value instanceof String string ? string : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            text = String.valueOf(value);
        }
        // 参数入库前脱敏密码和 Token，并限制长度，避免大对象撑爆日志字段。
        text = text.replaceAll("(?i)(password|token)([=: ]+)[^,}\\]]+", "$1$2***");
        return text.length() > 1900 ? text.substring(0, 1900) : text;
    }
}
