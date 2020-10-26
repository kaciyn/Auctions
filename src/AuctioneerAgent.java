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
        bidderAgents = new HashSet<>();

        // Register the auctioneer in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("auction");
        sd.setName("auction");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
//get args
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            catalogue = (Hashtable<Integer, Item>) args[0];

            System.out.println("Catalogue loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No catalogue found");
            doDelete();
        }


        System.out.println("Waiting for buyer registration...");

        addBehaviour(new AuctionRegistrationReceiver());

        addBehaviour(new WakerBehaviour(this, 3000)
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


            if (msg != null && msg.getConversationId().equals("register-for-auction")) {
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
//                System.out.println("Unknown/null message received");
                block();
            }

        }


    }

    private class AuctionServer extends Behaviour
    {
        private AID highestBidder = null; // The agent who provides the best offer
        int highestPrice = 0;
        int responseCount = 0; // The counter of replies from seller agents
        int bidCount = 0; //number of bids received
        int refusals = 0;
        HashSet<String> responses = new HashSet<String>();

        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        Item item;


        public void action()
        {
            switch (step) {
                case 0:
                    sendItemCFPs();
                    break;
                case 1:
                    receiveItemBids();
                    break;
                case 2:
                    sendItemAuctionConfirmation();

                    break;
                case 3:
                    //unsold item
                    System.out.println(item.Description + " has not been bid for, or has not met starting price. Auctioning next item");

                    incrementItem();
                    break;

                case 4:
                    endAuction();
                    break;

            }
        }

        private void sendItemCFPs()
        {
            item = catalogue.get(currentItemIndex);
            System.out.println(currentItemIndex);

            System.out.println("Auctioning item: " + item.Description);

            // Send the cfp to all bidders
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
//iterate over bidder hashset
            for (AID bidder : bidderAgents) {
                cfp.addReceiver(bidder);
            }

            cfp.setContent(item.Description + "," + item.CurrentPrice);
            cfp.setConversationId("auction-item");
            cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value myAgent.send(cfp);
// Prepare the template to get proposals
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bid-on-item"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));


            myAgent.send(cfp);

            step = 1;
        }

        private void receiveItemBids()
        {
            //receive bids
            ACLMessage reply = myAgent.receive(mt);
            if (reply != null) {
                // Reply received
                if (reply.getPerformative() == ACLMessage.PROPOSE) { // This is an offer
                    int bid = Integer.parseInt(reply.getContent());

                    if (bid > item.CurrentPrice) {
                        highestPrice = bid;
                        highestBidder = reply.getSender();
                        bidCount++;
                    }
                }
                else {
                    refusals++;
                }
//                responses.add(reply.getContent());
                responseCount++;
                if (responseCount >= bidderAgents.size()) { // received all responses
//                    HashSet<String> sdf = responses;
                    if (bidCount > 1) { //move to next round of bidding
                        newBiddingRound();
                    }
                    else if (highestBidder == null || item.CurrentPrice < item.StartingPrice) {//no bids or didn't meet starting price
                        step = 3;
                    }
                    else {//no new bids or last bidder standing
                        step = 2;
                    }
                }
            }
            else {
                block();
            }
        }

        private void sendItemAuctionConfirmation()
        {
            item.CurrentPrice=highestPrice;
            // Send confirmation to highest bidder
            ACLMessage bidConfirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            bidConfirmation.addReceiver(highestBidder);
            bidConfirmation.setContent(item.Description + "," + item.CurrentPrice);
            bidConfirmation.setConversationId("bid-successful");
            bidConfirmation.setReplyWith("win" + System.currentTimeMillis());
            myAgent.send(bidConfirmation);

            System.out.println(item.Description + " has been bought by: " + highestBidder.getLocalName() + " for " + item.CurrentPrice);

            incrementItem();
        }

        //resets for next item, ends auction if at end of catalogue
        void incrementItem()
        {
            //reset for next item
            responseCount = 0;
            bidCount = 0;
            highestBidder = null; // The agent who provides the best offer
            currentItemIndex++;
            responses = null;
            refusals = 0;
            if (currentItemIndex >= catalogue.size()) {
                step = 4;
            }
            else {
                step = 0;
            }
        }

        void newBiddingRound()
        {
            item.CurrentPrice = highestPrice;

            System.out.println("Starting new round of bidding for " + item.Description + ". Current highest bid is: " + item.CurrentPrice);

            responseCount = 0;
            bidCount = 0;
            step = 0;
        }

        private void endAuction()
        {
            //end auction
            System.out.println("All items bid for. Auction concluded");

            // Send the end of auction notification to all bidders
            ACLMessage endNotification = new ACLMessage(ACLMessage.INFORM);
//iterate over bidder hashset
            for (AID bidder : bidderAgents) {
                endNotification.addReceiver(bidder);
            }

            endNotification.setConversationId("auction-concluded");
            myAgent.send(endNotification);


            myAgent.doDelete();
            step = 5;
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


