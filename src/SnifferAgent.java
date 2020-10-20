import jade.core.Agent;
import jade.core.behaviours.Behaviour;

public class SnifferAgent
{
    protected void setup()
    {
    }

    public class B1 extends Behaviour
    {
        private int timesCalled = 0;

        public B1(Agent a)
        {
            myAgent = a;
        }

        @Override
        public void action()
        {
            System.out.println(myAgent.getLocalName());
            timesCalled++;
        }

        @Override
        public boolean done()
        {
            return timesCalled >= 10;
        }
    }
}
