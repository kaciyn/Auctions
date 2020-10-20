import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Hashtable;

public class BidderAgent extends Agent
{
    //enter desired details
    //while Seller not found roam marketplace
    //enter auction
    //wait for bid
    //switch:
    //AcceptBid
    //            RejectBid -> go back to roaming
    //            FinalBid
    //            done
//            BuyProduct
//            End

    Hashtable<String, Float> shoppingList;
    private Hashtable catalogue;
    private AID auctioneerAgent;

    protected void setup()
    {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            catalogue = (Hashtable) args[0];
            System.out.println("Catalogue loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No catalogue found");
            doDelete();
        }

// Printout a welcome message
        System.out.println("Hello! Bidder " + getAID().getName() + "is ready.");


        addBehaviour(new TickerBehaviour(this, 1000)
        {
            @Override
            protected void onTick()
            {
                // Search for auctions
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("auction");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    auctioneerAgent = result[0].getName();

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Register for auction
                myAgent.addBehaviour(new AuctionRegistrationServer());

                myAgent.addBehaviour(new AuctionRegistrationServer());


// Make the agent terminate immediately
                System.out.println("Auction concluded");
                doDelete();
            }
        }
    }


    private class ItemOfferReceiver extends Behaviour
    {
        private MessageTemplate mt; // The template to receive replies

        @Override
        public void action()
        {
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
            //TODO CONTINUE HERE
            // Receive offers from auction
            ACLMessage reply = myAgent.receive(mt);
            if(reply !=null)

            {
                // Reply received
                if (reply.getPerformative() == ACLMessage.PROPOSE) { // This is an offer
                    int price = Integer.parseInt(reply.getContent());
                    if (bestSeller == null || price < bestPrice) {
                        // This is the best offer at present
                        bestPrice = price;
                        bestSeller = reply.getSender();
                    }
                }
                repliesCnt++;
                if (repliesCnt >= sellerAgents.length) {
                    // We received all replies
                }
            }

        }

        @Override
        public boolean done()
        {
            return false;
        });
    }


    //inform auctioneer agent it wishes to register
    private class AuctionRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            // Send the cfp to all sellers
            ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
            inform.addReceiver(auctioneerAgent);
            //send bidder name to auctioneer to register
            inform.setContent(myAgent.getName());
            inform.setConversationId("auction-register");
            inform.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value myAgent.send(cfp);
        }
    }


    // Put agent clean-up operations here
    protected void takeDown()
    {
// Printout a dismissal message
        System.out.println("Bidder " + getAID().getName() + "terminating.");
    }

}