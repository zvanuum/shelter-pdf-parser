package org.maricopa.shelter;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class ShelterRescue {
    public static void main(String[] args){
        String inputFile, outputFile;
        PDDocument document;
        PDFParser parser;

        if (args.length < 2) {
            System.out.println("Usage:   java -jar shelter-pdf-parser.jar <input PDF file> <output text file>");
            return;
        }

        inputFile = args[0];
        outputFile = args[1];

        String result = convertPDFtoText(inputFile);

        if (inputFile.contains("WEST")) {
            parseWest(result, outputFile);
        } else if (inputFile.contains("EAST")) {
            //parseEast(result, outputFile);
        }
        //inputFile = "./pdfdata/WEST 1 euthpre 06-06-2016.pdf";

        parseWest(result, outputFile);
    }

    /**
     *
     * @param inputFileName the PDF file with shelter information to be parsed
     */
    private static String convertPDFtoText(String inputFileName) {
        File input;
        PDFTextStripper pdfStripper = null;
        PDDocument pdDoc = null;
        String parsedPDF = "";

        // Open PDF file to read from
        // make inputFileName a cmd line argument or something later
        input = new File(inputFileName);
        if (!input.exists()) {
            System.err.println("Input PDF \"" + inputFileName + "\" does not exist, quitting the parser.");
            System.exit(-1);
        }

        try {
            // make outputFileName a cmd line argument or something later
            pdfStripper = new PDFTextStripper();
            pdDoc = PDDocument.load(input);
            parsedPDF = pdfStripper.getText(pdDoc);
        } catch (Exception e) {
            System.err.println("Error occurred while initializing and parsing the PDF at \"" + inputFileName + "\".");
            e.printStackTrace();
        }

        // File & stream cleanup
        try {
            if (pdDoc != null) pdDoc.close();
        } catch (Exception e) {
            System.err.println("Error occurred while closing files.");
            e.printStackTrace();
        }

        return parsedPDF;
    }

    /**
     * Parse through the converted PDF for the WEST shelters and fill out Dog objects to generate the desired output
     *
     * @param text Dog shelter text to be parsed
     * @param outputFilePath the file to write to
     */
    private static void parseWest(String text, String outputFilePath) {
        if (text == null || text.isEmpty())
            return;

        BufferedWriter writer;
        File outFile = new File(outputFilePath);

        try {
            if (!outFile.exists())
                outFile.createNewFile();

            writer = new BufferedWriter(new FileWriter(outFile));
        } catch (Exception e) {
            System.out.println("Could not initialize output file at " + outputFilePath);
            return;
        }

        String [] colors = { "BRINDLE", "WHITE", "BLACK", "SABLE", "BROWN",
                "CREAM", "TAN", "RED", "CHOCOLATE", "TRICOLOR", "BLUE",
                "GOLD", "SEAL PT", "TORTIE", "CALICO" };
        Pattern kennelPattern = Pattern.compile("^[EW][A-Z]\\d+");
        Pattern animalIDPattern = Pattern.compile("A\\d+");
        Pattern yearMonthPattern = Pattern.compile("\\d\\s\\s\\d\\d?\\.\\d\\dYR/ MO$");
        Matcher matcher;

        //String[] dogs = text.split("KENNEL ANIMAL ID SEXYR/MO  BREED");
        String[] dogs = text.split("\\r\\n");
        Dog dog = new Dog();
        String line, yrMo;
        int years;
        float months;
        boolean multiDogPage = false;
        for (int i = 0; i < dogs.length; i++) {
            line = dogs[i];

            // Find kennel ID and following animal information
            matcher = kennelPattern.matcher(line);
            if (matcher.find()) {
                if (!dogs[i-2].contains("Verify"))
                    multiDogPage = true;

                dog.setKennel(matcher.group(0));
                line = line.substring(dog.getKennel().length() + 1); // remove kennel ID and trailing space

                // Find animal ID
                matcher = animalIDPattern.matcher(line);
                matcher.find();
                dog.setAnimalID(matcher.group(0));
                line = line.substring(dog.getAnimalID().length() + 1); // remove animal ID and trailing space

                // Find year and month, will be backwards because of the PDF reader
                matcher = yearMonthPattern.matcher(line);
                matcher.find();
                // ex: 0  10.00YR/ MO
                yrMo = matcher.group(0);
                years = Integer.parseInt(yrMo.substring(0, 1));
                months = Float.parseFloat(yrMo.substring(3, yrMo.indexOf("YR")));
                dog.setYears(years);
                dog.setMonths(months);


                // remove age info from string once processed
                line = line.substring(0, line.length() - yrMo.length());

                // Mark dog as adoptable when appropriate
                if (line.contains("ADOPTABLE")) {
                    dog.setAdoptable(true);
                    // remove ADOPTABLE from string so that the rest is easier to parse
                    line = line.substring(0, line.indexOf("ADOPTABLE"));
                }

                // Get and remove non-adoptable reason
                if (line.contains("BROWN HOLD")) {
                    // remove BROWN HOLD
                    line = line.substring(0, line.indexOf("BROWN HOLD"));
                } else if (line.contains("HOLDNOTIFY")) {
                    // remove HODLNOTIFY
                    line = line.substring(0, line.indexOf("HOLDNOTIFY"));
                } else if (line.contains("STRAY WAIT")) {
                    // remove STRAY WAIT
                    line = line.substring(0, line.indexOf("STRAY WAIT"));
                }

                // Sex should be character after the breed
                dog.setSex(String.valueOf(line.charAt(line.length() - 1)));
                // Remove the sex from the string
                line = line.substring(0, line.length() - 1);

                // Extract color from the info
                for (String color : colors) {
                    int colorIndex = line.indexOf(color);
                    if (colorIndex > -1) {
                        if (colorIndex == 0) {
                            dog.setColor1(line.substring(0, color.length() + 1));
                        } else {
                            dog.setColor2(line.substring(colorIndex, colorIndex +  color.length() + 1));
                        }
                    }
                }

                // Remove color info
                line = line.substring(dog.getColor1().length() + 4 + dog.getColor2().length());

                // Remaining general info should be the dog breed
                dog.setBreed(line);
            }


            if (dogs[i].contains("STRAY  ") || dogs[i].contains("OWNER SUR")) {
                String intake, intakeDate, intakeStatus;
                int intakeIndex;

                // Line before STRAY/OWNER SUR will be name if the animal has one, else it will be STRAY/OWNER SUR
                // If there is a name, then line i is STRAY/OWNER SUR
                if (dogs[i].contains("STRAY  ")) {
                    if (!dogs[i - 1].contains(dog.getKennel())) {
                        dog.setName(dogs[i - 1]);
                    }

                    dog.setStray(true);
                } else if (dogs[i].contains("OWNER SUR")) {
                    if (!dogs[i - 1].contains(dog.getKennel())) {
                        dog.setName(dogs[i - 1]);
                    }

                    dog.setOwnerSur(true);
                    dog.setOwnerSurReason(dogs[i]);
                } else {
                    if (dogs[i].contains("STRAY  ")) {
                        dog.setStray(true);
                    } else if (dogs[i].contains("OWNER SUR")) {
                        dog.setOwnerSur(true);
                        dog.setOwnerSurReason(dogs[i]);
                    }
                }


                if (dog.getName().isEmpty() && !multiDogPage) {
                    intake = dogs[i];
                    intakeIndex = i;
                } else {
                    intake = dogs[i+1];
                    intakeIndex = i+1;
                }

                int ind = intake.indexOf("Intake Date: ");
                int len = "Intake Date: ".length();
                int ind2 = intake.indexOf("Intake Status:");
                intakeDate = intake.substring(intake.indexOf("Intake Date: ") + "Intake Date: ".length(), intake.indexOf("Intake Status:"));
                dog.setIntakeDate(intakeDate);

                // Due out info should be directly after intake info
                String outStatus = dogs[intakeIndex + 1];
                dog.setDueOutStatus(outStatus.substring(outStatus.indexOf("Due Out Status: ") + "Due Out Status: ".length()));

                System.out.println("test");

                try {
                    // This animal's info should be complete, output it to file and reset the object
                    writer.write(dog.toString() + "\r\n");
                    writer.flush();
                    dog = new Dog();
                    multiDogPage = false;
                } catch (Exception e) {
                    System.out.println("Could not write to output file at " + outputFilePath);
                }
            }
        }

        try {
            writer.close();
        } catch (Exception e) {
            System.out.println("Could not close output file at " + outputFilePath);
        }
    }

}
