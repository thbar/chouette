package mobi.chouette.exchange.stopplace;

import org.apache.commons.lang.StringUtils;

public class SayHelloImplemFrench implements SayHello {

    @Override
    public String sayHello(String name) throws Exception {

        if (StringUtils.isEmpty(name)) {
            throw new Exception("Le name ne peut pas etre vide !");
        }

        return "coucou " + name;
    }

    public String sayGoodbye() {
        return "au revoir";
    };
}
