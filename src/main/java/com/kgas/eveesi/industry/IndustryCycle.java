/*
 * Copyright 2018 Kuba.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kgas.eveesi.industry;

import static com.kgas.eveesi.industry.utils.Constants.*;
import static com.kgas.eveesi.industry.utils.Shorthands.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import net.troja.eve.esi.ApiClient;
import net.troja.eve.esi.ApiException;
import net.troja.eve.esi.HeaderUtil;
import net.troja.eve.esi.api.IndustryApi;
import net.troja.eve.esi.api.WalletApi;
import net.troja.eve.esi.model.CorporationIndustryJobsResponse;
import net.troja.eve.esi.model.CorporationWalletTransactionsResponse;

/**
 * This class provides a means to transfer data from the eve online API
 * to the user in an organized manner.
 * 
 * @author Kuba
 */
public class IndustryCycle implements Serializable{
    
    //List of stats we want to hold on to
    private double ID;

    private int numProducts;
    private int numInputs;
    
    private double transactionTotals;
    private double taxTotals;
    private double jobCostTotals;
    private double totalCost;
    private double averageCostPerProduct;
    
    private double salesTaxAmount;
    private double totalRevenue;
    private double averageRevenue;
    private double averageProfit;
    private double totalProfit;
    
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private long duration;
    
    public double getID(){return ID;}
    
    public int getNumProducts(){return numProducts;}
    public int getNumInputs(){return numInputs;}
    
    public double getTransactionTotals(){return transactionTotals;}
    public double getTaxTotals(){return taxTotals;}
    public double getJobCostTotals(){return jobCostTotals;}
    public double getTotalCost(){return totalCost;}
    public double getAverageCostPerProduct(){return averageCostPerProduct;}
    
    public double getSalesTaxAmount(){return salesTaxAmount;}
    public double getTotalRevenue(){return totalRevenue;}
    public double getAverageRevenue(){return averageRevenue;}
    public double getAverageProfit(){return averageProfit;}
    public double getTotalProfit(){return totalProfit;}
    

    public OffsetDateTime getStartTime(){return startTime;}
    public OffsetDateTime getEndTime(){return endTime;}
    public long getDuration(){return duration;}
    
    
    public static IndustryCycle getIndustryCycleStats(ApiClient client) throws ApiException {
    
        WalletApi walletApi = new WalletApi(client);
        IndustryCycle toReturn = new IndustryCycle();
        
        toReturn.numInputs = BUY_LIMIT;
        toReturn.numProducts = SELL_LIMIT;
        
        //ol("Getting market order info...");
        List<CorporationWalletTransactionsResponse> walletTransactionHistory = walletApi.getCorporationsCorporationIdWalletsDivisionTransactions(CORP_ID, 1, null, null, null, null);
        
        //ol("Getting corp indy job history...");
        List<CorporationIndustryJobsResponse> industryJobsHistory = getIndyHistory(client);

        //ol("Organizing data..");

        //From now on, use only these lists when performing calculations.
        List<CorporationWalletTransactionsResponse> buyOrders = new ArrayList<>();
        List<CorporationWalletTransactionsResponse> sellOrders = new ArrayList<>();
        
        //Counts how many components were bought (up to 6k in this case)
        int buyCounter = 0;
        int sellCounter = 0;

        OffsetDateTime firstBuyOrder = OffsetDateTime.MAX, lastSellOrder = OffsetDateTime.MIN;
        
        //Here we filter all the transactions we care about into their respective lists
        //And we figure out the date of the first bought and last sold items
        for(CorporationWalletTransactionsResponse cwtr : walletTransactionHistory){

            //Retrieve the item id being exchanged here
            long typeId = cwtr.getTypeId();
            
            //If the transaction is a buy order...
            if(cwtr.getIsBuy()){

                //If we are over the buy limit, OR if the buy order is for something unrelated, skip it.
                if(buyCounter >= BUY_LIMIT || 
                        typeId != ENHANCED_WARD_CONSOLE_ID && 
                        typeId != LOGIC_CIRCUIT_ID && 
                        typeId != POWER_CIRCUIT_ID)
                    continue;
                
                if(firstBuyOrder.isAfter(cwtr.getDate())){
                    
                    //Assign new oldest buy order
                    firstBuyOrder = cwtr.getDate();
                    
                }
                
                buyOrders.add(cwtr);
                buyCounter += cwtr.getQuantity();

            }else{ //Otherwise, it's a sell order...

                if(sellCounter >= SELL_LIMIT ||
                        typeId != MEDIUM_CORE_DEFENSE_FIELD_EXTENDER_ID)
                    continue;
                
                if(lastSellOrder.isBefore(cwtr.getDate())){
                    
                    //Assign new oldest buy order
                    lastSellOrder = cwtr.getDate();
                    //The unique ID of this cycle is the transaction ID of the last sell order in the cycle.
                    toReturn.ID = cwtr.getTransactionId();
                    
                }
                
                sellOrders.add(cwtr);
                sellCounter += cwtr.getQuantity();

            }    

        }
        
        toReturn.startTime = firstBuyOrder;
        toReturn.endTime = lastSellOrder;
        
        long durationInSeconds = Math.abs(Duration.between(firstBuyOrder.toLocalDateTime(), lastSellOrder.toLocalDateTime()).getSeconds());
        toReturn.duration = durationInSeconds;
        
        //ol("Done.");

        ol("Buy orders: " + buyOrders.size() + "(" + buyCounter + ")");
        ol("Sell orders: " + sellOrders.size() + "(" + sellCounter + ")");
        
        //Tracks the total cost of the production cycle
        double totalCostCounter = 0;

        //Tracks the various amounts of isk that was spent
        double transactionTotals = 0;
        double taxTotals = 0;
        double jobCostTotals = 0;

        for(CorporationWalletTransactionsResponse cwtr : buyOrders){

            transactionTotals += (double)cwtr.getQuantity()*cwtr.getUnitPrice();

        }

        taxTotals += FINAL_BUY_TAX * transactionTotals;
        totalCostCounter += transactionTotals + taxTotals;

        int jobAmountCounter = 0;

        for(CorporationIndustryJobsResponse cijr : industryJobsHistory){

            //For calculating job costs, skip everything except manufacturing jobs
            if(cijr.getActivityId() != 1)
                continue;
            
            //We don't want to count more than 400 total rigs produced
            if(jobAmountCounter >= PRODUCTS_PER_CYCLE)
                break;
            else{

                jobCostTotals += cijr.getCost();
                jobAmountCounter += cijr.getRuns();

            }

        }

        totalCostCounter += jobCostTotals;

        double averageCostPerRun = totalCostCounter/((double)PRODUCTS_PER_CYCLE);

        
        //Here we write all the necessary cost fields to the object
        toReturn.transactionTotals = transactionTotals;
        toReturn.jobCostTotals = jobCostTotals;
        toReturn.taxTotals = taxTotals;
        toReturn.averageCostPerProduct = averageCostPerRun;
        toReturn.totalCost = totalCostCounter;

        double totalRevenue = 0;

        for(CorporationWalletTransactionsResponse cwtr : sellOrders){

            totalRevenue += (double)cwtr.getQuantity() * cwtr.getUnitPrice();
            
        }

        //Calculate total revenue for this round of jobs
        double salesTaxAmount = totalRevenue * FINAL_SALES_TAX;
        totalRevenue -= salesTaxAmount;

        //Calculate the average revenue per product
        double averageRevenue = totalRevenue/PRODUCTS_PER_CYCLE;

        
        //Calculate the average profit and from this the total profit for this run
        double averageProfit = averageRevenue-averageCostPerRun;
        double totalProfitByAverage = averageProfit*PRODUCTS_PER_CYCLE;

        
        toReturn.salesTaxAmount = salesTaxAmount;
        toReturn.totalRevenue = totalRevenue;
        toReturn.averageRevenue = averageRevenue;
        toReturn.averageProfit = averageProfit;
        toReturn.totalProfit = totalProfitByAverage;
        
        return toReturn;
    
    }
    
