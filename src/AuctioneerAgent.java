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
    private int bidderCount;

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

        addBehaviour(new AuctionServer());

        addBehaviour(new WakerBehaviour(this, 60000)
        {
            protected void handleElapsedTimeout()
            {
                System.out.println("Starting auction");

                addBehaviour(new AuctionServer());
            }
        });

    }


    private class RequestPerformer extends Behaviour
    {
        private AID bestSeller; // The agent who provides the best offer
        private int bestPrice; // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents private MessageTemplate mt; // The template to receive replies
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action()
        {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; ++i) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetBookTitle);
                    cfp.setConversationId("book-trade");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value myAgent.send(cfp);
// Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
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
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("book-trade");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
// Purchase successful. We can terminate System.out.println(targetBookTitle+" successfully purchased."); System.out.println("Price = "+bestPrice);
                            myAgent.doDelete();
                        }
                        step = 4;
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done()
        {
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }  // End of inner class RequestPerformer



    }


    /**
     * Inner class OfferRequestsServer.
     * This is the behaviour used by Book-seller agents to serve incoming requests for offer from buyer agents.
     * If the requested book is in the local catalogue the seller agent replies with a PROPOSE message specifying the price. Otherwise a REFUSE message is sent back.
     */
    private class AuctionServer extends CyclicBehaviour
    {
        public void action()
        {

        }
    }// End of inner class

    private class AuctionItemServer extends CyclicBehaviour
    {
          for(
        int i = 1; i<catalogue.size()+1;i++)

        {
            var item = catalogue.get(i);
            System.out.println("Auctioning item: " + item.Description);

        }


        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
// ACCEPT_PROPOSAL Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                Integer price = (Integer) catalogue.remove(title);
                if (price != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println(title + " sold to agent " + msg.getSender().getName());
                }
                else {
// The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }

    } // End of inner class OfferRequestsServer

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


