package sequence_abstractions.patterns;

import java.util.List;
import sequence_abstractions.abstractions.ItemAbstractionPair;

public interface IPattern {

	/**
	 * Get the string representation of this itemset adjusted to SPMF format.
	 * 
	 * @param outputSequenceIdentifiers
	 * @return the string representation.
	 */
	String toStringToFile(boolean outputSequenceIdentifiers);

	/**
	 * It clones a pattern
	 * 
	 * @return the resulting pattern
	 */
	IPattern clonePattern();

	/**
	 * It obtains the elements of the pattern
	 * 
	 * @return the list of elements in this pattern.
	 */
	List<ItemAbstractionPair> getElements();

	/**
	 * It obtains the Ith element of the pattern
	 * 
	 * @param i the ith element
	 * @return the element
	 */
	ItemAbstractionPair getIthElement(int i);

	/**
	 * It obtains the last but one element of the pattern
	 * 
	 * @return the last but one element
	 */
	ItemAbstractionPair getLastButOneElement();

	/**
	 * It obtains the last element of the pattern
	 * 
	 * @return the last element
	 */
	ItemAbstractionPair getLastElement();

	List<ItemAbstractionPair> getNElements(int n);

	/**
	 * Setter method to set the elements of the pattern
	 * 
	 * @param elements
	 */
	void setElements(List<ItemAbstractionPair> elements);

	/**
	 * It adds an item with its abstraction in the pattern. The new pair is
	 * added in the last position of the pattern.
	 * 
	 * @param pair
	 */
	void add(ItemAbstractionPair pair);

	/**
	 * It returns the number of items contained in the pattern
	 * 
	 * @return the number of items
	 */
	int size();

	/**
	 * Compare this pattern with another
	 * Since we always compare elements which belong to the same equivalence class
	 * we only make the comparison of the last items of both patterns.
	 * 
	 * @param arg another pattern
	 * @return 0 if equals, -1 if this one is smaller, otherwise 1
	 */
	int compareTo(IPattern arg);

	boolean equals(Object arg);

	/**
	 * It informs us if this pattern is a prefix of the argument given pattern.
	 * 
	 * @param p the pattern
	 * @return true if the pattern is a prefix, otherwise false.
	 */
	boolean isPrefix(IPattern p);

	/**
	 * It returns the list of sequence IDs where the pattern appears.
	 * 
	 * @return the list of sequence IDs
	 */
	Object getAppearingIn();

	/**
	 * it set the list of sequence IDs where the pattern appears.
	 * 
	 * @param appearingIn
	 */
	void setAppearingIn(Object appearingIn);

	void clear();

}
