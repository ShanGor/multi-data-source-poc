package com.example.demo.filter;

import com.example.demo.configuration.DataSourceConfig;
import com.example.demo.configuration.ThreadLocalStorage;
import com.example.demo.exception.AppException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(2)
public class MultiDBRequestFilter implements Filter {

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        try {
            getDatasourceName(request);
            chain.doFilter(request, response);
        } catch (AppException e) {
            HttpServletResponse resp = (HttpServletResponse)response;
            resp.setStatus(e.getHttpStatus());
            resp.getWriter().println(e.getMessage());
            resp.flushBuffer();
        }
    }

    private void getDatasourceName(final ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        String entity = req.getHeader("X-Entity");
        if (entity == null) {
            entity = req.getHeader("X-ENTITY");
        }
        if (entity == null) {
            entity = req.getHeader("x-entity");
        }
        if (entity == null) {
            throw new AppException(400, "You need to specify the Header X-Entity");
        } else {
            if (DataSourceConfig.supportedEntities.contains(entity)) {
                ThreadLocalStorage.setDBName(entity);
            } else {
                throw new AppException(400, "Unknown Entity in X-Entity: " + entity);
            }
        }
    }

}
