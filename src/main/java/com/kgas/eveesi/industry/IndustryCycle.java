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
import java.util.LinkedList;
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
    
    /**
     * Default constructor
     */
    public IndustryCycle(){
        
        ID = 0;
        numProducts = SELL_LIMIT;
        numInputs = BUY_LIMIT;
        
        transactionTotals = 0;
        taxTotals = 0;
        jobCostTotals = 0;
        totalCost = 0;
        averageCostPerProduct = 0;
        
        salesTaxAmount = 0;
        totalRevenue = 0;
        averageRevenue = 0;
        averageProfit = 0;
        totalProfit = 0;
        
        startTime = null;
        endTime = null;
        
        duration = 0;
        
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
    
    /**
     * Takes the API access point and formats the data into a 
     * list of industry cycles. Only full cycles are counted
     * 
     * @param client
     *      API access point
     * @return
     *      A list of cycle data
     * @throws ApiException 
     */
    public static List<IndustryCycle> generateIndyCycleDataFromApi(ApiClient client) throws ApiException{
        
        //Pull all wallet data
        List<CorporationWalletTransactionsResponse> walletResponsesTemp = 
                new WalletApi(client).getCorporationsCorporationIdWalletsDivisionTransactions(CORP_ID, 1, DATASOURCE, null, null, null);
        
        //Pull all industry data
        List<CorporationIndustryJobsResponse> indyJobs = getIndyHistory(client);
        
        List<CorporationWalletTransactionsResponse> sellOrders = new LinkedList();
        List<CorporationWalletTransactionsResponse> buyOrders = new LinkedList();
        
        
        /*
        
        Now we have all our data loaded.
        Next steps:
        
        Clean the data. 
            Remove industry jobs we don't care about, only keep completed manufacturing jobs.
            Separate wallet stuff into buy/sell orders (?)
        
        */
        
        //Remove all the indy responses that arent manufacturing
        for(int i = indyJobs.size()-1; i >= 0; i--){
            
            if(indyJobs.get(i).getActivityId() != 1)
                indyJobs.remove(i);
            
        }
        
        //Separate wallet transactions into buy and sell, and ignore items we dont care about
        for(CorporationWalletTransactionsResponse response : walletResponsesTemp){
            
            int itemID = response.getTypeId();
            
            if(response.getIsBuy()){
                
                if(itemID == LOGIC_CIRCUIT_ID || itemID == POWER_CIRCUIT_ID || itemID == ENHANCED_WARD_CONSOLE_ID)
                    buyOrders.add(response);
                
                
            }else{
                
                if(itemID == MEDIUM_CORE_DEFENSE_FIELD_EXTENDER_ID)
                    sellOrders.add(response);
                
                
            }
            
        }
        
        /*
        
        Now, from the master list of wallet responses, find the first 6000 worth 
        of buy orders and add them to the temporary buy list, while deleting them from 
        the master list.
        
        Do the same with 400 worth of sell orders. 
        
        Do the same with 400 worth of indy orders.
        
        Pass all three lists into a function that calculates statistics and returns a 
        IndustryCycle object
        
        */
        
        List<CorporationWalletTransactionsResponse> tempBuy;
        List<CorporationWalletTransactionsResponse> tempSell;
        List<CorporationIndustryJobsResponse> tempIndy;
        
        IndustryCycle cycle;
        ArrayList<IndustryCycle> cycleList = new ArrayList();
        
        while(true){
        
            tempBuy = new ArrayList();
            tempSell = new ArrayList();
            tempIndy = new ArrayList();

            int buyCounter = 0, sellCounter = 0, jobCounter = 0;

            while(buyCounter < BUY_LIMIT && !buyOrders.isEmpty()){

                //Add this buy order and add it to the buy order quantity counter
                tempBuy.add(buyOrders.get(0));
                buyCounter += buyOrders.get(0).getQuantity();

                //Remove it from the original list.
                buyOrders.remove(0);

            }

            //Indicates we're run out of buy orders
            if(tempBuy.isEmpty())
                break;
                

            while(sellCounter < SELL_LIMIT && !sellOrders.isEmpty()){

                tempSell.add(sellOrders.get(0));
                sellCounter += sellOrders.get(0).getQuantity();

                sellOrders.remove(0);

            }

            //Indicates we've run out of sell orders
            if(tempSell.isEmpty())
                break;

            while(jobCounter < SELL_LIMIT && !indyJobs.isEmpty()){

                tempIndy.add(indyJobs.get(0));
                jobCounter += tempIndy.get(0).getRuns();

                indyJobs.remove(0);

            }

            //Indicates we've run out of job info
            if(indyJobs.isEmpty())
                break;
                

            cycle = convertApiDataToIndustryCycle(tempBuy, tempSell, tempIndy);
            cycleList.add(cycle);
            
        }
            
        return cycleList;
        
    }

    /**
     * Takes the pre-selected data for one industry cycle and converts it into a IndustryCycle object
     * 
     * @param buyList
     * @param sellList
     * @param indyList
     * @return 
     */
    private static IndustryCycle convertApiDataToIndustryCycle(List<CorporationWalletTransactionsResponse> buyList, 
            List<CorporationWalletTransactionsResponse> sellList, 
            List<CorporationIndustryJobsResponse> indyList){
        
        IndustryCycle toReturn = new IndustryCycle();
        
        CorporationWalletTransactionsResponse firstBuyOrder = buyList.get(buyList.size()-1);
        CorporationWalletTransactionsResponse lastSellOrder = sellList.get(0);
        
        OffsetDateTime firstBuyOrderDate = firstBuyOrder.getDate();
        OffsetDateTime lastSellOrderDate = lastSellOrder.getDate();
        
        toReturn.ID = firstBuyOrder.getTransactionId();
        
        long durationInSeconds = Math.abs(Duration.between(firstBuyOrderDate.toLocalDateTime(), lastSellOrderDate.toLocalDateTime()).getSeconds());
        
        toReturn.duration = durationInSeconds;
        toReturn.startTime = firstBuyOrderDate;
        toReturn.endTime = lastSellOrderDate;
        
        for(CorporationWalletTransactionsResponse cwtr : buyList){

            toReturn.transactionTotals += (double)cwtr.getQuantity()*cwtr.getUnitPrice();

        }
        
        toReturn.taxTotals += FINAL_BUY_TAX * toReturn.transactionTotals;
        toReturn.totalCost += toReturn.transactionTotals + toReturn.taxTotals;
        
        for(CorporationIndustryJobsResponse cijr : indyList){

            toReturn.jobCostTotals += cijr.getCost();

        }
        
        toReturn.totalCost += toReturn.jobCostTotals;
        toReturn.averageCostPerProduct = toReturn.totalCost/((double)PRODUCTS_PER_CYCLE);
        
        for(CorporationWalletTransactionsResponse cwtr : sellList){

            toReturn.totalRevenue += (double)cwtr.getQuantity() * cwtr.getUnitPrice();
            
        }
        
        toReturn.salesTaxAmount = toReturn.totalRevenue * FINAL_SALES_TAX;
        toReturn.totalRevenue -= toReturn.salesTaxAmount;

        toReturn.averageRevenue = toReturn.totalRevenue/(double)PRODUCTS_PER_CYCLE;

        toReturn.averageProfit = toReturn.averageRevenue-toReturn.averageCostPerProduct;
        toReturn.totalProfit = toReturn.averageProfit*PRODUCTS_PER_CYCLE;
        
        return toReturn;
        
    }
    
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
      * First function I wrote to request api data, 
      * and perform profit calculations on it
      * 
      * @param client
      * @return
      * @throws ApiException
      * @deprecated
      */
    @Deprecated
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
    
}
