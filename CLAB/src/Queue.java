
public class Queue<T> {
	private Node<T> head;
	private Node<T> tail;
	public Queue() {
		head = tail = null;
	}
	public void add(T val) {
		if(head==null)
			tail = head = new Node<T>(val);
		else {
			tail.setNext(new Node<T>(val));
			tail = tail.next();
		}
	}
	public T get() {
		if(tail==null)
			return null;
		T temp = head.value();
		head = head==tail?tail = null:head.next();
		return temp;
	}
	public T look() {
		if(head==null)
			return null;
		return tail.value();
	}
	public int size() {
		if(isEmpty())
			return 0;
		return size(head)+1;
	}
	private int size(Node<T> t) {
		if(t==tail)
			return 0;
		return size(t.next())+1;
	}
	public boolean isEmpty() {
		return tail==null;
	}
	public String toString() {
		if(head==null)
			return "EMPTY Queue";
		return "Queue-> \r\n"+head.toString();
	}
}
