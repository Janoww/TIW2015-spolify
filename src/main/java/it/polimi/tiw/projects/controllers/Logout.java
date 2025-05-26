package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

public class Logout extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        // Take the session if it exists
        HttpSession session = req.getSession(false);

        // Invalidate session
        if (session != null) {
            session.invalidate();
        }

        // Redirect to the login page
        String path = getServletContext().getContextPath() + "/index.html";
        resp.sendRedirect(path);
    }
}
