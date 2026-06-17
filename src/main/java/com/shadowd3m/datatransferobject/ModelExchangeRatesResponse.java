package com.shadowd3m.datatransferobject;

public class ModelExchangeRatesResponse {

    private int id;
    private ModelCurrency baseCurrency;
    private ModelCurrency targetCurrency;
    private double rate;

    public ModelExchangeRatesResponse(int id, ModelCurrency baseCurrency, ModelCurrency targetCurrency, double rate) {
        this.id = id;
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
    }

    public ModelExchangeRatesResponse() {}

    public ModelExchangeRatesResponse(ModelCurrency baseCurrency, ModelCurrency targetCurrency, double rate) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public ModelCurrency getBaseCurrency() {
        return baseCurrency;
    }
    public void setBaseCurrency(ModelCurrency baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public ModelCurrency getTargetCurrency() {
        return targetCurrency;
    }
    public void setTargetCurrency(ModelCurrency targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

}


