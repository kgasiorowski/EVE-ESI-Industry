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

import static java.lang.System.out;

/**
 *
 * @author Kuba
 */
public abstract class Shorthands {
    
    //Just a shorthand for sout
    public static void o(Object o){

        out.print(o+"");

    }

    //Anothe shorthand, for soutnl
    public static void ol(Object ... o){

        switch(o.length){

            case 0: 
                o("\n"); 
                break;

            case 1: 
                o(o[0] + "\n"); 
                break;

            default: 
                throw new IllegalArgumentException("This function may take exactly one or zero arguments."); 


        }


    }

    //More shorthands
    public static void pf(String format, Object ... params){

        out.printf(format, params);

    }

}
