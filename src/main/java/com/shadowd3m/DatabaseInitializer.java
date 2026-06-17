package com.shadowd3m;

import java.sql.*;

public class DatabaseInitializer {
    public static void main(String[] args) throws SQLException {
        String url = "jdbc:sqlite:currency.db";
        Connection connection = DriverManager.getConnection(url);
        System.out.println("Connected to database successfully");


        Statement statement = connection.createStatement();
        statement.executeUpdate("PRAGMA foreign_keys=ON");

        ResultSet resultSet = statement.executeQuery("SELECT * FROM Currencies");
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            String code = resultSet.getString("code");
            String fullName = resultSet.getString("full_name");
            String sign = resultSet.getString("sign");
            System.out.println(id + " | " + code + " | " + fullName + " | " + sign);
        }

        resultSet = statement.executeQuery("SELECT * FROM ExchangeRates");
        while (resultSet.next()) {
            int id = resultSet.getInt("id");
            int baseCurrencyId = resultSet.getInt("base_currency_id");
            int targetCurrencyId = resultSet.getInt("target_currency_id");
            double exchangeRate = resultSet.getDouble("rate");
            System.out.println(id + " | " + baseCurrencyId + " | " + targetCurrencyId + " | " + exchangeRate);
        }
        resultSet.close();
        statement.close();
        connection.close();
    }
}
