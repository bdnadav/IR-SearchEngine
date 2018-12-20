package Engine.Model;

import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 */
public class City {
    public String getCity_name() {
        return city_name;
    }

    public String getState_name() {
        return state_name;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPopulation() {
        return population;
    }

    private  String city_name  = "" ;
    private  String state_name = "" ;
    private  String currency = "" ;
    private  String population = "" ;





    public City(String state_name, String currency, String population) {
        this.state_name = state_name;
        this.currency = currency;
        this.population = population;
    }



    public City(String docCity) {
        city_name = docCity ;
    }


    public void setState_name(String state_name) {
        this.state_name = state_name;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setPopulation(String population) {
        this.population = population;
    }

    public String getCityName() {
        return city_name;
    }
}
