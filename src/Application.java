import Agents.BidderAgent;
import Agents.AuctioneerAgent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.core.Runtime;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;


public class Application
{

    public static void main(String[] args)
    {

        var catalogue = new Hashtable<Integer, Item>();

        var csvPath = "components.csv";



        CsvParser.ParseCsv(csvPath, catalogue);

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

            for (int i = 0; i < 5; i++) {
                AgentController bidderAgent = myContainer.createNewAgent("bidder"+ i, BidderAgent.class.getCanonicalName(), new Hashtable[]{catalogue});
                bidderAgent.start();
            }





        } catch (Exception e) {
            System.out.println("Exception starting agent: " + e.toString());
            e.printStackTrace();


        }

    }
}
