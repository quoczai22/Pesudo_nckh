package idlist;

import java.util.Arrays;

public class IntArrayBuffer {
	// The storage for the elements.
	// The capacity is the length of this array.
	private int[] data;

	// The number of elements (logical size).
	// It must be smaller than the capacity.
	private int size;

	// Constructs an empty DynamicArray
	public IntArrayBuffer() {
		data = new int[16];
		size = 0;
	}

	// Constructs an empty DynamicArray with the
	// specified initial capacity.
	public IntArrayBuffer(int capacity) {
		if (capacity < 1)
			capacity = 2;
		data = new int[capacity];
		size = 0;
	}

	public void trim() {
		this.data = Arrays.copyOf(this.data, size);
	}

	// Increases the capacity, if necessary, to ensure that
	// it can hold at least the number of elements
	// specified by the minimum capacity argument.
	public void ensureCapacity(int minCapacity) {
		int oldCapacity = data.length;
		if (minCapacity > oldCapacity) {
			int newCapacity = (int) (oldCapacity * 1.25) + 1;
			if (newCapacity < minCapacity)
				newCapacity = minCapacity;
			data = Arrays.copyOf(data, newCapacity);
		}
	}

	// Returns the logical size
	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	// Returns the element at the specified position.
	public int get(int index) {
		// rangeCheck(index);
		return data[index];
	}

	// Appends the specified element to the end.
	public boolean add(int element) {
		if (size == data.length)
			ensureCapacity(size + 1);
		data[size++] = element;
		return true;
	}

	public boolean add(int index, int element) {
		if (index == size)
			add(element);
		else if (index > size) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
		} else {
			System.arraycopy(data, index, data, index + 1,
					size - index);
			data[index] = element;
			size++;
		}
		return true;
	}

	// Removes all of the elements.
	public void clear() {
		size = 0;
	}

	// Replaces the element at the specified position
	public int set(int index, int element) {
		// rangeCheck(index);
		int oldValue = data[index];
		data[index] = element;
		return oldValue;
	}

	public int capacity() {
		return data.length;
	}
}
