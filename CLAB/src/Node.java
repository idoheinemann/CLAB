
public class Node<T> {
	
	private Node<T> next;
	private T value;
	public Node(T val) {
		value = val;
	}
	public Node(T val,Node<T> n) {
		value = val;
		next = n;
	}
	public Node(Node<T> n,T val) {
		value = val;
		Node<T> temp = n.next();
		n.setNext(this);
		setNext(temp);
	}
	public Node<T> next() {
		return next;
	}
	public void setNext(Node<T> next) {
		this.next = next;
	}
	public T value() {
		return value;
	}
	public void setValue(T value) {
		this.value = value;
	}
	@Override
	public String toString() {
		return "("+value+")"+(next!=null?" -> \r\n"+next:"");
	}
	public boolean isLast() {
		return next==null;
	}
	public void setNext(T v) {
		setNext(new Node<T>(v));
		
	}
}
