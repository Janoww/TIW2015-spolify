package it.polimi.tiw.projects.utils;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TemplateHandler {

	public static TemplateEngine initializeEngine(ServletContext context) {
		// In Thymeleaf 3.1+, they introduced a new, flexible abstraction layer for web
		// environments called WebApplication, It wraps the standard ServletContext in a
		// higher-level abstraction that Thymeleaf understands.
		JakartaServletWebApplication webApplication =
				JakartaServletWebApplication.buildApplication(context);
		// We pass the webApplication to new
		// WebApplicationTemplateResolver(webApplication) so the template resolver knows
		// how to find and load HTML templates from your deployed web app.
		WebApplicationTemplateResolver templateResolver =
				new WebApplicationTemplateResolver(webApplication);
		// HTML is the default mode, but we will set it anyway for better understanding
		// of code
		templateResolver.setTemplateMode(TemplateMode.HTML);
		// This will convert "home" to "home.html"
		templateResolver.setSuffix(".html");

		TemplateEngine templateEngine = new TemplateEngine();
		templateEngine.setTemplateResolver(templateResolver);

		return templateEngine;
	}

	public static WebContext getWebContext(HttpServletRequest req, HttpServletResponse resp,
			ServletContext context) {

		JakartaServletWebApplication webApplication =
				JakartaServletWebApplication.buildApplication(context);

		// Contexts should contain all the data required for an execution of the
		// template engine in a variables map, and also reference the locale that must
		// be used for externalized messages.
		return new WebContext(webApplication.buildExchange(req, resp), req.getLocale());
	}

}
