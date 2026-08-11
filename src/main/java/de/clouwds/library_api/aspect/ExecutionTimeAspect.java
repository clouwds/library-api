package de.clouwds.library_api.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ExecutionTimeAspect {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Pointcut("execution(* de.clouwds.library_api.controller.AuthController.login(..))")
    private void loginPointcut() {}

    @Pointcut("execution(* de.clouwds.library_api.service.*.*(..))")
    private void serviceLayerPointcut() {}

    @Around("loginPointcut()")
    public Object logBorrowBookExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.nanoTime() - start;
            LOG.debug("Execution time for login: {} ms", executionTime);
        }
    }

    @Around("serviceLayerPointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.nanoTime() - start;
            LOG.debug("Execution time: {} ms", executionTime);
        }
    }

}
