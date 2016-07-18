package org.maricopa.shelter;

/**
 * Created by Zach.VanUum on 6/9/2016.
 */
public class Dog {
    private String kennel;
    private String animalID;
    private String name;
    private boolean adoptable;
    private int years;
    private float months;
    private String sex;
    private String color1;
    private String color2;
    private String breed;
    private boolean stray;
    private boolean ownerSur;
    private String ownerSurReason;
    private String intakeDate; // Switch to actual Date object?
    private String dueOutStatus;
    private boolean pull;
    private boolean timeLimit;

    public Dog() {
        kennel = "N/A";
        animalID = "N/A";
        name = "";
        adoptable = false;
        years = 0;
        months = 0f;
        sex = "N/A";
        color1 = "";
        color2 = "";
        breed = "N/A";
        stray = false;
        ownerSur = false;
        ownerSurReason = "";
        intakeDate = "N/A";
        dueOutStatus = "N/A";
        pull = false;
        timeLimit = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdoptable() {
        return adoptable;
    }

    public void setAdoptable(boolean adoptable) {
        this.adoptable = adoptable;
    }

    public String getAnimalID() {
        return animalID;
    }

    public void setAnimalID(String animalID) {
        this.animalID = animalID;
    }

    public int getYears() {
        return years;
    }

    public void setYears(int years) {
        this.years = years;
    }

    public float getMonths() {
        return months;
    }

    public void setMonths(float months) {
        this.months = months;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getColor1() {
        return color1;
    }

    public void setColor1(String color1) {
        this.color1 = color1;
    }

    public String getColor2() {
        return color2;
    }

    public void setColor2(String color2) {
        this.color2 = color2;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public boolean isStray() {
        return stray;
    }

    public void setStray(boolean stray) {
        this.stray = stray;
    }

    public boolean isOwnerSur() {
        return ownerSur;
    }

    public void setOwnerSur(boolean ownerSur) {
        this.ownerSur = ownerSur;
    }

    public String getOwnerSurReason() {
        return ownerSurReason;
    }

    public void setOwnerSurReason(String ownerSurReason) {
        this.ownerSurReason = ownerSurReason;
    }

    public String getIntakeDate() {
        return intakeDate;
    }

    public void setIntakeDate(String intakeDate) {
        this.intakeDate = intakeDate;
    }

    public String getDueOutStatus() {
        return dueOutStatus;
    }

    public void setDueOutStatus(String dueOutStatus) {
        this.dueOutStatus = dueOutStatus;
    }

    public boolean isPull() {
        return pull;
    }

    public void setPull(boolean pull) {
        this.pull = pull;
    }

    public boolean isTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(boolean timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getKennel() {
        return kennel;
    }

    public void setKennel(String kennel) {
        this.kennel = kennel;
    }


    /**
     * ex:
     * TIME LIMIT
     * WC071 / A3763737 “SAVANNAH” ADOPTABLE
     * 0 YR/ 10.00 MO F BLACK / WHITE AM PIT BULL TER/MIX
     * STRAY Intake Date: 5/10/2016 S43 DIS-URI
     *
     * @return The formatted string of this Dog object's information
     */
    public String toString() {
        String ret = pull ? "GUARANTEED PULL\n" : timeLimit ? "TIME LIMIT\n" : "";
        String adoptableStr = adoptable ? " ADOPTABLE" : "";
        String strayStr = stray ? "STRAY " : ownerSur ? ownerSurReason + " " : "";
        String color = color2.isEmpty() ? color1 : color1 + " / " + color2;

        ret += String.format("%s / %s \"%s\"%s\n", kennel, animalID, name, adoptableStr);
        ret += String.format("%d YR/ %.2f MO %s %s %s\n", years, months, sex, color, breed);
        ret += String.format("%sIntake Date: %s %s\n", strayStr, intakeDate, dueOutStatus);

        return ret;
    }
}
