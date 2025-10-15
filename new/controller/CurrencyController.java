

import model.CurrencyConverter;
import model.CurrencyManager;
import model.ExchangeHistory;

public class CurrencyController {
    private CurrencyManager currencyManager;
    private CurrencyConverter currencyConverter;
    private ExchangeHistory exchangeHistory;

    public CurrencyController(String ratesFile, String historyFile) {
        currencyManager = new CurrencyManager(ratesFile);
        currencyConverter = new CurrencyConverter(currencyManager);
        exchangeHistory = new ExchangeHistory();
        exchangeHistory.loadFromFile(historyFile);
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public CurrencyConverter getCurrencyConverter() {
        return currencyConverter;
    }

    public ExchangeHistory getExchangeHistory() {
        return exchangeHistory;
    }
}