package com.kgas.eveesi;

import com.kgas.eveesi.industry.IndustryCycle;
import static java.lang.System.exit;
import net.troja.eve.esi.ApiClient;
import net.troja.eve.esi.ApiException;
import net.troja.eve.esi.auth.OAuth;

import static com.kgas.eveesi.industry.utils.Shorthands.*;
import static com.kgas.eveesi.industry.utils.Constants.*;
import java.util.List;

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
        
        List<IndustryCycle> cycles = null;
        
        try{
        
            cycles = IndustryCycle.generateIndyCycleDataFromApi(client);
        
        }catch(ApiException ae){
            
            ae.printStackTrace();
            
        }
        
        for(IndustryCycle ic : cycles){
            
            ic.printStats();
            ol();
            
        }
        
        

    }
    
}
