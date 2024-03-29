package com.rentals.rentalmanager.common;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class Apartment extends RentalProperty {
    // Apartment can start out with 3, 6, or 12 months before going monthly.
    int start;

    public Apartment(double balance, double price, String id, String description, LocalDate moveIn,
                     boolean endOfMonth) {
        super(balance, price, id, description, moveIn, endOfMonth);
        // for now start will just remain three
        this.start = 3;
    }

    @Override
    protected int dueDatesSinceMoveIn() {
        // Apartment starts with 3/6/12 months, then monthly.
        // initialize to 1 to include the initial payment
        int dueDates = 1;
        LocalDate now = LocalDate.now();

        // if it has been more than the chosen start period (3 months, 6 months, or 12 months)
        if (ChronoUnit.MONTHS.between(getMoveInDate(), now) > this.start) {
            // subtract those months from the date
            now = now.minusMonths(this.start);
            dueDates++;
        } else {
            return dueDates;
        }
        // add the remaining months
        dueDates += ChronoUnit.MONTHS.between(getMoveInDate(), now);

        return dueDates;
    }

    @Override
    public LocalDate nextDueDate() {
        // begin by adding the start period
        LocalDate dueDate = this.getMoveInDate().plusMonths(this.start);
        // continue incrementing months until the dueDate is past the current date
        LocalDate currentDate = LocalDate.now();
        while (currentDate.isAfter(dueDate)) {
            dueDate = dueDate.plusMonths(1);
        }
        return dueDate;
    }
}
