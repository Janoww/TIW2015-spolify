package it.polimi.tiw.projects.controllers;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;


public class CheckLogin extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	@Override
	public void init() throws ServletException {
		try {
		ServletContext context = getServletContext();
		String driver = context.getInitParameter("dbDriver");
		String url = context.getInitParameter("dbUrl");
		String user = context.getInitParameter("dbUser");
		String password = context.getInitParameter("dbPassword");
		
		Class.forName(driver); //In previous versions of JDBC, to obtain a connection, you first had to initialize your JDBC driver by calling the method Class.forName
		
		connection = DriverManager.getConnection(url, user, password);
		
		} catch(SQLException e) {
			//TODO
		} catch(ClassNotFoundException e) {
			//TODO
		}
		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		UserDAO userDAO = new UserDAO(connection);
		User user = null;
		
		String username = null;
		String password = null;
		
		try {
			
			username = req.getParameter("lUsername");
			password = req.getParameter("lPwd");
		
			if(username == null || password == null || username.isEmpty() || password.isEmpty()) {
				//TODO
			}
		} catch (Exception e) {
			//TODO
		}
		
		try {
			user = userDAO.checkCredentials(username, password);
		} catch (DAOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			req.getSession().setAttribute("user", user);
			String path = getServletContext().getContextPath() + "/Home";
			resp.sendRedirect(path);
		} catch (Exception e) {
			//TODO
		}
		
	}


	
	

	
	
}