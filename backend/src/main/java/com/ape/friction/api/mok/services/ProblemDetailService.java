package com.ape.friction.api.mok.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import com.ape.friction.api.interceptors.MethodCallLogged;


import java.io.IOException;
import java.net.URI;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@MethodCallLogged
public class ProblemDetailService {

    public static final String TAG_PREFIX = "tag:ssbd02,2026:";

    private final ObjectMapper objectMapper;

    private final MessageSource messageSource;

    private final TranslationExceptionService translationExceptionService;

    public void writeProblemDetail(HttpServletResponse response, HttpServletRequest request,
                                   HttpStatus responseStatus, String type, String code) throws IOException {
        ProblemDetail pd = createProblemDetail(responseStatus, type, code, null,request.getLocale());
        setInstance(pd, request);

        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(pd.getStatus());
        objectMapper.writeValue(response.getWriter(), pd);
    }

    public ProblemDetail createProblemDetail(HttpStatus status, String type, String code, Object[] args,Locale locale) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setType(URI.create(type));

        pd.setTitle(translationExceptionService.translateErrorText(messageSource.getMessage(code + ".title", args, locale), locale.toString().toLowerCase()));
        pd.setDetail(translationExceptionService.translateErrorText(messageSource.getMessage(code + ".detail", args, locale), locale.toString().toLowerCase()));

        return pd;
    }

    public ProblemDetail createProblemDetail(HttpStatus status, String type, String code,
                                             Object[] args, HttpServletRequest request) {
        ProblemDetail pd = createProblemDetail(status, type, code, args,request.getLocale());
        setInstance(pd, request);
        return pd;
    }

    public ProblemDetail createProblemDetail(HttpStatus status, Exception exception,
                                             Object[] args, HttpServletRequest request) {
        ProblemDetail pd = createProblemDetail(status, TAG_PREFIX + exception.getClass().getSimpleName().toLowerCase(), resolveMessageCode(exception), args,request.getLocale());
        setInstance(pd, request);
        return pd;
    }

    public ProblemDetail createProblemDetail(HttpStatus status, Exception exception, Object[] args, WebRequest request) {
        ProblemDetail pd = createProblemDetail(status, TAG_PREFIX + exception.getClass().getSimpleName().toLowerCase(), resolveMessageCode(exception), args,request.getLocale());
        pd.setInstance(URI.create(getFullUrl(request)));
        return pd;
    }

    private String resolveMessageCode(Exception ex) {
        String className = ex.getClass().getSimpleName();
        String baseName = className.replaceAll("Exception$", "");
        String kebabCase = baseName.replaceAll("(.)(\\p{Upper})", "$1-$2").toLowerCase(Locale.ROOT);
        String key = "error." + kebabCase;
        return key;
    }


    private String getFullUrl(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            StringBuffer url = httpRequest.getRequestURL();
            String query = httpRequest.getQueryString();
            if (query != null) {
                url.append('?').append(query);
            }
            return url.toString();
        }
        return null;
    }

    private void setInstance(ProblemDetail pd, HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }
        pd.setInstance(URI.create(url));
    }

}
