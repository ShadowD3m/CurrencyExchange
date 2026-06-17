package com.shadowd3m;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws IOException {

        resp.setContentType("text/plain");
        resp.getWriter().println("my favorite text!");

        resp.getWriter().flush();
        resp.getWriter().close();
    }
}
