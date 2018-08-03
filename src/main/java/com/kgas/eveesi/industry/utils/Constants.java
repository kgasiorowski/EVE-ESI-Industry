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
package com.kgas.eveesi.industry.utils;

/**
 *
 * @author Kuba
 */
public abstract class Constants {
    
    public static final long MEDIUM_CORE_DEFENSE_FIELD_EXTENDER_ID = 31796;
    public static final long LOGIC_CIRCUIT_ID = 25619;
    public static final long POWER_CIRCUIT_ID = 25617;
    public static final long ENHANCED_WARD_CONSOLE_ID = 25625;
    
    public static final float BROKER_BUY_FEE = 0.003f;
    public static final float FINAL_BUY_TAX = BROKER_BUY_FEE;
    
    public static final float BROKER_SELL_FEE = 0.0233f;
    public static final float SALES_TAX = 0.01f;
    public static final float FINAL_SALES_TAX = BROKER_SELL_FEE + SALES_TAX;
    
    public static final int BUY_LIMIT = 6000;
    public static final int SELL_LIMIT = 400;
    
    public static final int PRODUCTS_PER_CYCLE = SELL_LIMIT;
    
    public static final String DATASOURCE = "tranquility";
    public static final int CORP_ID = 98279731;
    
    public static final String DATA_PATH = "";
    public static final String FILE_NAME = "";
    public static final String FILE_PATH = DATA_PATH + FILE_NAME;
    
    public static final String FILE_EXTENSION = "";
    
    public final static String CLIENT_REFRESH = "8pozcUeUgUhk68TMvmIF5GB3Lj8SOj_gITYGU78j3l7OY36h8N2DD-SeTPzZuqbQ9a6NMYg1-J_WbsvGkYKN-A-BqyKhTbZyM5w7LxyNwgrzI4-_GYi_OqmPkpYkit7xHGG0dMoK4MRwEw-bsYEcSu5dJkC0bajn7J8zEg4NO0ybt0zW1AZHUk0wTuJFc_XAlfiOUfGhROL3U0kQWuYBGAgZE2rbBKLYtlKxzFu-wptsd_2nZymrwdc1lkerJJjXhwnsZCcUexJsctfvOCW_2MtBDV2PRui-JNyMDCqQAwJhLHTh4okG4CC0rPP0KEVXvL-gz6dY7r0Cql71Qoa8TjjGI19pCHepF2cm2iTAo9-OQgcW5GLUU9PJY27-q0_oNqoQBxcV6KRs-z79-T0A_Q2";
    public final static String CLIENT_ID = "ff08b7eeaa464b6b98801e42d86b3509";
    public final static String CLIENT_SECRET = "yy6QlhcqxryVS81uFTnWlLJtw1vQ0YKoHWaRcfWK";
    
}
