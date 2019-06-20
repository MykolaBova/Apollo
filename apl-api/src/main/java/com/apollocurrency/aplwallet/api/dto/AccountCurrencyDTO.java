/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author <andrew.zinchenko@gmail.com>
 */

@Getter @Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountCurrencyDTO {
    private String account;
    private String accountRS;

    private String currency;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long units;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long unconfirmedUnits;

    private String name;
    private String code;
    private Integer type;
    private Byte decimals;
    private Integer issuanceHeight;
    private String issuerAccount;
    private String issuerAccountRS;
}
