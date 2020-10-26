import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

public class CsvParsers
{
    static Hashtable<Integer, Item> ParseCatalogueCsv(String filePath)
    {
        BufferedReader csvReader;
        var catalogue = new Hashtable<Integer, Item>();

        try {
            csvReader = new BufferedReader(new FileReader(filePath));
            csvReader.readLine(); //reads first line
            String row;
            while ((row = csvReader.readLine()) != null) {

                String[] data = row.split(",");
                //sets current price as starting price initially
                catalogue.put(Integer.parseInt(data[0]) - 1, new Item(data[1], Integer.parseInt(data[2]), Integer.parseInt(data[2])));
            }

            csvReader.close();

        } catch (
                IOException e) {
            e.printStackTrace();

        }
        return catalogue;
    }

    static Hashtable<String, Integer> ParseShoppingListCsv(String filePath)
    {
        BufferedReader csvReader;

        var shoppingList = new Hashtable<String, Integer>();

        try {
            csvReader = new BufferedReader(new FileReader(filePath));
            csvReader.readLine(); //reads first line
            String row;
            while ((row = csvReader.readLine()) != null) {

                String[] data = row.split(",");
//                shoppingList.put(data[0], Integer.parseInt(data[1]));
                //my own generated data is shit so i'm setting maxes randomly
                shoppingList.put(data[0], (int) (Math.random() * (300 - 70) + 70));

            }
            csvReader.close();

        } catch (
                IOException e) {
            e.printStackTrace();

        }
        return shoppingList;
    }

    static Hashtable<String, Integer> GenerateShoppingList()
    {

        var shoppingList = new Hashtable<String, Integer>();
        var itemTypeList = new ArrayList<>(Arrays.asList("CPU",
                "Keyboard",
                "Memory Module", "Monitor", "Motherboard", "Mouse", "SSD", "Case"));


        for (String itemType : itemTypeList) {
            shoppingList.put(itemType, (int) (Math.random() * (300 - 120) + 120));
        }

        return shoppingList;
    }

}
