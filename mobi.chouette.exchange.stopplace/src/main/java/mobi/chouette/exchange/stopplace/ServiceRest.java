package mobi.chouette.exchange.stopplace;

public class ServiceRest {

    SayHello coucou;

    public void sayCoucou() {

        try {
            coucou.sayHello("Mathieu");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
