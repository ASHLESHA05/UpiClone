// src/main/java/com/xai/upi/bank/filters/FonltHeaderFilter.java
package com.xai.upi.bank.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class FonltHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path != null && path.startsWith("/api/ipc/")) {
            String fonlt = request.getHeader("fonlt");
            if (fonlt == null || fonlt.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Missing fonlt header\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}