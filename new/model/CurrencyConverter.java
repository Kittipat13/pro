package model;

public class CurrencyConverter {
    private CurrencyManager currencyManager;

    public CurrencyConverter(CurrencyManager currencyManager) {
        this.currencyManager = currencyManager;
    }

    public double convert(String fromCode, String toCode, double amount) {
        Currency from = currencyManager.getCurrency(fromCode);
        Currency to = currencyManager.getCurrency(toCode);

        if (from == null || to == null) throw new IllegalArgumentException("Invalid currency code");

        double amountInBase = amount * from.getRate();
        return amountInBase / to.getRate();
    }
}