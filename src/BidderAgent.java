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

import java.util.HashSet;
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

    Hashtable<String, Integer> shoppingList;
    Hashtable<String, Integer> boughtItems;
    private AID auctioneerAgent;

    protected void setup()
    {
        boughtItems = new Hashtable<>();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            shoppingList = (Hashtable) args[0];
            System.out.println("Shopping list loaded");
        }
        else {
// Make the agent terminate immediately
            System.out.println("No Shopping list found");
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
                    if (result.length>0){
                        auctioneerAgent = result[0].getName();
                    }

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Register for auction
                myAgent.addBehaviour(new AuctionRegistrationServer());

                myAgent.addBehaviour(new AuctionBidPerformer());
                myAgent.addBehaviour(new BidResultReceiver());


            }
        });
    }

    //inform auctioneer agent it wishes to register
    private class AuctionRegistrationServer extends OneShotBehaviour
    {
        @Override
        public void action()
        {
            ACLMessage registration = new ACLMessage(ACLMessage.INFORM);
            registration.addReceiver(auctioneerAgent);
            //send bidder name to auctioneer to register
            registration.setContent(myAgent.getName());
            registration.setConversationId("register-for-auction");

            myAgent.send(registration);

        }


    }


    private class AuctionBidPerformer extends CyclicBehaviour
    {
        public void action()
        {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);

            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
// Message received. Process it
                String itemDescription = msg.getContent();

                //TODO nothing to force format of message received which is not good or even check for whether the second bit is a number lol
//for when the price is relevant later
//                String itemDescription = itemDetails.split(",")[0];

                ACLMessage reply = msg.createReply();
                reply.setConversationId("bid-on-item");
                //if item is on shopping list, bid with requisite price
                if (shoppingList.containsKey(itemDescription)) {
//                    int itemPrice = Integer.parseInt(itemDetails.split(",")[1]);
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(shoppingList.get(itemDescription)));
                }
                else {
                    reply.setPerformative(ACLMessage.REFUSE);

                }
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }// End of inner class

    private class BidResultReceiver extends CyclicBehaviour
    {
        private int step = 0;

        public void action()
        {
            switch (step) {
                case 0:

                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
// INFORM Message received. Process it

                        if (msg.getConversationId().equals("auction-concluded")) {
                            step = 1;
                        }
                        else if (msg.getConversationId().equals("bid-successful")) {
                            System.out.println("Bidding won by: " + myAgent.getLocalName());
                            var itemDescription = msg.getContent().split(",")[0];
                            var itemPrice = Integer.parseInt(msg.getContent().split(",")[1]);

                            //add item to bought items, remove from shopping list
                            boughtItems.put(itemDescription, itemPrice);
                            shoppingList.remove(itemDescription,itemPrice);
                        }
                        else {
                            System.out.println(myAgent.getLocalName() + "'s bid unsuccessful");
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 1: {
                    System.out.println("Bidder " + getAID().getName() + " purchased " + boughtItems.size() + " items out of the" + shoppingList.size() + " items they wanted.");

// Printout a dismissal message
                    System.out.println("Bidder " + getAID().getName() + "terminating.");

                    // Make the agent terminate immediately
                    doDelete();
                    break;
                }

            }
        }
    }
}