package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.TransportInteractionController;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TransportInteractionServiceImpl implements TransportInteractionService {
    
    
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionServiceImpl.class);
    private String wsUrl; 

    TransportInteractionWebSocket transportInteractionWebSocket;
    
    
    @Inject
    TransportInteractionServiceImpl( PropertiesHolder prop ) {
        log.debug("Initializing TransportInteractionServiceImpl");   
        wsUrl = prop.getStringProperty("apl.securetransporturl","ws://localhost:8888/");
                
    }
    

    @Override
    public TransportStatusResponse getTransportStatusResponse() {
        TransportStatusResponse transportStatusResponse =  new TransportStatusResponse();        
        log.debug("getTransportStatusResponse");
        
        // log.debug("isOpen: " + transportInteractionWebSocket.isOpen());
        boolean isOpen = transportInteractionWebSocket.isOpen();
        log.debug("isOpen: " + isOpen );
                
        transportStatusResponse.connected = transportInteractionWebSocket.isOpen(); 
        return transportStatusResponse;
    }
    
    @Override
    public void start() {
       
                
        log.debug("Ingition point: "); 
         
         try {
            // open websocket            
            transportInteractionWebSocket = new TransportInteractionWebSocket(new URI(wsUrl));
            
            Runnable task = () -> {
		// System.out.println("Hello, World!");
                for(;;) {
                    
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        java.util.logging.Logger.getLogger(TransportInteractionServiceImpl.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    transportInteractionWebSocket.tick();
                    
                }
                
            };
            Thread thread = new Thread(task);
            thread.start();

        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
        
    }
    
}