
public class Datatable<T, E> {
	private Object[] init = new Object[0];
	private Object[] keys = new Object[0];
	public void set(T s,E o) {
		for(int i=0;i!=keys.length;i++) {
			if(s.equals(keys[i])) {
				init[i] = o;
				return;
			}
		}
		Object[] tempo = init;
		Object[] temps = keys;
		keys = new String[temps.length+1];
		init = new Object[tempo.length+1];
		for(int i=0;i!=tempo.length;i++) {
			keys[i] = temps[i];
			init[i] = tempo[i];
		}
		init[init.length-1] =o;
		keys[keys.length-1] = s;
	}
	public E get(T s) {
		for(int i=0;i!=keys.length;i++) {
			if(s.equals(keys[i])) {
				return (E)init[i];
			}
		}
		return null;
	}
	public Object[] keys() {
		return keys;
	}
	public Object[] values() {
		return init;
	}
	public String toString() {
		String result = "{";
		for(int i=0;i!=init.length;i++) {
			result += keys[i]+"="+init[i]+",";
		}
		return result.substring(0, result.length()-1)+"}";
	}
}
