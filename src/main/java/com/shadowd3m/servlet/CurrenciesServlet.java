package com.shadowd3m.servlet;

import com.google.gson.Gson;
import com.shadowd3m.datatransferobject.ModelCurrency;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

@WebServlet("/currencies")
public class CurrenciesServlet extends HttpServlet {

    private static final String URL = "jdbc:sqlite:currency.db";

    @Override
    protected void doGet(HttpServletRequest requset, HttpServletResponse resp) throws IOException{
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        ArrayList<ModelCurrency> currencies = new ArrayList<>();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;

        try{
            String url = "jdbc:sqlite:currency.db";
            connection = DriverManager.getConnection(url);

            System.out.println("Connected to database successfully");

            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM Currencies");
            while(resultSet.next()){
                ModelCurrency currency = new ModelCurrency(
                        resultSet.getInt("id"),
                        resultSet.getString("code"),
                        resultSet.getString("full_name"),
                        resultSet.getString("sign")
                );
                currencies.add(currency);
            }
        }
        catch(SQLException e){
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        finally {
            if (resultSet != null) {
                try { resultSet.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (statement != null) {
                try { statement.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (connection != null) {
                try { connection.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        String json = new Gson().toJson(currencies);

        resp.getWriter().write(json);
        resp.getWriter().close();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
        response.setContentType("application/json;charset=UTF-8");
        String code = request.getParameter("code");
        String fullName = request.getParameter("name");
        String sign = request.getParameter("sign");
        if(code == null || fullName == null || sign == null){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid parameters\"}");
            return;
        }
        try(Connection connection = DriverManager.getConnection(URL);
            PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO Currencies (code, full_name, sign) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, code);
            stmt.setString(2, fullName);
            stmt.setString(3, sign);
            int insertedRows = stmt.executeUpdate();
            if (insertedRows == 0) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"message\": \"Failed to insert currency\"}");
                return;
            }
            int newId = 0;
            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                newId = generatedKeys.getInt(1);
            }
            try(PreparedStatement viewStmt = connection.prepareStatement("SELECT id, code, full_name, sign FROM Currencies WHERE id = ?")) {
                viewStmt.setInt(1, newId);
                ResultSet resultSet = viewStmt.executeQuery();
                ModelCurrency currency = null;
                if(resultSet.next()){
                    currency = new ModelCurrency(
                            resultSet.getInt("id"),
                            resultSet.getString("code"),
                            resultSet.getString("full_name"),
                            resultSet.getString("sign")
                    );
                }
                if (currency == null) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"message\": \"Failed to retrieve created currency\"}");
                    return;
                }

                response.setStatus(HttpServletResponse.SC_CREATED);
                response.setHeader("Location", "/currency/" +  code );
                String json = new Gson().toJson(currency);
                response.getWriter().write(json);
                response.getWriter().flush();
                response.getWriter().close();
            }



        }catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("{\"message\": \"Currency with this code already exists\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"message\": \"Database error\"}");
            }
        }
    }
}
