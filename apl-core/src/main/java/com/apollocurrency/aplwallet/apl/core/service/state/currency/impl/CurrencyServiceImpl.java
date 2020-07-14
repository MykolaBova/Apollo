/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.currency.impl;

import com.apollocurrency.aplwallet.apl.core.converter.rest.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencySupplyTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.currency.CurrencyTable;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountCurrency;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyBuyOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyFounder;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySellOffer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencySupply;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyTransfer;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.CurrencyType;
import com.apollocurrency.aplwallet.apl.core.entity.state.exchange.Exchange;
import com.apollocurrency.aplwallet.apl.core.service.state.BlockChainInfoService;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyExchangeOfferFacade;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyFounderService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyMintService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyTransferService;
import com.apollocurrency.aplwallet.apl.core.service.state.exchange.ExchangeService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MonetarySystemCurrencyIssuance;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencySupplyTable currencySupplyTable;
    private final CurrencyTable currencyTable;
    private final BlockChainInfoService blockChainInfoService;
    private final IteratorToStreamConverter<CurrencySellOffer> iteratorToStreamConverter;
    private final AccountService accountService;
    private final AccountCurrencyService accountCurrencyService;
    private final CurrencyExchangeOfferFacade currencyExchangeOfferFacade;
    private final CurrencyFounderService currencyFounderService;
    private final ExchangeService exchangeService;
    private final CurrencyTransferService currencyTransferService;
    private final ShufflingService shufflingService;
    private CurrencyMintService currencyMintService; // lazy init to break up circular dependency

    @Inject
    public CurrencyServiceImpl(CurrencySupplyTable currencySupplyTable,
                               CurrencyTable currencyTable,
                               BlockChainInfoService blockChainInfoService,
                               AccountService accountService,
                               AccountCurrencyService accountCurrencyService,
                               CurrencyExchangeOfferFacade currencyExchangeOfferFacade,
                               CurrencyFounderService currencyFounderService,
                               ExchangeService exchangeService,
                               CurrencyTransferService currencyTransferService,
                               ShufflingService shufflingService) {
        this.currencySupplyTable = currencySupplyTable;
        this.currencyTable = currencyTable;
        this.blockChainInfoService = blockChainInfoService;
        this.iteratorToStreamConverter = new IteratorToStreamConverter<>();
        this.accountService = accountService;
        this.accountCurrencyService = accountCurrencyService;
        this.currencyExchangeOfferFacade = currencyExchangeOfferFacade;
        this.currencyFounderService = currencyFounderService;
        this.exchangeService = exchangeService;
        this.currencyTransferService = currencyTransferService;
        this.shufflingService = shufflingService;
    }

    @Override
    public DbIterator<Currency> getAllCurrencies(int from, int to) {
        return currencyTable.getAll(from, to);
    }

    @Override
    public int getCount() {
        return currencyTable.getCount();
    }

    @Override
    public Currency getCurrency(long id) {
        return currencyTable.get(CurrencyTable.currencyDbKeyFactory.newKey(id));
    }

    @Override
    public Currency getCurrencyByName(String name) {
        return currencyTable.getBy(new DbClause.StringClause("name_lower", name.toLowerCase()));
    }

    @Override
    public Currency getCurrencyByCode(String code) {
        return currencyTable.getBy(new DbClause.StringClause("code", code.toUpperCase()));
    }

    @Override
    public DbIterator<Currency> getCurrencyIssuedBy(long accountId, int from, int to) {
        return currencyTable.getManyBy(new DbClause.LongClause("account_id", accountId), from, to);
    }

    @Override
    public DbIterator<Currency> searchCurrencies(String query, int from, int to) {
        return currencyTable.search(query, DbClause.EMPTY_CLAUSE, from, to, " ORDER BY ft.score DESC, currency.creation_height DESC ");
    }

    @Override
    public DbIterator<Currency> getIssuedCurrenciesByHeight(int height, int from, int to) {
        return currencyTable.getManyBy(new DbClause.IntClause("issuance_height", height), from, to);
    }

    @Override
    public void addCurrency(LedgerEvent event, long eventId, Transaction transaction, Account senderAccount,
                            MonetarySystemCurrencyIssuance attachment) {
        Currency oldCurrency;
        if ((oldCurrency = this.getCurrencyByCode(attachment.getCode())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        if ((oldCurrency = this.getCurrencyByCode(attachment.getName())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        if ((oldCurrency = this.getCurrencyByName(attachment.getName())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        if ((oldCurrency = this.getCurrencyByName(attachment.getCode())) != null) {
            this.delete(oldCurrency, event, eventId, senderAccount);
        }
        int height = blockChainInfoService.getHeight();
        Currency currency = new Currency(transaction, attachment, height);
        currencyTable.insert(currency);
        if (currency.is(CurrencyType.MINTABLE) || currency.is(CurrencyType.RESERVABLE)) {
            CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
            if (currencySupply != null) {
                currencySupply.setCurrentSupply( attachment.getInitialSupply() );
                currencySupply.setHeight(height);
                currencySupplyTable.insert(currencySupply);
            }
        }
    }

    @Override
    public void increaseReserve(LedgerEvent event, long eventId, Account account, long currencyId, long amountPerUnitATM) {
        Currency currency = this.getCurrency(currencyId);
        accountService.addToBalanceATM(account, event, eventId, -Math.multiplyExact(currency.getReserveSupply(), amountPerUnitATM));
        CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
        if (currencySupply != null) {
            long tempAmountPerUnitATM = currencySupply.getCurrentReservePerUnitATM() + amountPerUnitATM;
            currencySupply.setCurrentReservePerUnitATM(tempAmountPerUnitATM);
            currencySupply.setHeight(blockChainInfoService.getHeight());
            currencySupplyTable.insert(currencySupply);
        }
        currencyFounderService.addOrUpdateFounder(currencyId, account.getId(), amountPerUnitATM);
    }

    @Override
    public void claimReserve(LedgerEvent event, long eventId, Account account, long currencyId, long units) {
        accountCurrencyService.addToCurrencyUnits(account, event, eventId, currencyId, -units);
        Currency currency = this.getCurrency(currencyId);
        this.increaseSupply(currency, -units);
        accountService.addToBalanceAndUnconfirmedBalanceATM(account, event, eventId,
            Math.multiplyExact(units, this.getCurrentReservePerUnitATM(currency)));
    }

    @Override
    public void transferCurrency(LedgerEvent event, long eventId, Account senderAccount, Account recipientAccount,
                                 long currencyId, long units) {
        accountCurrencyService.addToCurrencyUnits(senderAccount, event, eventId, currencyId, -units);
        accountCurrencyService.addToCurrencyAndUnconfirmedCurrencyUnits(recipientAccount, event, eventId, currencyId, units);
    }

    public long getCurrentSupply(Currency currency) {
        if (!currency.is(CurrencyType.RESERVABLE) && !currency.is(CurrencyType.MINTABLE)) {
            return currency.getInitialSupply();
        }
        CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
        if (currencySupply == null) {
            return 0;
        }
        return currencySupply.getCurrentSupply();
    }

    @Override
    public long getCurrentReservePerUnitATM(Currency currency) {
        if (!currency.is(CurrencyType.RESERVABLE) || this.loadCurrencySupplyByCurrency(currency) == null) {
            return 0;
        }
        return currency.getCurrencySupply().getCurrentReservePerUnitATM();
    }

    @Override
    public boolean isActive(Currency currency) {
        return currency.getIssuanceHeight() <= blockChainInfoService.getHeight();
    }

    @Override
    public CurrencySupply loadCurrencySupplyByCurrency(Currency currency) {
        if (!currency.is(CurrencyType.RESERVABLE) && !currency.is(CurrencyType.MINTABLE)) {
            return null;
        }
        CurrencySupply currencySupply = currency.getCurrencySupply();
        if (currencySupply == null) {
            currencySupply = currencySupplyTable.get(currencyTable.getDbKeyFactory().newKey(currency));
            if (currencySupply == null) {
                currencySupply = new CurrencySupply(currency, blockChainInfoService.getHeight());
            }
            currency.setCurrencySupply(currencySupply);
        }
        return currencySupply;
    }

    @Override
    public void increaseSupply(Currency currency, long units) {
        CurrencySupply currencySupply = this.loadCurrencySupplyByCurrency(currency);
        long tempCurrentSupply = currencySupply.getCurrentSupply() + units;
        currencySupply.setCurrentSupply(tempCurrentSupply);
        if (currencySupply.getCurrentSupply() > currency.getMaxSupply() || currencySupply.getCurrentSupply() < 0) {
            tempCurrentSupply = currencySupply.getCurrentSupply() - units;
            currencySupply.setCurrentSupply(tempCurrentSupply);
            throw new IllegalArgumentException("Cannot add " + units + " to current supply of " + currencySupply.getCurrentSupply());
        }
        currencySupply.setHeight(blockChainInfoService.getHeight());
        currencySupplyTable.insert(currencySupply);
    }

    @Override
    public DbIterator<Exchange> getExchanges(long currencyId, int from, int to) {
        return exchangeService.getCurrencyExchanges(currencyId, from, to);
    }

    @Override
    public DbIterator<CurrencyTransfer> getTransfers(long currencyId, int from, int to) {
        return currencyTransferService.getCurrencyTransfers(currencyId, from, to);
    }

    @Override
    public boolean canBeDeletedBy(Currency currency, long senderAccountId) {
        if (currency == null) return false; // prevent NPE

        if (!currency.is(CurrencyType.NON_SHUFFLEABLE)
            && shufflingService.getHoldingShufflingCount(currency.getId(), false) > 0) {
            return false;
        }
        if (!this.isActive(currency)) {
            return senderAccountId == currency.getAccountId();
        }
        if (currency.is(CurrencyType.MINTABLE)
            && this.getCurrentSupply(currency) < currency.getMaxSupply()
            && senderAccountId != currency.getAccountId()) {
            return false;
        }

        List<AccountCurrency> accountCurrencies = accountCurrencyService
            .getCurrenciesByAccount(currency.getId(), 0, -1);
        return accountCurrencies.isEmpty() || accountCurrencies.size() == 1 && accountCurrencies.get(0).getAccountId() == senderAccountId;
    }

    @Override
    public void delete(Currency currency, LedgerEvent event, long eventId, Account senderAccount) {
        if (!canBeDeletedBy(currency, senderAccount.getId())) {
            // shouldn't happen as ownership has already been checked in validate, but as a safety check
            throw new IllegalStateException("Currency " + currency.getId() + " not entirely owned by "
                + senderAccount.getId());
        }
        if (currency.is(CurrencyType.RESERVABLE)) {
            if (currency.is(CurrencyType.CLAIMABLE) && this.isActive(currency)) {
                accountCurrencyService.addToUnconfirmedCurrencyUnits(senderAccount, event, eventId, currency.getId(),
                    -accountCurrencyService.getCurrencyUnits(senderAccount, currency.getId()));
                this.claimReserve(event, eventId, senderAccount, currency.getId(),
                    accountCurrencyService.getCurrencyUnits(senderAccount, currency.getId()));
            }
            if (!isActive(currency)) {
                try (DbIterator<CurrencyFounder> founders = currencyFounderService
                    .getCurrencyFounders(currency.getId(), 0, Integer.MAX_VALUE)) {
                    for (CurrencyFounder founder : founders) {
                        accountService.addToBalanceAndUnconfirmedBalanceATM(
                            accountService.getAccount(founder.getAccountId()),
                            event, eventId, Math.multiplyExact(currency.getReserveSupply(), founder.getAmountPerUnitATM()));
                    }
                }
            }
            currencyFounderService.remove(currency.getId());
        }
        if (currency.is(CurrencyType.EXCHANGEABLE)) {
            List<CurrencyBuyOffer> buyOffers = new ArrayList<>();
            try (DbIterator<CurrencyBuyOffer> offers =
                     currencyExchangeOfferFacade.getCurrencyBuyOfferService().getOffers(currency, 0, -1)) {
                while (offers.hasNext()) {
                    buyOffers.add(offers.next());
                }
            }
            int height = blockChainInfoService.getHeight();
            buyOffers.forEach((offer) -> {
                currencyExchangeOfferFacade.removeOffer(event, offer);
            });
        }
        if (currency.is(CurrencyType.MINTABLE)) {
            // lazy init to break up circular dependency
            lookupCurrencyMintService().deleteCurrency(currency);
        }
        accountCurrencyService.addToUnconfirmedCurrencyUnits(
            senderAccount, event, eventId, currency.getId(),
            -accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, currency.getId()));
        accountCurrencyService.addToCurrencyUnits(
            senderAccount, event, eventId, currency.getId(),
            -accountCurrencyService.getCurrencyUnits(senderAccount, currency.getId()));
        int height = blockChainInfoService.getHeight();
        currency.setHeight(height);
        currencyTable.deleteAtHeight(currency, height);
    }

    private CurrencyMintService lookupCurrencyMintService() {
        if (this.currencyMintService == null) {
            this.currencyMintService = CDI.current().select(CurrencyMintService.class).get();
        }
        return this.currencyMintService;
    }

}