import java.util.Comparator;
import java.util.Map;

class ValueComparator implements Comparator<Integer> {

    Map<Integer, Integer> base;
    public ValueComparator(Map<Integer, Integer> base) {
        this.base = base;
    }

	@Override
	public int compare(Integer o1, Integer o2) {
	     if (base.get(o1) >= base.get(o2)) {
	            return -1;
	        } else {
	            return 1;
	        } // returning 0 would merge keys
	}
}