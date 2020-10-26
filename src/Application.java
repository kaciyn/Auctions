import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.core.Runtime;
import jade.wrapper.StaleProxyException;

import java.util.Hashtable;


public class Application
{

    public static void main(String[] args)
    {


        var catalogueCsvPath = "components.csv";


        var catalogue = CsvParsers.ParseCatalogueCsv(catalogueCsvPath);

        //setup jade environment
        Profile myProfile = new ProfileImpl();
        Runtime myRuntime = Runtime.instance();
        ContainerController myContainer = myRuntime.createMainContainer(myProfile);

        try {

            //start agent controller (also an agent (rma)
            AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
            rma.start();

            AgentController auctioneerAgent = myContainer.createNewAgent("auctioneer", AuctioneerAgent.class.getCanonicalName(), new Hashtable[]{catalogue});
            auctioneerAgent.start();

            var biddersToGenerate=3;
            generateBiddersWithRandomisedShoppingLists(myContainer,biddersToGenerate);

//            generateBiddersWithCsvShoppingLists(myContainer);


        } catch (Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
            e.printStackTrace();


        }

    }

    private static void generateBiddersWithRandomisedShoppingLists(ContainerController myContainer,int biddersToGenerate) throws StaleProxyException
    {
        for (int i = 0; i < biddersToGenerate; i++) {


            var shoppingList = CsvParsers.GenerateShoppingList();

            AgentController bidderAgent = myContainer.createNewAgent("bidder" + i, BidderAgent.class.getCanonicalName(), new Hashtable[]{shoppingList});
            bidderAgent.start();
        }
    }

    private static void generateBiddersWithCsvShoppingLists(ContainerController myContainer) throws StaleProxyException
    {
        for (int i = 0; i < 5; i++) {

            var shoppingListCsvPath = "pcBuilderShoppingList" + i + ".csv";

            var shoppingList = CsvParsers.ParseShoppingListCsv(shoppingListCsvPath);

            AgentController bidderAgent = myContainer.createNewAgent("bidder" + i, BidderAgent.class.getCanonicalName(), new Hashtable[]{shoppingList});
            bidderAgent.start();
        }
    }
}
