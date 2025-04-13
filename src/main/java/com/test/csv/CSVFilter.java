package com.test.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

public class CSVFilter {
    public static void filterCSVFiles(List<String> csvFilePaths, String[] acceptedCountries,
            String[] acceptedJobTitleKeywords) throws Exception {
        int totalRemovedEmptyFields = 0;
        int totalRemovedComputerIndustry = 0;
        int totalRemovedByLocation = 0;
        int totalRemovedByJobTitle = 0;

        for (String filePath : csvFilePaths) {
            List<String[]> filteredRows = new ArrayList<>();

            try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
                List<String[]> allRows = reader.readAll();

                if (allRows.isEmpty()) {
                    System.out.println("Empty file: " + filePath);
                    continue;
                }

                String[] header = allRows.get(0);
                filteredRows.add(header);

                int industryIndex = getColumnIndex(header, "standardized_industry");
                int locationIndex = getColumnIndex(header, "country");
                int jobTitleIndex = getColumnIndex(header, "normalized_job_title");

                if (industryIndex == -1 || locationIndex == -1 || jobTitleIndex == -1) {
                    System.out.println("Required column not found in: " + filePath);
                    continue;
                }

                int removedEmptyFields = 0;
                int removedComputerIndustry = 0;
                int removedByLocation = 0;
                int removedByJobTitle = 0;

                for (int i = 1; i < allRows.size(); i++) {
                    String[] row = allRows.get(i);

                    if (hasEmptyField(row)) {
                        removedEmptyFields++;
                        continue;
                    }

                    if ("Computer Industry".equalsIgnoreCase(row[industryIndex].trim())) {
                        removedComputerIndustry++;
                        continue;
                    }

                    String country = extractCountry(row[locationIndex]);
                    if (!isAcceptedValue(country, acceptedCountries)) {
                        removedByLocation++;
                        continue;
                    }

                    String jobTitle = row[jobTitleIndex].toLowerCase();
                    if (!containsAnyKeyword(jobTitle, acceptedJobTitleKeywords)) {
                        removedByJobTitle++;
                        continue;
                    }

                    filteredRows.add(row);
                }

                try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
                    writer.writeAll(filteredRows);
                }

                totalRemovedEmptyFields += removedEmptyFields;
                totalRemovedComputerIndustry += removedComputerIndustry;
                totalRemovedByLocation += removedByLocation;
                totalRemovedByJobTitle += removedByJobTitle;

                int originalRowCount = allRows.size() - 1;
                int kept = filteredRows.size() - 1;

                System.out.println("File: " + filePath);
                System.out.println("  Original Rows: " + originalRowCount);
                System.out.println("  Removed - Empty Fields: " + removedEmptyFields);
                System.out.println("  Removed - Computer Industry: " + removedComputerIndustry);
                System.out.println("  Removed - Not in accepted country: " + removedByLocation);
                System.out.println("  Removed - Irrelevant job title: " + removedByJobTitle);
                System.out.println("  Kept: " + kept);
                System.out.println();

            } catch (IOException e) {
                System.err.println("Error processing file: " + filePath);
                e.printStackTrace();
            }
        }

        // Print grand total
        int grandTotalRemoved = totalRemovedEmptyFields + totalRemovedComputerIndustry + totalRemovedByLocation
                + totalRemovedByJobTitle;
        System.out.println("==== Overall Summary ====");
        System.out.println("  Total Removed - Empty Fields: " + totalRemovedEmptyFields);
        System.out.println("  Total Removed - Computer Industry: " + totalRemovedComputerIndustry);
        System.out.println("  Total Removed - Not in accepted country: " + totalRemovedByLocation);
        System.out.println("  Total Removed - Irrelevant job title: " + totalRemovedByJobTitle);
        System.out.println("  Total Rows Removed: " + grandTotalRemoved);
    }

    private static int getColumnIndex(String[] header, String columnName) {
        for (int i = 0; i < header.length; i++) {
            if (columnName.equalsIgnoreCase(header[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasEmptyField(String[] row) {
        for (String cell : row) {
            if (cell == null || cell.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String extractCountry(String location) {
        if (location == null || location.isEmpty())
            return "";
        String[] parts = location.split(",");
        return parts[parts.length - 1].trim().toLowerCase();
    }

    private static boolean isAcceptedValue(String value, String[] acceptedList) {
        for (String accepted : acceptedList) {
            if (value.equalsIgnoreCase(accepted.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAnyKeyword(String value, String[] keywords) {
        for (String keyword : keywords) {
            if (value.contains(keyword.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
