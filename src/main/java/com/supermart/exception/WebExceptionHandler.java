package com.supermart.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/** Global exception handling for the server-rendered (Thymeleaf) pages. */
@ControllerAdvice(basePackages = "com.supermart.web")
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException ex) {
        return view(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException ex) {
        return view(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView tooLarge(MaxUploadSizeExceededException ex) {
        return view(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large (maximum 2 MB).");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView integrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return view(HttpStatus.CONFLICT, "The change conflicts with existing data (for example a duplicate value).");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView generic(Exception ex) {
        log.error("Unexpected error", ex);
        return view(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    private ModelAndView view(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("message", message);
        return mav;
    }
}
