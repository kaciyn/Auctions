import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;

public class AuctioneerAgent extends Agent
{
    //enter desired details
    //choose negotiation strategy
//                calculate new bid
//                offer bids to buyers
//simultaneously
    //while Buyer not found roam marketplace
    //Start auction
    //Offer bids to buyers
    //switch:
    //Multiple buyers
//                    Calculate new bid (new price higher than previous bid according to neg. strategy)
//                    Offer Bids
//                Single buyer
//                        Sell
//                        done
//                RejectAll
//                TerminateAuction
//                done
    // The catalogue of items for sale (maps the item description to its price)
    private Hashtable<Integer, Item> catalogue;
    private HashSet<AID> bidderAgents;
    private int currentItemIndex;

    protected void setup()
    {

        // Register the auctioneer in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("auction");
        sd.setName("Auction");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
//get args
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


        System.out.println("Waiting for buyer registration...");

        addBehaviour(new AuctionRegistrationReceiver());

        addBehaviour(new WakerBehaviour(this, 60000)
        {
            protected void handleElapsedTimeout()
            {
                System.out.println("Starting auction");
                currentItemIndex = 1;
                addBehaviour(new AuctionServer());
            }
        });

    }

    //receives bidder registrations for auction
    private class AuctionRegistrationReceiver extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);


            if (msg != null && Objects.equals(msg.getConversationId(), "register-for-auction")) {
// INFORM Message received. Process it

                AID newBidderAID = msg.getSender();
                bidderAgents.add(newBidderAID);

                //leaving this for now

//                ACLMessage reply = msg.createReply();
//                reply.setPerformative(ACLMessage.INFORM);
//                reply.setContent("Registration confirmed");
//
//                System.out.println(newBidderAID.getName() + " registered for auction");
//
//                myAgent.send(reply);
            }
            else {
                System.out.println("Unknown/null message received");
                block();
            }

        }


    }

    private class AuctionServer extends Behaviour
    {
        private AID highestBidder; // The agent who provides the best offer
        private int highestBid; // The best offered price
        private int responseCount = 0; // The counter of replies from seller agents private MessageTemplate mt; // The template to receive replies
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        Item item;

        public void action()
        {
            switch (step) {
                case 0:

                    item = catalogue.get(currentItemIndex);
                    System.out.println("Auctioning item: " + item.Description);

                    // Send the cfp to all bidders
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
//iterate over bidder hashset
                    for (AID bidder : bidderAgents) {
                        cfp.addReceiver(bidder);
                    }

                    cfp.setContent(item.Description);
                    cfp.setConversationId("auction-item");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value myAgent.send(cfp);
// Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bid-on-item"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all bids/refusals from bidder
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) { // This is an offer
                            int bid = Integer.parseInt(reply.getContent());
                            if (highestBidder == null || bid > highestBid) {
                                // This is the highest bid at present
                                highestBid = bid;
                                highestBidder = reply.getSender();
                            }
                        }
                        responseCount++;
                        if (responseCount >= bidderAgents.size()) {
                            // received all responses
                            if (highestBidder == null || highestBid < item.StartingPrice) {
                                step = 3;
                            }
                            else {
                                step = 2;

                            }
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Send confirmation to bidder
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(highestBidder);
                    order.setContent(item.Description);
                    order.setConversationId("bid-win");
                    order.setReplyWith("win" + System.currentTimeMillis());
                    myAgent.send(order);

                    currentItemIndex++;
                    if (currentItemIndex > catalogue.size()) {
                        step = 4;

                    }
                    else {
                        step = 1;
                    }

                    break;
                case 3:
                    //unsold item
                    System.out.println(item.Description + " has not been bid for, or has not met starting price. Auctioning next item");

                    currentItemIndex++;
                    if (currentItemIndex > catalogue.size()) {
                        step = 4;

                    }
                    else {
                        step = 1;
                    }
                    break;
                case 4:
                    //end auction
                    System.out.println("All items bid for. Auction concluded");
                    myAgent.doDelete();
                    step = 5;
                    break;

            }

        }

        public boolean done()
        {
            return (step == 5);
        }
    }


    protected void takeDown()
    {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Auctioneer " + getAID().getName() + " terminating.");
    }
}


