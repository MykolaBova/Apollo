/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.rest.endpoint;


import com.apollocurrency.aplwallet.apl.core.rest.request.GetBalancesRequest;
import com.apollocurrency.aplwallet.api.response.WithdrawResponse;
import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.rest.service.CustomRequestWrapper;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.DexOfferCancelAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.WalletsBalance;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOffer;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOfferDBRequest;
import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.exchange.service.DexOfferTransactionCreator;
import com.apollocurrency.aplwallet.apl.exchange.service.DexService;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ethereum.util.blockchain.EtherUtil;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.json.simple.JSONStreamAware;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.incorrect;
import static com.apollocurrency.aplwallet.apl.util.Constants.MAX_ORDER_DURATION_SEC;

@Path("/dex")
@OpenAPIDefinition(tags = {@Tag(name = "/dex")}, info = @Info(description = "Operations with exchange."))
@Singleton
public class DexController {

    private DexService service;
    private DexOfferTransactionCreator dexOfferTransactionCreator;
    private EpochTime epochTime;
    private DexEthService dexEthService;
    private Integer DEFAULT_DEADLINE_MIN = 60*2;
    private ObjectMapper mapper = new ObjectMapper();


    @Inject
    public DexController(DexService service, DexOfferTransactionCreator dexOfferTransactionCreator, EpochTime epochTime, DexEthService dexEthService) {
        this.service = service;
        this.dexOfferTransactionCreator = dexOfferTransactionCreator;
        this.epochTime = epochTime;
        this.dexEthService = dexEthService;
    }

    //For DI
    public DexController() {
    }