    /**
     * Fetches all industry activity. Utilizes paging.
     * 
     * @param apiClient
     *      ESI access point.
     * @return
     *      All industry activity fetched from the ESI api.
     * @throws ApiException 
     */
    private static List<CorporationIndustryJobsResponse> getIndyHistory(ApiClient apiClient) throws ApiException{
    
        IndustryApi indyClient = new IndustryApi(apiClient);
        
        List<CorporationIndustryJobsResponse> results = 
                indyClient.getCorporationsCorporationIdIndustryJobs(CORP_ID, DATASOURCE, null, Boolean.TRUE, null, null);
        
        Integer xPages = HeaderUtil.getXPages(apiClient.getResponseHeaders());   
        //ol("Number of pages detected: " + xPages);  
        
        //If theres less than 2 pages, just exit instead of querying for more pages
        if(xPages == null || xPages < 2)
            return results;
        
        for(int page = 2; page <= xPages; page++)
        {
        
            //ol("Page: " + page);
            results.addAll(indyClient.getCorporationsCorporationIdIndustryJobs(CORP_ID, DATASOURCE, null, Boolean.TRUE, page, null));
        
        }
        
        //ol("Total number of results: " + results.size());
        
        return results;
        
    
    }
    
    /**
     * Compares two indy cycles
     * 
     * @param other
     *      The other cycle to compare
     * @return 
     *      True if they are the same cycles, false otherwise
     * 
     */
    public boolean equals(IndustryCycle other){
        
        return this.ID == other.ID;
        
    }
    
    /**
     * Prints the stats for this cycle to the console.
     * Only use this for debugging.
     */
    public void printStats(){
    
        long durationInSeconds = duration;
        
        long numDays = durationInSeconds / (60 * 60 * 24);
        durationInSeconds %= (60 * 60 * 24);
        
        long numHours = durationInSeconds / (60 * 60);
        durationInSeconds %= (60 * 60);
        
        long numMins = durationInSeconds / 60;
        durationInSeconds %= 60;
        
        long numSeconds = durationInSeconds;
        
        pf("Cycle ID: %.0f\n", ID);
        pf("This cycle lasted from %s to %s. (%dd %dh %dmin %ds)\n", startTime.toString().replaceAll("T", "+").replaceAll("Z", ""), endTime.toString().replaceAll("T", "+").replaceAll("Z", ""), numDays, numHours, numMins, numSeconds);
        pf("\n%-25s : %,20.2f\n%-25s : %,20.2f\n%-25s : %,20.2f\n%-25s : %,20.2f\n\n%-25s : %,20.2f\n", "Buy Orders Total", transactionTotals, "Tax Total", taxTotals, "Indy Facility Total Cost", jobCostTotals, "Total Cost", totalCost, "Average Job Cost", averageCostPerProduct);
        pf("\n%-25s : %,20.2f\n%-25s : %,20.2f\n%-25s : %,20.2f\n", "Sales Tax", salesTaxAmount, "Total Revenue", totalRevenue, "Average Revenue", averageRevenue);
        pf("\n%-25s : %,20.2f\n\n%-25s : %,20.2f\n", "Average profit", averageProfit, "Total profit", totalProfit);
        
    }
    
}
