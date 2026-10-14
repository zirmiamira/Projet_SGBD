package sgbd.disk;

public class PageId implements IPageId {
    private final int index;

    public PageId(int index) {
        this.index = index;
    }
    @Override
    public int getIndex() {
        return index;
    }
    @Override
    public boolean equals(Object o) {
    	if ((o == null)  || !(o instanceof PageId)) {
    		return false;
    	}
        if (this == o) {
            return true;
        }
        return index == ((PageId) o).index;
    }
    @Override
    public int hashCode() {
        return Integer.hashCode(index);
    }
}