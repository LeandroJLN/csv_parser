package com.test.csv;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Hello world!
 */
public final class App {

    private static final int MAX_ROWS = 200; // Max rows per split file (excluding header)
    private static String resPath = "src\\main\\java\\com\\test\\csv\\Resource\\";
    private static String outputFilePrefix = resPath + "split_";
    private static List<String> generatedFiles;

    public static void main(String[] args) throws Exception {

        String inputFilePath = resPath + "sampleCSV.csv"; // Change if needed
        String[] acceptedCountries = { "United States", "Canada", "United Kingdom" };
        String[] jobTitleKeywords = { "ceo", "founder", "chief executive officer", "owner", "co-founder" };

        // First split into multiple files
        generatedFiles = splitCSVFile(inputFilePath, outputFilePrefix, MAX_ROWS);

        // Then remove rows with "Computer Industry" in the specified column
        CSVFilter.filterCSVFiles(generatedFiles, acceptedCountries, jobTitleKeywords);

        // Finally, merge the filtered files into one
        String mergedOutputPath = resPath + "merged_output.csv";
        mergeCSVFiles(generatedFiles, mergedOutputPath);

        String finalOutputPath = resPath + "FinalOutput.csv";
        createLocationCountryColumn(mergedOutputPath, finalOutputPath);
    }

    // This will split sample file into multiple files
    public static List<String> splitCSVFile(String inputFilePath, String outputFilePrefix, int maxRowsPerFile)
            throws Exception {
        List<String> createdFilePaths = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(inputFilePath))) {
            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                System.out.println("The input CSV is empty.");
                return createdFilePaths;
            }

            String[] header = allRows.get(0);
            int fileCount = 1;

            for (int i = 1; i < allRows.size(); i += maxRowsPerFile) {
                String fullOutputPath = outputFilePrefix + fileCount + ".csv";

                try (CSVWriter writer = new CSVWriter(new FileWriter(fullOutputPath))) {
                    writer.writeNext(header);

                    for (int j = i; j < i + maxRowsPerFile && j < allRows.size(); j++) {
                        writer.writeNext(allRows.get(j));
                    }

                    createdFilePaths.add(fullOutputPath);
                    System.out.println("Created: " + fullOutputPath);
                    fileCount++;
                }
            }

            System.out.println("Split complete! Total files: " + (fileCount - 1));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return createdFilePaths;
    }

    public static void mergeCSVFiles(List<String> csvFilePaths, String mergedOutputPath) throws Exception {
        if (csvFilePaths == null || csvFilePaths.isEmpty()) {
            System.out.println("No files to merge.");
            return;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(mergedOutputPath))) {
            boolean isFirstFile = true;

            for (String filePath : csvFilePaths) {
                try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
                    List<String[]> rows = reader.readAll();

                    if (rows.isEmpty()) {
                        continue;
                    }

                    if (isFirstFile) {
                        writer.writeAll(rows); // write header + data
                        isFirstFile = false;
                    } else {
                        writer.writeAll(rows.subList(1, rows.size())); // skip header
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file: " + filePath);
                    e.printStackTrace();
                }
            }

            System.out.println("Merged file created: " + mergedOutputPath);
        } catch (IOException e) {
            System.err.println("Error writing merged file: " + mergedOutputPath);
            e.printStackTrace();
        }
    }

    public static void createLocationCountryColumn(String inputFilePath, String outputFilePath) {
        try (CSVReader reader = new CSVReader(new FileReader(inputFilePath))) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                System.out.println("File is empty: " + inputFilePath);
                return;
            }

            String[] header = allRows.get(0);

            int locationIndex = getColumnIndex(header, "location");
            int countryIndex = getColumnIndex(header, "country");

            if (locationIndex == -1 || countryIndex == -1) {
                System.out.println("Required columns not found in: " + inputFilePath);
                return;
            }

            // Create new header with location_country column after location
            List<String[]> updatedRows = new ArrayList<>();
            String[] newHeader = new String[header.length + 1];
            for (int i = 0, j = 0; i < header.length; i++, j++) {
                newHeader[j] = header[i];
                if (i == locationIndex) {
                    newHeader[++j] = "location_country";
                }
            }
            updatedRows.add(newHeader);

            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                String location = row[locationIndex];
                String country = row[countryIndex];
                String locationCountry = "";

                if (location != null && country != null &&
                        location.toLowerCase().contains(country.toLowerCase())) {

                    locationCountry = country;

                    // Remove the country substring from location
                    location = location.replaceAll("(?i)\\b" + Pattern.quote(country) + "\\b", "")
                            .replaceAll(",\\s*,", ",").trim();
                    // Clean trailing commas/spaces
                    location = location.replaceAll(",\\s*$", "").replaceAll("^\\s*,", "").trim();
                }

                // Create new row with modified location and inserted location_country
                String[] newRow = new String[row.length + 1];
                for (int r = 0, w = 0; r < row.length; r++, w++) {
                    if (r == locationIndex) {
                        newRow[w] = location; // modified location
                        newRow[++w] = locationCountry; // inserted country
                    } else {
                        newRow[w] = row[r];
                    }
                }

                updatedRows.add(newRow);
            }

            try (CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath))) {
                writer.writeAll(updatedRows);
            }

            System.out.println("File written to: " + outputFilePath);

        } catch (Exception e) {
            System.err.println("Error processing file: " + inputFilePath);
            e.printStackTrace();
        }
    }

    private static int getColumnIndex(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (columnName.equalsIgnoreCase(header[i].trim())) {
                return i;
            }
        }
        return -1;
    }

}