    @GET
    @Path("/balance")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Balances of cryptocurrency wallets",
            description = "dexGetBalances endpoint returns cryptocurrency wallets' (ETH/PAX) balances")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets balances"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getBalances(@Parameter(description = "Addresses to get balance", required = true) @QueryParam("eth") List<String> ethAddresses
        ) throws NotFoundException {

        for (String ethAddress : ethAddresses) {
            if(!EthUtil.isAddressValid(ethAddress)){
                return Response.status(Response.Status.OK).entity(incorrect("ethAddress", "Address length is not correct.")).build();
            }
        }

        WalletsBalance customerWalletsBalance = service.getBalances(new GetBalancesRequest(ethAddresses));

        try {
            return Response.ok(mapper.writeValueAsString(customerWalletsBalance)).build();
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.OK).entity(JSON.toString(JSONResponses.incorrect("Response processing exception."))).build();
        }
    }

    @GET
    @Path("/history")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get trading history for certain account", description = "get trading history for certain account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallets balances"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getHistory( @NotNull  @QueryParam("account") String account,  @QueryParam("pair") String pair,  @QueryParam("type") String type,@Context SecurityContext securityContext)
            throws NotFoundException {
        return Response.ok(service.getHistory(account,pair,type)).build();
    }

    @POST
    @Path("/offer")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Create order", description = "get trading history for certain account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response createOffer(@Parameter(description = "Type of the offer. (BUY/SELL) 0/1", required = true) @FormParam("offerType") Byte offerType,
                                @Parameter(description = "Offer amount in Gwei (1 Gwei = 0.000000001)", required = true) @FormParam("offerAmount") Long offerAmount,
                                @Parameter(description = "Offered currency. (APL=0, ETH=1, PAX=2)", required = true) @FormParam("offerCurrency") Byte offerCurrency,
                                @Parameter(description = "Paired currency. (APL=0, ETH=1, PAX=2)", required = true) @FormParam("pairCurrency") Byte pairCurrency,
                                @Parameter(description = "Pair rate in Gwei. (1 Gwei = 0.000000001)", required = true) @FormParam("pairRate") Long pairRate,
                                @Parameter(description = "Amount of time for this offer. (seconds)", required = true) @FormParam("amountOfTime") Integer amountOfTime,
                                @Context HttpServletRequest req) throws NotFoundException {
        if (pairRate <= 0 ) {
            return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("pairRate", "Should be more than zero."))).build();
        }
        if (offerAmount <= 0 ) {
            return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("offerAmount", "Should be more than zero."))).build();
        }

        try {
            Math.multiplyExact(pairRate, offerAmount);
        } catch (ArithmeticException ex){
            return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("pairRate or offerAmount", "Are too big."))).build();
        }

        if (amountOfTime <= 0 || amountOfTime > MAX_ORDER_DURATION_SEC) {
            return Response.status(Response.Status.OK).entity(
                    JSON.toString(incorrect("amountOfTime",  String.format("value %d not in range [%d-%d]", amountOfTime, 0, MAX_ORDER_DURATION_SEC)))
            ).build();
        }

        Integer currentTime = epochTime.getEpochTime();
        try {
            DexOffer offer = new DexOffer();
            try {
                offer.setType(OfferType.getType(offerType));
                offer.setOfferAmount(offerAmount);
                offer.setOfferCurrency(DexCurrencies.getType(offerCurrency));
                offer.setPairCurrency(DexCurrencies.getType(pairCurrency));
                offer.setPairRate(pairRate);
                offer.setStatus(OfferStatus.OPEN);
                offer.setFinishTime(currentTime + amountOfTime);
            } catch (Exception ex) {
                return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
            }

            if (offer.getOfferCurrency().equals(offer.getPairCurrency())) {
                return Response.status(Response.Status.OK).entity(JSON.toString(JSONResponses.incorrect("OfferCurrency and PairCurrency are equal."))).build();
            }

            Account account = ParameterParser.getSenderAccount(req);
            //If we should freeze APL
            if (OfferType.SELL.equals(offer.getType()) && DexCurrencies.APL.equals(offer.getOfferCurrency())) {
                Long amountATM = offer.getOfferAmount();
                if (account.getUnconfirmedBalanceATM() < amountATM) {
                    return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
                }
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", "1440");
            DexOfferAttachment dexOfferAttachment = new DexOfferAttachment(offer);
            try {
                JSONStreamAware response = dexOfferTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, dexOfferAttachment);
                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            }
        } catch (ParameterException ex){
            return Response.ok(JSON.toString(ex.getErrorResponse())).build();
        }
    }

    @GET
    @Path("/offers")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Get exchange offers", description = "dexGetOffers endpoint list of opened pending exchange orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Exchange offers"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response getOffers(  @Parameter(description = "Type of the offer. (BUY = 0 /SELL = 1)") @QueryParam("orderType") Byte orderType,
                                @Parameter(description = "Criteria by Offered currency. (APL=0, ETH=1, PAX=2)") @QueryParam("offerCurrency") Byte offerCurrency,
                                @Parameter(description = "Criteria by Paired currency. (APL=0, ETH=1, PAX=2)") @QueryParam("pairCurrency") Byte pairCurrency,
                                @Parameter(description = "Offer status. (Open = 0, Close = 2)") @QueryParam("status") Byte status,
                                @Parameter(description = "User account id.") @QueryParam("accountId") String accountIdStr,
                                @Parameter(description = "Return offers available for now. By default = false") @DefaultValue(value = "false") @QueryParam("isAvailableForNow") boolean isAvailableForNow,
                                @Parameter(description = "Criteria by min prise.") @QueryParam("minAskPrice") BigDecimal minAskPrice,
                                @Parameter(description = "Criteria by max prise.") @QueryParam("maxBidPrice") BigDecimal maxBidPrice,
                                @Context HttpServletRequest req) throws NotFoundException {
        OfferType type = null;
        OfferStatus offerStatus = null;
        DexCurrencies offerCur = null;
        DexCurrencies pairCur = null;
        Integer currentTime = null;
        Long accountId = null;

        //Validate
        try {
            if (orderType != null) {
                type = OfferType.getType(orderType);
            }
            if (offerCurrency != null) {
                offerCur = DexCurrencies.getType(offerCurrency);
            }
            if (pairCurrency != null) {
                pairCur = DexCurrencies.getType(pairCurrency);
            }
            if (isAvailableForNow) {
                currentTime = epochTime.getEpochTime();
            }
            if(!StringUtils.isBlank(accountIdStr)){
                accountId = Long.parseUnsignedLong(accountIdStr);
            }
            if(status != null){
                offerStatus = OfferStatus.getType(status);
            }
        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        int offset = firstIndex > 0 ? firstIndex : 0;
        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);

        DexOfferDBRequest dexOfferDBRequest = new DexOfferDBRequest(type, currentTime, offerCur, pairCur, accountId, offerStatus, minAskPrice, maxBidPrice, offset, limit);
        List<DexOffer> offers = service.getOffers(dexOfferDBRequest);

        return Response.ok(offers.stream()
                .map(o -> o.toDto())
                .collect(Collectors.toList())
        ).build();
    }

    @POST
    @Path("/offer/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Cancel Order By Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response cancelOrderByOrderID(@Parameter(description = "Order id") @FormParam("orderId") String transactionIdStr,
                                         @Context HttpServletRequest req) throws NotFoundException {
        try{
            Long transactionId;
            Account account = ParameterParser.getSenderAccount(req);

            if (StringUtils.isBlank(transactionIdStr)) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("transactionId", "Can't be null."))).build();
            }
            try{
                transactionId = Long.parseUnsignedLong(transactionIdStr);
            } catch (Exception ex){
                return Response.ok(JSON.toString(incorrect("transactionId", "Transaction ID is not correct."))).build();
            }
            DexOffer offer = service.getOfferByTransactionId(transactionId);
            if(offer == null) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "Order was not found."))).build();
            }
            if(!Long.valueOf(offer.getAccountId()).equals(account.getId())){
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("accountId", "Can cancel only your orders."))).build();
            }
            if(!OfferStatus.OPEN.equals(offer.getStatus())) {
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "Can cancel only Open orders."))).build();
            }

            if(service.isThereAnotherCancelUnconfirmedTx(transactionId, null)){
                return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("orderId", "There is another cancel transaction for this order in the unconfirmed tx pool already."))).build();
            }

            CustomRequestWrapper requestWrapper = new CustomRequestWrapper(req);
            requestWrapper.addParameter("deadline", DEFAULT_DEADLINE_MIN.toString());
            DexOfferCancelAttachment dexOfferCancelAttachment = new DexOfferCancelAttachment(transactionId);

            try {
                JSONStreamAware response = dexOfferTransactionCreator.createTransaction(requestWrapper, account, 0L, 0L, dexOfferCancelAttachment);
                return Response.ok(JSON.toString(response)).build();
            } catch (AplException.ValidationException e) {
                return Response.ok(JSON.toString(JSONResponses.NOT_ENOUGH_FUNDS)).build();
            }
        } catch (ParameterException ex){
            return Response.ok(JSON.toString(ex.getErrorResponse())).build();
        }
    }

    @POST
    @Path("/withdraw")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Widthraw cryptocurrency", description = "dexWidthraw endpoint provides transferEth of Ethereum/Pax")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction hash"),
            @ApiResponse(responseCode = "200", description = "Unexpected error") })
    public Response dexWithdrawPost(
                                    @NotNull @Parameter(description = "amount eth for withdraw") @FormParam("amount") BigDecimal amount,
                                    @NotNull @Parameter(description = "Send from address") @FormParam("fromAddress") String fromAddress,
                                    @NotNull @Parameter(description = "Send to address") @FormParam("toAddress") String toAddress,
                                    @NotNull @Parameter(description = "Transfer fee in GWei") @FormParam("transferFee") Long transferFee,
                                    @NotNull @Parameter(description = "crypto currency for withdraw:ETH=1/PAX=2") @FormParam("cryptocurrency") Byte cryptocurrency,
                                    @Context HttpServletRequest req) {
        DexCurrencies currencies = null;
        String passphrase;
        Account sender;
        try{
            passphrase = Convert.emptyToNull(ParameterParser.getPassphrase(req, true));
            sender = ParameterParser.getSenderAccount(req);

            if (cryptocurrency != null) {
                currencies = DexCurrencies.getType(cryptocurrency);
            }
        } catch (Exception ex){
            return Response.ok(JSON.toString(JSONResponses.ERROR_INCORRECT_REQUEST)).build();
        }

        if (currencies == null || DexCurrencies.APL.equals(currencies)) {
            return Response.status(Response.Status.OK).entity(JSON.toString(incorrect("cryptocurrency", "Withdraw can work only with Eth or Pax."))).build();
        }

        if(!EthUtil.isAddressValid(toAddress)){
            return Response.status(Response.Status.OK).entity(incorrect("toAddress", "Address length is not correct.")).build();
        }
        if(!EthUtil.isAddressValid(fromAddress)){
            return Response.status(Response.Status.OK).entity(incorrect("fromAddress", "Address length is not correct.")).build();
        }

        String transaction;
        try {
            transaction = service.withdraw(sender.getId(), passphrase, fromAddress, toAddress, amount, currencies, transferFee);
        } catch (AplException.ExecutiveProcessException e){
            return Response.ok(JSON.toString(JSONResponses.error(e.getMessage()))).build();
        }

        if(StringUtils.isBlank(transaction)){
            return Response.ok(JSON.toString(JSONResponses.error("Transfer didn't send."))).build();
        } else {
            return Response.ok(new WithdrawResponse(transaction)).build();
        }
    }


    @GET
    @Path("/ethInfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(tags = {"dex"}, summary = "Eth gas info", description = "get gas prices for different tx speed.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Eth gas info")})
    public Response dexEthInfo(@Context SecurityContext securityContext) throws NotFoundException {
        EthGasInfo ethGasInfo = dexEthService.getEthPriceInfo();

        if(ethGasInfo != null){
            return Response.ok(ethGasInfo.toDto()).build();
        } else {
            return Response.ok().build();
        }
    }



}
