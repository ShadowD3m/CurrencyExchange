package com.shadowd3m.servlet;

import com.google.gson.Gson;
import com.shadowd3m.datatransferobject.ModelCurrency;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/exchange")
public class ExchangeServlet extends HttpServlet {

    private static final String URL = "jdbc:sqlite:currency.db";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
        response.setContentType("application/json;charset=UTF-8");
        String from = request.getParameter("from");
        String to = request.getParameter("to");
        String amountStr = request.getParameter("amount");

        if (from == null || to == null || amountStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Missing required parameters: from, to, amount\"}");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"message\": \"Invalid amount format\"}");
            return;
        }

        int fromId = 0;
        int toId = 0;

        try (Connection connection = DriverManager.getConnection(URL)) {
            try(PreparedStatement stmt = connection.prepareStatement("SELECT id FROM Currencies WHERE code = ?")){
                stmt.setString(1, from);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    fromId =  rs.getInt("id");
                }
                else{
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Base currency not found\"}");
                    return;
                }
            }
            try(PreparedStatement stmt = connection.prepareStatement("SELECT id FROM Currencies WHERE code = ?")){
                stmt.setString(1, to);
                ResultSet rs2 = stmt.executeQuery();
                if (rs2.next()) {
                    toId =  rs2.getInt("id");
                }
                else{
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.getWriter().write("{\"message\": \"Target currency not found\"}");
                    return;
                }
            }
            double rate = 0.0;
            boolean found = false;
            try(PreparedStatement stmt = connection.prepareStatement("SELECT rate FROM ExchangeRates WHERE base_currency_id = ? AND " +
                    "target_currency_id = ?")){
                stmt.setInt(1, fromId);
                stmt.setInt(2, toId);
                ResultSet rs3 = stmt.executeQuery();
                if (rs3.next()) {
                    rate = rs3.getDouble("rate");
                    found = true;
                }
            }
            if (!found) {
                try (PreparedStatement stmt = connection.prepareStatement("SELECT rate FROM ExchangeRates WHERE base_currency_id = ? AND " +
                        "target_currency_id = ?")){
                    stmt.setInt(1, toId);
                    stmt.setInt(2, fromId);
                    ResultSet rs3 = stmt.executeQuery();
                    if (rs3.next()) {
                        rate = rs3.getDouble("rate");
                        found = true;
                    }
                }
            }
            int usdId = 0;
            if (!found) {
                try(Statement stmt = connection.createStatement()){
                    ResultSet resultSet = stmt.executeQuery("SELECT id FROM Currencies WHERE code = 'USD'");
                    if (resultSet.next()) {
                        usdId = resultSet.getInt("id");
                    }
                    double rateUsdFrom = 0.0;
                    try(PreparedStatement stmt2 = connection.prepareStatement("SELECT rate FROM ExchangeRates WHERE base_currency_id = ? AND " +
                            "target_currency_id = ?")){
                        stmt2.setInt(1, usdId);
                        stmt2.setInt(2, fromId);
                        resultSet = stmt2.executeQuery();
                        if (resultSet.next()) {
                            rateUsdFrom = resultSet.getDouble("rate");
                        }
                    }
                    double rateUsdTo = 0.0;
                    try(PreparedStatement stmt2 = connection.prepareStatement("SELECT rate FROM ExchangeRates WHERE base_currency_id = ? AND " +
                            "target_currency_id = ?")){
                        stmt2.setInt(1, usdId);
                        stmt2.setInt(2, toId);
                        resultSet = stmt2.executeQuery();
                        if (resultSet.next()) {
                            rateUsdTo = resultSet.getDouble("rate");
                        }
                    }

                    if (rateUsdFrom > 0 && rateUsdTo > 0) {
                        rate = rateUsdTo / rateUsdFrom;
                        found = true;
                    }
                }

                // найти ID USD
                // найти курс USD → from
                // найти курс USD → to
                // если оба есть → rate = rateUSD_to / rateUSD_from, found = true
            }
            if (!found) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"message\": \"Exchange rate not found\"}");
                return;
            }

            double convertedAmount = amount * rate;
            ModelCurrency currencyFrom = null;
            ModelCurrency currencyTo = null;
            try(PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Currencies WHERE code = ?")){
                stmt.setString(1, from);
                ResultSet rs2 = stmt.executeQuery();
                if (rs2.next()) {
                    currencyFrom = new ModelCurrency(
                            rs2.getInt("id"),
                            rs2.getString("code"),
                            rs2.getString("full_name"),
                            rs2.getString("sign")
                    );
                }
            }
            try(PreparedStatement stmt = connection.prepareStatement("SELECT * FROM Currencies WHERE code = ?")){
                stmt.setString(1, to);
                ResultSet rs2 = stmt.executeQuery();
                if (rs2.next()) {
                    currencyTo = new ModelCurrency(
                            rs2.getInt("id"),
                            rs2.getString("code"),
                            rs2.getString("full_name"),
                            rs2.getString("sign")
                    );
                }
            }
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("baseCurrency", currencyFrom);
            responseMap.put("targetCurrency", currencyTo);
            responseMap.put("rate", rate);
            responseMap.put("amount", amount);
            responseMap.put("convertedAmount", convertedAmount);

            String json = new Gson().toJson(responseMap);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(json);


        }catch(SQLException e){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"message\": \"Database error\"}");
            return;
        }
    }
}
