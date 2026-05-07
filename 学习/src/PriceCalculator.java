public class PriceCalculator {

    public int calTotalPricePlus(int priceA, int priceB) {
        int total = priceA + priceB;
        if (total < 100) {
            total += 20;
        }
        return total;
    }
}
