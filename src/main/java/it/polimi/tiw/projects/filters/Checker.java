package it.polimi.tiw.projects.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Checker implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(Checker.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        // If the user is not logged in (not present in session) redirect to the login
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;

        HttpSession session = req.getSession();
        logger.info("Session is new: {}", session.isNew());

        if (session.isNew() || session.getAttribute("user") == null) {
            logger.info("User is not logged in");
            String loginPath = req.getServletContext().getContextPath() + "/index.html";
            resp.sendRedirect(loginPath);
            return;
        }


        filterChain.doFilter(servletRequest, servletResponse);
    }
}
