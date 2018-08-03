package com.kgas.eveesi;

import com.kgas.eveesi.industry.IndustryCycle;
import static java.lang.System.exit;
import net.troja.eve.esi.ApiClient;
import net.troja.eve.esi.ApiException;
import net.troja.eve.esi.auth.OAuth;

import static com.kgas.eveesi.industry.utils.Shorthands.*;
import static com.kgas.eveesi.industry.utils.Constants.*;

/**
 *
 * @author Kuba
 */
public class Client {
    
    private final static ApiClient client = new ApiClient();
    
    public static void main(String[] args){
        
        final OAuth auth = (OAuth)client.getAuthentication("evesso");
        auth.setClientId(CLIENT_ID);
        auth.setClientSecret(CLIENT_SECRET);
        auth.setRefreshToken(CLIENT_REFRESH);
        
        IndustryCycle cycle = null;
        
        try{
        
            cycle = IndustryCycle.getIndustryCycleStats(client);
        
        }catch(ApiException ae){
            
            System.out.println("There was a problem with the API.");
            System.out.println(ae.getMessage());
            exit(0);
            
        }
        
        cycle.printStats();
        
    }
    
}
