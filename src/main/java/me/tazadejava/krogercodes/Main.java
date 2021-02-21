package me.tazadejava.krogercodes;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    private static Set<String> allIds = new HashSet<>();

    public static void main(String[] args) {
        Gson gson = new Gson();
        HashMap<String, List<String>> zipCodeLocations = new HashMap<>();

        //load the zip codes: ZipCode, List<store IDS obtained by this zip code>

        try {
            for(File zipFile : new File("krogerZipData/").listFiles()) {
                String zip = zipFile.getName().substring(0, zipFile.getName().lastIndexOf("."));

                FileReader reader = new FileReader(zipFile);
                JsonArray data = gson.fromJson(reader, JsonArray.class);
                reader.close();

                List<String> ids = new ArrayList<>();

                for(JsonElement location : data) {
                    JsonObject locationData = location.getAsJsonObject();
                    ids.add(locationData.get("loc_no").getAsString());
                }

                zipCodeLocations.put(zip, ids);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //create a set of all possible store ids based on the zip code data

        allIds = new HashSet<>();

        for(List<String> locs : zipCodeLocations.values()) {
            allIds.addAll(locs);
        }

        System.out.println("TOTAL ZIP CODES: " + zipCodeLocations.keySet().size());
        System.out.println("TOTAL LOCS: " + allIds.size());

        //run the set cover approximation algorithm 100,000 times to find the optimal configuration

        List<String> optimalZipCodes = null;

        for(int i = 0; i < 100000; i++) {
            List<String> allZipCodes = new ArrayList<>(zipCodeLocations.keySet());
            Collections.shuffle(allZipCodes);
            List<String> optimalZipCodesLoop = approximateZipCodes(allZipCodes, zipCodeLocations);

            if(optimalZipCodes == null || optimalZipCodesLoop.size() < optimalZipCodes.size()) {
                optimalZipCodes = optimalZipCodesLoop;
            }

            if(i % 10000 == 0) {
                System.out.println("SMALLEST SO FAR: " + optimalZipCodes.size());
                System.out.println(optimalZipCodes.toString());

                //verify that it works

                Set<String> ids = new HashSet<>(allIds);

                for(String zipCode : optimalZipCodes) {
                    for(String zipCodeId : zipCodeLocations.get(zipCode)) {
                        ids.remove(zipCodeId);
                    }
                }

                System.out.println("VERIFIED? " + (ids.isEmpty()));
            }
        }

        for(String zipCode : optimalZipCodes) {
            System.out.println(zipCode);
        }
        System.out.println("(approx) OPTIMAL ZIP CODES: " + optimalZipCodes.size());
    }

    //greedy approximation set cover algorithm: finds the minimum number of zip codes needed to cover all store IDs
    private static List<String> approximateZipCodes(List<String> allZipCodes, HashMap<String, List<String>> allZipCodeLocations) {
        List<String> remainingZipCodes = new ArrayList<>(allZipCodes);
        List<String> optimalZipCodes = new ArrayList<>();

        Set<String> uncoveredIds = new HashSet<>(allIds);
        while(!uncoveredIds.isEmpty()) {
            //greedy algorithm, find the highest ids covered in the zip code set
            int maxIdsCovered = 0;
            Set<String> maxSetIdsCovered = null;
            String maxZipCode = null;
            for(String zipCode : remainingZipCodes) {
                int idsCovered = 0;
                Set<String> setIdsCovered = new HashSet<>();
                for(String id : allZipCodeLocations.get(zipCode)) {
                    if(uncoveredIds.contains(id)) {
                        setIdsCovered.add(id);
                        idsCovered++;
                    }
                }

                if(idsCovered > maxIdsCovered) {
                    maxZipCode = zipCode;
                    maxIdsCovered = idsCovered;
                    maxSetIdsCovered = setIdsCovered;
                }
            }

            //add zip code to set
            uncoveredIds.removeAll(maxSetIdsCovered);
            optimalZipCodes.add(maxZipCode);
        }

        return optimalZipCodes;
    }
}
