package com.rentals.rentalmanager.server.db;

import com.rentals.rentalmanager.common.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Used for retrieving, updating, and creating RentalProperties in the database
 */
public class PropertyQueries {
    private static final Logger LOGGER = Logger.getLogger(PropertyQueries.class.getName());

    private Connection db;
    private PreparedStatement allPropertyIds;
    private PreparedStatement propertyById;
    private PreparedStatement newProperty;
    private PreparedStatement updateProperty;
    private PreparedStatement propertiesByVacancyAndString;

    public PropertyQueries(String username, String password) {
        try {
            this.db = DriverManager.getConnection(DatabaseUtilities.URL, username, password);

            // returns every property in the db
            this.allPropertyIds = this.db.prepareStatement(
                    "SELECT id FROM properties ORDER BY id"
            );

            this.propertyById = this.db.prepareStatement(
                    "SELECT * FROM properties WHERE id=?"
            );

            // searches based on whether there are tenants AND checks the description against a string
            this.propertiesByVacancyAndString = this.db.prepareStatement(
                    "SELECT id FROM properties " +
                     "WHERE hasTenants=? AND description LIKE ?"
            );

            // updates every field in a property, with the last argument being the id of it
            this.updateProperty = this.db.prepareStatement(
                    "UPDATE properties " +
                    "SET balance=?, price=?, moveIn=?, description=? " +
                    "WHERE id=?"
            );

            // create new property with user-defined id, with everything else empty
            this.newProperty = this.db.prepareStatement(
                    "INSERT INTO properties " +
                       "(id, moveIn) VALUES (?, ?)"
            );
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }
    }

    /**
     * @return a list of every property id in the database
     */
    public List<String> getAllPropertyIds() {
        List<String> properties = new ArrayList<>();

        LOGGER.info("Querying all properties.");
        try (ResultSet results = this.allPropertyIds.executeQuery()) {
            while (results.next()) {
                properties.add(results.getString("id"));
            }
            return properties;
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        return null;
    }

    public RentalProperty getPropertyById(String id) {
        try {
            propertyById.setString(1, id);
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        try (ResultSet results = this.propertyById.executeQuery()) {
            // returns null after try clause if no property is found (if !results.next())
            if (results.next()) return DatabaseUtilities.getPropertyFromResultSet(results);
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }
        return null;
    }

    public List<String> search(PropertySearch s) {
        PreparedStatement statement = null;
        List<String> properties = new ArrayList<>();

        // attempt to just search for a specific property by ID
        RentalProperty property = getPropertyById(s.search);
        if (property != null)
            properties.add(property.getId());

        try {
            this.propertiesByVacancyAndString.setInt(1, s.hasTenants ? 1 : 0);
            this.propertiesByVacancyAndString.setString(2, s.search);
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        try (ResultSet results = this.propertiesByVacancyAndString.executeQuery()) {
            while (results.next()) {
                properties.add(results.getString("id"));
            }
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }
        return properties;
    }

    // TODO the move in date should probably by default be the current date.
    public int newProperty(String id) throws IllegalArgumentException {
        LOGGER.info("Adding new property to database with id " + id + ".");
        try {
            newProperty.setString(1, id);
            newProperty.setDate(2, Date.valueOf(LocalDate.now()));
            return newProperty.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("ID already exists.");
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        return 0;
    }

    public void close() {
        try {
            LOGGER.info("Closing database connection.");
            this.db.close();
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }
    }
}
