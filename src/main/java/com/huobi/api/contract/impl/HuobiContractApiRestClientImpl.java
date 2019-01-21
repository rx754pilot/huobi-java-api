package com.huobi.api.contract.impl;

import com.huobi.api.client.domain.Candle;
import com.huobi.api.client.domain.Merged;
import com.huobi.api.client.domain.Trade;
import com.huobi.api.client.domain.enums.MergeLevel;
import com.huobi.api.client.domain.enums.Resolution;
import com.huobi.api.client.domain.resp.RespTick;
import com.huobi.api.contract.HuobiContractApiRestClient;
import com.huobi.api.contract.HuobiContractApiService;
import com.huobi.api.contract.domain.*;

import java.util.List;

import static com.huobi.api.client.HuobiApiServiceGenerator.createService;
import static com.huobi.api.client.HuobiApiServiceGenerator.executeSync;

/**
 * created by jacky. 2018/11/30 11:43 AM
 */
public class HuobiContractApiRestClientImpl implements HuobiContractApiRestClient {
    private HuobiContractApiService service;

    public HuobiContractApiRestClientImpl(String apiKey, String secretKey) {
        service = createService(HuobiContractApiService.class, apiKey, secretKey);
    }

    @Override
    public List<ContractInfo> info(String symbol, String type, String code) {
        return executeSync(service.info(symbol, type, code)).getData();
    }


    @Override
    public List<ContractIndex> index(String symbol) {
        return executeSync(service.index(symbol)).getData();
    }

    @Override
    public List<ContractPriceLimit> priceLimit(String symbol, String type, String code) {
        return executeSync(service.priceLimit(symbol, type, code)).getData();
    }

    @Override
    public List<Interest> openInterest(String symbol, String type, String code) {
        return executeSync(service.openInterest(symbol, type, code)).getData();
    }


    @Override
    public Delivery deliveryPrice(String symbol) {
        return executeSync(service.deliveryPrice(symbol)).getData();
    }

    @Override
    public Depth marketDepth(String symbol, MergeLevel type) {
        return executeSync(service.marketDepth(symbol, type.getCode())).getTick();
    }


    @Override
    public List<Candle> historyKline(String symbol, Resolution period, Integer size) {
        return executeSync(service.historyKline(symbol, period.getCode(), size)).getData();
    }

    @Override
    public Merged marketDetailMerged(String symbol) {
        return executeSync(service.marketDetailMerged(symbol)).getTick();
    }

    @Override
    public RespTick<Trade> marketTrade(String symbol) {
        return executeSync(service.marketTrade(symbol)).getTick();
    }

    @Override
    public List<RespTick<Trade>> historyTrade(String symbol, Integer size) {
        return executeSync(service.historyTrade(symbol,size)).getData();
    }
    @Override
    public List<ContractAccount> accountInfo(String symbol) {
        return executeSync(service.accountInfo(symbol)).getData();
    }


}