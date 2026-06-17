package com.shadowd3m.servlet;

import com.google.gson.Gson;
import com.shadowd3m.datatransferobject.ModelCurrency;
import com.shadowd3m.datatransferobject.ModelExchangeRatesResponse;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

@WebServlet("/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {

    private static final String URL = "jdbc:sqlite:currency.db";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String path = request.getPathInfo();
        if(path == null || path.equals("/")){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Missing currency code\"}");
            return;
        }
        String pair = path.substring(1);

        if(pair.length() != 6){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid currency pair format\"}");
            return;
        }

        String baseCode = pair.substring(0, 3).toUpperCase();
        String targetCode = pair.substring(3).toUpperCase();
        ModelExchangeRatesResponse exchangeRatesResponse;
        try(Connection connection = DriverManager.getConnection(URL)) {
            try(PreparedStatement stmt = connection.prepareStatement(
                    "SELECT " +
                    "er.id, er.rate, " +
                    "bc.id as base_id, " +
                    "bc.code as base_code, " +
                    "bc.full_name as base_full_name, " +
                    "bc.sign as base_sign, " +
                    "tc.id as target_id, " +
                    "tc.code as target_code, " +
                    "tc.full_name as target_full_name, " +
                    "tc.sign as target_sign " +
                    "FROM ExchangeRates er " +
                    "JOIN Currencies bc ON er.base_currency_id = bc.id " +
                    "JOIN Currencies tc ON er.target_currency_id = tc.id " +
                    "WHERE bc.code = ? AND tc.code = ?"
            )){
                stmt.setString(1, baseCode);
                stmt.setString(2, targetCode);
                try(ResultSet resultSet = stmt.executeQuery()){
                    if (resultSet.next()) {
                        ModelCurrency baseCurrency = new ModelCurrency(
                                resultSet.getInt("base_id"),
                                resultSet.getString("base_code"),
                                resultSet.getString("base_full_name"),
                                resultSet.getString("base_sign")
                        );
                        ModelCurrency targetCurrency = new ModelCurrency(
                                resultSet.getInt("target_id"),
                                resultSet.getString("target_code"),
                                resultSet.getString("target_full_name"),
                                resultSet.getString("target_sign")
                        );
                        exchangeRatesResponse = new ModelExchangeRatesResponse(
                                resultSet.getInt("id"),
                                baseCurrency,
                                targetCurrency,
                                resultSet.getDouble("rate")
                        );

                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        response.getWriter().write("{\"message\": \"Exchange rate not found\"}");
                        return;
                    }
                }
            }

            String json = new Gson().toJson(exchangeRatesResponse);

            response.getWriter().write(json);
            response.getWriter().flush();
            response.getWriter().close();

        }catch(SQLException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Database error\"}");
            return;
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            doPatch(request, response);
        }
        else{
            super.service(request, response);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println("=== doPatch called ===");
        System.out.println("path: " + request.getPathInfo());
        System.out.println("rate from getParameter: " + request.getParameter("rate"));
        System.out.println("All parameters: " + request.getParameterMap());
        response.setContentType("application/json;charset=UTF-8");
        String path = request.getPathInfo();
        if(path == null || path.equals("/")){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Missing currency code\"}");
            return;
        }
        String pair = path.substring(1);

        if(pair.length() != 6){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid currency pair format\"}");
            return;
        }

        String baseCode = pair.substring(0, 3).toUpperCase();
        String targetCode = pair.substring(3).toUpperCase();


        StringBuilder sb = new StringBuilder();
        String line;
        try (java.io.BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();

        if (body == null || body.isEmpty() || !body.startsWith("rate=")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Rate is required\"}");
            return;
        }

        String rateValue = body.substring(5);
        double rate;
        try{
            rate = Double.parseDouble(rateValue);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid rate format\"}");
            return;
        }

        int baseCurrencyId = 0;
        int targetCurrencyId = 0;

        try(Connection connection = DriverManager.getConnection(URL)){
            try(PreparedStatement stmt = connection.prepareStatement("SELECT id FROM Currencies WHERE code = ?")){
                stmt.setString(1, baseCode);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    baseCurrencyId = resultSet.getInt("id");
                }
                else{
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Base currency not found\"}");
                    return;
                }
            }
            try(PreparedStatement stmt = connection.prepareStatement("SELECT id FROM Currencies WHERE code = ?")){
                stmt.setString(1, targetCode);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    targetCurrencyId = resultSet.getInt("id");
                }
                else{
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Target currency not found\"}");
                    return;
                }
            }

            if (baseCurrencyId == 0 ||  targetCurrencyId == 0) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"message\": \"Currency not found\"}");
                return;
            }

            try(PreparedStatement stmt = connection.prepareStatement("UPDATE ExchangeRates SET rate = ? WHERE base_currency_id = ? AND target_currency_id = ?")){
                stmt.setDouble(1, rate);
                stmt.setInt(2, baseCurrencyId);
                stmt.setInt(3, targetCurrencyId);

                int updateRows = stmt.executeUpdate();
                if (updateRows == 0){
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Exchange rate not found\"}");
                    return;
                }
            }

            try(PreparedStatement stmt = connection.prepareStatement("SELECT " +
                    "er.id, er.rate, " +
                    "bc.id as base_id, bc.code as base_code, bc.full_name as base_full_name, bc.sign as base_sign, " +
                    "tc.id as target_id, tc.code as target_code, tc.full_name as target_full_name, tc.sign as target_sign " +
                    "FROM ExchangeRates er " +
                    "JOIN Currencies bc ON er.base_currency_id = bc.id " +
                    "JOIN Currencies tc ON er.target_currency_id = tc.id " +
                    "WHERE bc.code = ? AND tc.code = ?")){
                stmt.setString(1, baseCode);
                stmt.setString(2, targetCode);
                ResultSet resultSet = stmt.executeQuery();
                ModelExchangeRatesResponse exchangeRatesResponse = null;
                if (resultSet.next()) {
                    ModelCurrency baseCurrency = new ModelCurrency(
                            resultSet.getInt("base_id"),
                            resultSet.getString("base_code"),
                            resultSet.getString("base_full_name"),
                            resultSet.getString("base_sign")
                    );
                    ModelCurrency targetCurrency = new ModelCurrency(
                            resultSet.getInt("target_id"),
                            resultSet.getString("target_code"),
                            resultSet.getString("target_full_name"),
                            resultSet.getString("target_sign")
                    );
                    exchangeRatesResponse = new ModelExchangeRatesResponse(
                            resultSet.getInt("id"),
                            baseCurrency,
                            targetCurrency,
                            resultSet.getDouble("rate")
                    );
                }

                if (exchangeRatesResponse == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Exchange rate not found\"}");
                    return;
                }

                String json = new Gson().toJson(exchangeRatesResponse);
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(json);
                return;
            }


        }catch(SQLException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Database error\"}");
            return;
        }

    }
}

