public class Article {
    public final static String DELIMITER = ",";

    private String barcode;
    private int quantity;

    public Article(String barcode) {
        this.barcode = barcode;
    }

    public Article(String barcode, int quantity) {
        this.barcode = barcode;
        this.quantity = quantity;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    // a new method creating a String with the account attributes
    public String articleToString() {
        return this.getBarcode()+ DELIMITER
                + this.getQuantity();
    }

    // a new static method creating and returning a account from a String
    public static Article stringToArticle(String line) {
        String[] results = line.split(DELIMITER);
        return new Article(results[0],Integer.parseInt(results[1]));
    }
}
