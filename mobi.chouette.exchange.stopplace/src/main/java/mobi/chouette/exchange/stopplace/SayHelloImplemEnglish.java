package mobi.chouette.exchange.stopplace;

import org.apache.commons.lang.StringUtils;

public class SayHelloImplemEnglish implements SayHello {

    @Override
    public String sayHello(String name) throws Exception {

        if (StringUtils.isEmpty(name)) {
            throw new Exception("Name can't be void !");
        }

        return "hi  " + name;
    }

    @Override
    public String sayGoodbye() throws Exception {
        return "bye";
    }
}
