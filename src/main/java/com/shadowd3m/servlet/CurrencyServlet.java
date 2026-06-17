package com.shadowd3m.servlet;

import com.google.gson.Gson;
import com.shadowd3m.datatransferobject.ModelCurrency;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/currency/*")
public class CurrencyServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String path = request.getPathInfo();
        System.out.println("CurrencyServlet called with pathInfo: " + request.getPathInfo());
        if (path == null || path.equals("/")){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Missing currency code\"}");
            return;
        }
        String code = path.substring(1).toUpperCase();
        if (code.isEmpty()){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Currency code is empty\"}");
            return;
        }
        String url = "jdbc:sqlite:currency.db";
        ModelCurrency mc = null;
        try(Connection connection = DriverManager.getConnection(url);
            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT * FROM Currencies WHERE code = ?")){
                stmt.setString(1, code);
                ResultSet rs = stmt.executeQuery();
                if(rs.next()){
                    mc = new ModelCurrency(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("full_name"),
                            rs.getString("sign")
                    );
                }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Currency not found\"}");
        }

        if (mc == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"message\": \"Currency not found\"}");
        }
        else{
            String json = new Gson().toJson(mc);
            response.getWriter().write(json);
        }

    }

}
