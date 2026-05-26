package ru.copperside.core.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.copperside.core.config.InternalAdminSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InternalAdminApiKeyFilter extends OncePerRequestFilter {

    private static final String TYPE_BASE = "https://contracts.newpay/errors/";

    private final InternalAdminSecurityProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public InternalAdminApiKeyFilter(InternalAdminSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (matchesConfiguredKey(request.getHeader(properties.headerName()))) {
            filterChain.doFilter(request, response);
            return;
        }

        writeUnauthorized(response);
    }

    private boolean matchesConfiguredKey(String provided) {
        if (provided == null || provided.isBlank()) {
            return false;
        }
        byte[] expectedBytes = properties.apiKey().getBytes(StandardCharsets.UTF_8);
        byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        ProblemDetail detail = new ProblemDetail(
                TYPE_BASE + "unauthorized",
                "Unauthorized",
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Missing or invalid internal admin API key",
                null,
                traceId
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ProblemEnvelope.of(detail));
    }
}
