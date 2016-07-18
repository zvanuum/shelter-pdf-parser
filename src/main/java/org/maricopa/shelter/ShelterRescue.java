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
        for (String dogSection : dogs) {
            if (!dogSection.isEmpty()) {
                Dog dog = new Dog();
                String genInfo, yrMo;
                int years;
                float months;

                String[] details = dogSection.split("\r\n");

                genInfo = details[10];

                // Find kennel ID
                matcher = kennelPattern.matcher(genInfo);
                matcher.find();
                dog.setKennel(matcher.group(0));
                genInfo = genInfo.substring(dog.getKennel().length() + 1); // remove kennel ID and trailing space

                // Find animal ID
                matcher = animalIDPattern.matcher(genInfo);
                matcher.find();
                dog.setAnimalID(matcher.group(0));
                genInfo = genInfo.substring(dog.getAnimalID().length() + 1); // remove animal ID and trailing space

                // Find year and month, will be backwards because of the PDF reader
                matcher = yearMonthPattern.matcher(genInfo);
                matcher.find();
                // ex: 0  10.00YR/ MO
                yrMo = matcher.group(0);
                years = Integer.parseInt(yrMo.substring(0, 1));
                months = Float.parseFloat(yrMo.substring(3, yrMo.indexOf("YR")));
                dog.setYears(years);
                dog.setMonths(months);

                // remove age info from string once processed
                genInfo = genInfo.substring(0, genInfo.length() - yrMo.length());

                // Mark dog as adoptable when appropriate
                if (genInfo.contains("ADOPTABLE")) {
                    dog.setAdoptable(true);
                    // remove ADOPTABLE from string so that the rest is easier to parse
                    genInfo = genInfo.substring(0, genInfo.indexOf("ADOPTABLE"));
                }

                // Get and remove non-adoptable reason
                if (genInfo.contains("BROWN HOLD")) {
                    // remove BROWN HOLD
                    genInfo = genInfo.substring(0, genInfo.indexOf("BROWN HOLD"));
                } else if (genInfo.contains("HOLDNOTIFY")) {
                    // remove HODLNOTIFY
                    genInfo = genInfo.substring(0, genInfo.indexOf("HOLDNOTIFY"));
                } else if (genInfo.contains("STRAY WAIT")) {
                    // remove STRAY WAIT
                    genInfo = genInfo.substring(0, genInfo.indexOf("STRAY WAIT"));
                }

                // Sex should be character after the breed
                dog.setSex(String.valueOf(genInfo.charAt(genInfo.length() - 1)));
                // Remove the sex from the string
                genInfo = genInfo.substring(0, genInfo.length() - 1);

                // Extract color from the info
                for (String color : colors) {
                    int colorIndex = genInfo.indexOf(color);
                    if (colorIndex > -1) {
                        if (colorIndex == 0) {
                            dog.setColor1(genInfo.substring(0, color.length() + 1));
                        } else {
                            dog.setColor2(genInfo.substring(colorIndex, colorIndex +  color.length() + 1));
                        }
                    }
                }

                // Remove color info
                genInfo = genInfo.substring(dog.getColor1().length() + 4 + dog.getColor2().length());

                // Remaining general info should be the dog breed
                dog.setBreed(genInfo);

                // Line 11 will be name if the animal has one, else it will be STRAY/OWNER SUR
                // If there is a name, then line 12 is STRAY/OWNER SUR
                if (details[12].contains("STRAY")) {
                    dog.setName(details[11]);
                    dog.setStray(true);
                } else if (details[12].contains("OWNER SUR")) {
                    dog.setName(details[11]);
                    dog.setOwnerSur(true);
                    dog.setOwnerSurReason(details[12]);
                } else {
                    if (details[11].contains("STRAY")) {
                        dog.setStray(true);
                    } else if (details[11].contains("OWNER SUR")) {
                        dog.setOwnerSur(true);
                        dog.setOwnerSurReason(details[11]);
                    }
                }

                String intake, intakeDate, intakeStatus;
                int intakeIndex;
                if (dog.getName().isEmpty()) {
                    intake = details[12];
                    intakeIndex = 12;
                } else {
                    intake = details[13];
                    intakeIndex = 13;
                }

                intakeDate = intake.substring(intake.indexOf("Intake Date: ") + "Intake Date: ".length(), intake.indexOf("Intake Status:"));
                dog.setIntakeDate(intakeDate);

                // Due out info should be directly after intake info
                String outStatus = details[intakeIndex + 1];
                dog.setDueOutStatus(outStatus.substring(outStatus.indexOf("Due Out Status: ") + "Due Out Status: ".length()));

                try {
                    writer.write(dog.toString() + "\r\n");
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
