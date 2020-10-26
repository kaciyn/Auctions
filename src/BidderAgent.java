import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

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
    ArrayList<Item> boughtItems;
    private AID auctioneerAgent;

    int cpus = 0;
    int keyboards = 0;
    int cases = 0;
    int memoryModules = 0;
    int monitors = 0;
    int motherboards = 0;
    int mice = 0;
    int ssds = 0;
    ArrayList<Integer> componentAmounts;

    protected void setup()
    {
        boughtItems = new ArrayList<>();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            shoppingList = (Hashtable<String, Integer>) args[0];
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
                    if (result.length > 0) {
                        auctioneerAgent = result[0].getName();
                    }

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Register for auction
                myAgent.addBehaviour(new AuctionRegistrationServer());

                componentAmounts = new ArrayList<>();
                componentAmounts.add(cpus);
                componentAmounts.add(cases);
                componentAmounts.add(keyboards);
                componentAmounts.add(memoryModules);
                componentAmounts.add(monitors);
                componentAmounts.add(motherboards);
                componentAmounts.add(mice);
                componentAmounts.add(ssds);


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

            //incoming message needs to be format [description],[price(int)]
            if (msg != null && msg.getContent().matches("^((^[a-zA-Z\\s]+),(\\d+))$")) {

// Message received. Process it
                String itemDetails = msg.getContent();

                String itemDescription = itemDetails.split(",")[0];


                int currentItemPrice = Integer.parseInt(itemDetails.split(",")[1]);

                ACLMessage reply = msg.createReply();
                reply.setConversationId("bid-on-item");


                //if item is on shopping list, bid with slightly higher price up to own max
                if (shoppingList.containsKey(itemDescription) && currentItemPrice < shoppingList.get(itemDescription) && Collections.min(componentAmounts); //TODO UGH I ACTUALLY CAN'T BE BOTHERED ANYMORE
) {
                    var bidIncrement = (int) (Math.random() * (5 - 1) + 1); //maybe passing this in with the shopping list could be worthwhile someday but cba
                    var newBid = currentItemPrice + bidIncrement; //we're trying to get the lowest possible price right

//                    var newBid = (int) (Math.random() * (shoppingList.get(itemDescription) - currentItemPrice) + currentItemPrice); //so the spec says "somewhere between current and max price so maybe I will actually make this random to make things a bit more spicey

                    System.out.println(myAgent.getLocalName() + " bids " + newBid);


                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(newBid));

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

                    MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
// INFORM Message received. Process it

                        if (msg.getConversationId().equals("auction-concluded")) {
                            step = 1;
                        }
                        else if (msg.getConversationId().equals("bid-successful")) {
                            recordWinningBid(msg);
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
//                    System.out.println("Bidder " + getAID().getName() + " purchased " + boughtItems.size() + " items out of the" + shoppingList.size() + " items they wanted.");


                    var totalSpent = 0;
                    for (Item item : boughtItems) {
//                        System.out.println(myAgent.getLocalName()+" bought "+item.Description);

                        totalSpent = totalSpent + item.CurrentPrice;
                    }

//                    var builtComputers = new HashSet<Computer>();


                    var builtComputers = Collections.min(componentAmounts);
                    //HGHGRHHGRH WHY DO I HAVE TO MAKE EVERYTHING SO COMPLICATED
//                    while (true) { //this feels like a bad way to do this but like....... i'm not paid to be novel
//
//                        Item CPU = boughtItems.stream()
//                                .filter(component -> "CPU".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//
//                        if (CPU == null) {
//                            break;
//                        }
//                        boughtItems.remove(CPU);
//
//                        Item Keyboard = boughtItems.stream()
//                                .filter(component -> "Keyboard".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (Keyboard == null) {
//                            break;
//                        }
//                        boughtItems.remove(Keyboard);
//
//                        Item Case = boughtItems.stream()
//                                .filter(component -> "Keyboard".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (Case == null) {
//                            break;
//                        }
//                        boughtItems.remove(Case);
//
//                        Item MemoryModule = boughtItems.stream()
//                                .filter(component -> "Memory Module".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (MemoryModule == null) {
//                            break;
//                        }
//                        boughtItems.remove(MemoryModule);
//
//                        Item Monitor = boughtItems.stream()
//                                .filter(component -> "Monitor".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (Monitor == null) {
//                            break;
//                        }
//                        boughtItems.remove(Monitor);
//
//                        Item Motherboard = boughtItems.stream()
//                                .filter(component -> "Motherboard".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (Motherboard == null) {
//                            break;
//                        }
//                        boughtItems.remove(Motherboard);
//
//                        Item Mouse = boughtItems.stream()
//                                .filter(component -> "Mouse".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (Mouse == null) {
//                            break;
//                        }
//                        boughtItems.remove(Mouse);
//
//                        Item SSD = boughtItems.stream()
//                                .filter(component -> "SSD".equals(component.Description))
//                                .findFirst()
//                                .orElse(null);
//                        if (SSD == null) {
//                            break;
//                        }
//                        boughtItems.remove(SSD);
//
//                        builtComputers.add(new Computer(CPU, Keyboard, Case, MemoryModule, Monitor, Motherboard, Mouse, SSD));
//                    }

                    if (builtComputers > 0) {
                        System.out.println("Bidder " + getAID().getName() + " purchased enough components to build " + builtComputers + " computers at a price of " + totalSpent / builtComputers + " per computer, spending a total of: " + totalSpent);

                    }
                    else {
                        System.out.println("Bidder " + getAID().getName() + " purchased could not purchase enough components to build a single computer despite spending a total of: " + totalSpent + ". Honestly mood");
                    }


// Printout a dismissal message
//                    System.out.println("Bidder " + getAID().getName() + "terminating.");

                    // Make the agent terminate immediately
                    doDelete();
                    break;
                }

            }
        }

        private void recordWinningBid(ACLMessage msg)
        {
            System.out.println("Bidding won by: " + myAgent.getLocalName());
            var itemDescription = msg.getContent().split(",")[0];
            var itemPrice = Integer.parseInt(msg.getContent().split(",")[1]);

            switch (itemDescription) {
                case "CPU":
                    cpus++;
                    break;
                case "Keyboard":
                    keyboards++;
                    break;
                case "Case":
                    cases++;
                    break;
                case "Memory Module":
                    memoryModules++;
                    break;
                case "Monitor":
                    monitors++;
                    break;
                case "Motherboard":
                    motherboards++;
                    break;
                case "Mouse":
                    mice++;
                    break;
                case "SSD":
                    ssds++;
                    break;
            }

            //add item to bought items, cba fucking about w the extra starting price
            boughtItems.add(new Item(itemDescription, itemPrice, itemPrice));
        }
    }
}