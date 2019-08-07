package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.util.Constants;

public enum ExchangeContractStatus {
    STEP_1,
    STEP_2;

    public static ExchangeContractStatus getType(int ordinal){
        if(ordinal < 0 || ordinal > OfferType.values().length){
            return null;
        }
        return ExchangeContractStatus.values()[ordinal];
    }

    public boolean isStep1(){
        return this.ordinal() == ExchangeContractStatus.STEP_1.ordinal();
    }

    public boolean isStep2(){
        return this.ordinal() == ExchangeContractStatus.STEP_2.ordinal();
    }


    public Integer timeOfWaiting(){
        return this.isStep1() ? Constants.DEX_TIME_OF_WAITING_TX_WITH_APPROVAL_STEP_1 : Constants.DEX_TIME_OF_WAITING_TX_WITH_APPROVAL_STEP_2;
    }
}