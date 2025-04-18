package it.polimi.tiw.projects.controllers;

import java.sql.*;

import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.UserDAO;
import it.polimi.tiw.projects.exceptions.DAOException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SignUp extends HttpServlet{

	private Connection connection;
	
	public void init() {
		try {
			ServletContext context = getServletContext();
			String user = context.getInitParameter("dbUsr");
			String password = context.getInitParameter("dbPassword");
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			
			Class.forName(driver);
			
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		UserDAO userDAO = new UserDAO(connection);
		
		String name = null;
		String surname = null;
		String username = null;
		String password = null;
		
		try {
			name = req.getParameter("sName");
			surname = req.getParameter("sSurname");
			username = req.getParameter("sUsername");
			password = req.getParameter("sPwd");
			
			if(name == null || surname == null || username == null || password == null || name.isEmpty() || surname.isEmpty() || password.isEmpty() || username.isEmpty()) {
				//TODO also check for falid values
			}
			
		} catch(Exception e) {
			//TODO
		} 
		
		try {
			userDAO.createUser(username, password, username, surname);
		} catch (DAOException e) {
			//TODO
		}
		
		User user = null;
		try {
			user = userDAO.checkCredentials(username, password);
			
	
		} catch (DAOException e) {
			//TODO
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














