public class PriceCalTester {

    private final PriceCalculator priceCalculator = new PriceCalculator();

    public boolean testMethodEntry() {
        return pricePlusTest1(priceCalculator)
                && pricePlusTest2(priceCalculator)
                && pricePlusTest3(priceCalculator);
    }

    public boolean pricePlusTest1(PriceCalculator calculator) {
        return calculator.calTotalPricePlus(10, 20) == 50;
    }

    public boolean pricePlusTest2(PriceCalculator calculator) {
        return calculator.calTotalPricePlus(2, 5) == 27;
    }

    public boolean pricePlusTest3(PriceCalculator calculator) {
        return calculator.calTotalPricePlus(9231, 8892) == 18123;
    }

    public static void main(String[] args) {
        PriceCalTester tester = new PriceCalTester();
        boolean result = tester.testMethodEntry();
        System.out.println("Test result: " + (result ? "PASS" : "FAIL"));
    }
}
