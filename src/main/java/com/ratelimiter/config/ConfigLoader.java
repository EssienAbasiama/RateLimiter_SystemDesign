package com.ratelimiter.config;

import java.io.InputStream;
import java.util.Properties;
public class ConfigLoader {
    public static Properties loadProperties(){
        Properties props=new Properties();
        try(InputStream input=ConfigLoader.class.getClassLoader().getResourceAsStream("application.properties")){
            if(input!=null){
                props.load(input);
            }else{
                System.err.println("application.properties not found in classpath");
            }
        }catch(Exception e){
            System.err.println("Error loading properties, using defaults: " + e.getMessage());
        }
        return props;
        
    }
}
