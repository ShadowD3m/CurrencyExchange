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
import java.util.ArrayList;

@WebServlet("/exchangeRates")
public class ExchangeRatesServlet extends HttpServlet {

    private static final String URL = "jdbc:sqlite:currency.db";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
        response.setContentType("application/json;charset=UTF-8");


        ArrayList<ModelExchangeRatesResponse> list = new ArrayList<ModelExchangeRatesResponse>();

        try(
                Connection connection = DriverManager.getConnection(URL);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
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
                                "JOIN Currencies tc ON er.target_currency_id = tc.id"))
        {
            while(resultSet.next()){
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
                ModelExchangeRatesResponse exchangeRatesResponse = new ModelExchangeRatesResponse(
                        resultSet.getInt("id"),
                        baseCurrency,
                        targetCurrency,
                        resultSet.getDouble("rate")
                );
                list.add(exchangeRatesResponse);

            }
        } catch (SQLException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Database error\"}");
            return;
        }

        if(list.isEmpty()){
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"message\": \"No exchange rates found\"}");
            return;
        }

        String json = new Gson().toJson(list);

        response.getWriter().write(json);
        response.getWriter().flush();
        response.getWriter().close();
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,  IOException {
        response.setContentType("application/json;charset=UTF-8");
        String baseCode = request.getParameter("baseCurrencyCode");
        String targetCode = request.getParameter("targetCurrencyCode");
        String rateStr = request.getParameter("rate");
        if (baseCode == null || targetCode == null || rateStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid parameters\"}");
            return;
        }
        double rate;
        try{
            rate = Double.parseDouble(rateStr);

        } catch (NumberFormatException e){
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid parameters\"}");
            return;
        }

        int baseCurrencyId;
        int targetCurrencyId;
        try(Connection connection = DriverManager.getConnection(URL)){
            try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT id FROM Currencies WHERE code = ?")){
                stmt.setString(1, baseCode);
                ResultSet resultSet = stmt.executeQuery();
                if(resultSet.next()){
                    baseCurrencyId = resultSet.getInt("id");
                }
                else{
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Base currency not found\"}");
                    return;
                }

            }
            try(PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id FROM Currencies WHERE code = ?")){
                stmt.setString(1, targetCode);
                ResultSet resultSet = stmt.executeQuery();
                if(resultSet.next()){
                    targetCurrencyId = resultSet.getInt("id");
                }
                else{
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Target currency not found\"}");
                    return;
                }
            }

            try(PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO ExchangeRates (base_currency_id, target_currency_id, rate) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)){
                stmt.setInt(1, baseCurrencyId);
                stmt.setInt(2, targetCurrencyId);
                stmt.setDouble(3, rate);
                int insertedRows = stmt.executeUpdate();
                if (insertedRows == 0) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"message\": \"Failed to insert exchange rate\"}");
                    return;
                }
                int newId = 0;
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    newId = generatedKeys.getInt(1);
                }

                try (PreparedStatement selectStmt = connection.prepareStatement(
                        "SELECT " +
                                "er.id, er.rate, " +
                                "bc.id as base_id, bc.code as base_code, bc.full_name as base_full_name, bc.sign as base_sign, " +
                                "tc.id as target_id, tc.code as target_code, tc.full_name as target_full_name, tc.sign as target_sign " +
                                "FROM ExchangeRates er " +
                                "JOIN Currencies bc ON er.base_currency_id = bc.id " +
                                "JOIN Currencies tc ON er.target_currency_id = tc.id " +
                                "WHERE er.id = ?")){
                    selectStmt.setInt(1, newId);
                    ResultSet resultSet = selectStmt.executeQuery();
                    ModelCurrency baseCurrency;
                    ModelCurrency targetCurrency;
                    ModelExchangeRatesResponse exchangeRatesResponse = null;
                    if(resultSet.next()){
                        baseCurrency = new ModelCurrency(
                                resultSet.getInt("base_id"),
                                resultSet.getString("base_code"),
                                resultSet.getString("base_full_name"),
                                resultSet.getString("base_sign")
                        );
                        targetCurrency = new ModelCurrency(
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
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        response.getWriter().write("{\"message\": \"Failed to retrieve created exchange rate\"}");
                        return;
                    }

                    response.setStatus(HttpServletResponse.SC_CREATED);
                    response.setHeader("Location", "/exchangeRate/" + baseCode + targetCode);
                    String json = new Gson().toJson(exchangeRatesResponse);
                    response.getWriter().write(json);
                    response.getWriter().flush();
                    response.getWriter().close();
                }
            }
        }catch (SQLException e){
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write("{\"message\": \"Exchange rate already exists\"}");
            }
            else{
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("{\"message\": \"Database error\"}");
            }
        }
    }
}
