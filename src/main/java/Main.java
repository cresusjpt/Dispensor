import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;

public class Main {

    public static void main(String[] args) {
        try {
            Article art1 = new Article("cocacola_barcode");
            Article art2 = new Article("icetea_barcpde");
            Article art3 = new Article("snicker_barcode");

            Dispensor distributeSystem = new Dispensor();

            art1.setQuantity(10);
            purchasePossible(art1, distributeSystem.dispense(art1));
            art2.setQuantity(7);
            purchasePossible(art2, distributeSystem.dispense(art2));
            art3.setQuantity(2);
            purchasePossible(art3, distributeSystem.dispense(art3));
            exit(0);

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void purchasePossible(Article article, boolean bool) {

        if (bool)
            System.out.println("purchase is possible for " + article.articleToString());
        else
            System.out.println("purchase is not possible for " + article.articleToString());
    }
}
