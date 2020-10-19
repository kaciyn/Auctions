import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;

public class CsvParser
{
    static Hashtable<Integer, Item> ParseCsv(String filePath, Hashtable<Integer, Item> catalogue)
    {
        BufferedReader csvReader;

        try {
            csvReader = new BufferedReader(new FileReader(filePath));
            csvReader.readLine(); //reads first line
            String row;
            while ((row = csvReader.readLine()) != null) {

                String[] data = row.split(",");
                catalogue.put(Integer.parseInt(data[0]) - 1, new Item(data[1], Integer.parseInt(data[2])));
            }

            csvReader.close();

        } catch (
                IOException e) {
            e.printStackTrace();

        }
        return catalogue;
    }

}
