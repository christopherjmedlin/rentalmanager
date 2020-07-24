package com.rentals.rentalmanager.server.db;

import com.rentals.rentalmanager.common.*;
import org.apache.derby.iapi.db.Database;

import javax.xml.crypto.Data;
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

            // retrieves every property in the db
            this.allPropertyIds = this.db.prepareStatement(
                    "SELECT id FROM properties ORDER BY id"
            );

            // retrieves a property based on its id
            this.propertyById = this.db.prepareStatement(
                    "SELECT * FROM properties WHERE id=?"
            );

            // searches based on whether there are tenants AND checks the description against a string
            this.propertiesByVacancyAndString = this.db.prepareStatement(
                    "SELECT * FROM properties " +
                     "WHERE hasTenants=? AND description LIKE ?"
            );

            // updates every field in a property, with the last argument being the id of it
            this.updateProperty = this.db.prepareStatement(
                    "UPDATE properties " +
                    "SET balance=?, price=?, moveIn=?, description=?, hasTenants=?" +
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

    /**
     * Performs a search based on the information in the PropertySearch instance and returns a list of ids satisfying
     * it.
     */
    public List<String> search(PropertySearch s) {
        LOGGER.info("Performing search request.");
        PreparedStatement statement = null;
        List<RentalProperty> properties = new ArrayList<>();

        // attempt to just search for a specific property by ID
        RentalProperty property = getPropertyById(s.search);
        if (property != null) {
            List<String> ids = new ArrayList<>();
            // return a string list containing just the one id of the retrieved property.
            ids.add(property.getId());
            return ids;
        }

        try {
            // set first parameter, hasTenants
            this.propertiesByVacancyAndString.setInt(1, s.hasTenants ? 1 : 0);
            // set second parameter, the description search
            this.propertiesByVacancyAndString.setString(2, s.search);
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        try (ResultSet results = this.propertiesByVacancyAndString.executeQuery()) {
            while (results.next()) {
                properties.add(DatabaseUtilities.getPropertyFromResultSet(results));
            }
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        // gets the ids of those properties satisfying the rental status requirement
        return DatabaseUtilities.filterByRentalStatus(properties, s.rentalStatus);
    }

    /**
     * Ensures the property in the database has columns identical to the fields of the RentalProperty object
     */
    public int updateProperty(RentalProperty property) {
        // TODO eventually, this is supposed to also update every tenant that the object contains.
        LOGGER.info("Updating property with id " + property.getId() + ".");
        try {
            // set parameters of the prepared statement to the values of the RentalProperty object
            updateProperty.setDouble(1, property.getBalance());
            updateProperty.setDouble(2, property.getPrice());
            updateProperty.setDate(3, Date.valueOf(property.getMoveInDate()));
            updateProperty.setString(4, property.getDescription());
            //TODO once the tenant-related code is complete, this should be set to 1 if there are tenants and 0 if not. this must be done!!!
            updateProperty.setInt(5, 0);
            updateProperty.setString(6, property.getId());

            return updateProperty.executeUpdate();
        } catch (SQLException e) {
            LOGGER.severe(e.toString());
        }

        return 0;
    }

    public int newProperty(String id) throws IllegalArgumentException {
        LOGGER.info("Adding new property to database with id " + id + ".");
        try {
            newProperty.setString(1, id);
            newProperty.setDate(2, Date.valueOf(LocalDate.now()));
            return newProperty.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            // throw this so the calling method can send an error to the client
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
