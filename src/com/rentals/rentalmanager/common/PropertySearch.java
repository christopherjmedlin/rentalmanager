package com.rentals.rentalmanager.common;

import java.io.Serializable;

/**
 * Just a grouping of several search parameters, to be sent from client to server
 */
public class PropertySearch implements Serializable {
    public String search;
    public int rentalStatus;
    public boolean hasTenants;

    public PropertySearch(String search, int rentalStatus, boolean hasTenants) {
        this.search = search;
        this.rentalStatus = rentalStatus;
        this.hasTenants = hasTenants;
    }
}
