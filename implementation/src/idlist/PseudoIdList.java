package idlist;

import sequence_abstractions.patterns.IPattern;

/**
 * Inspired by the author's projected pseudo-IDList. It keeps a backbone
 * continuous-array IDList and a projected local UCB/IUBC array that marks which
 * parent rows survive a join.
 *
 * @author Huy
 */
public class PseudoIdList implements ProjectedIdList {

	/**
	 * A continuous array that represents a Projected IDList
	 */
	protected IntArrayBuffer sequence_ItemsetEntries;
	protected IntArrayBuffer backboneIDList = null;
	protected int[] projectedLocalUcb = null;
	protected int projectedLocalUniverseSize = 0;
	protected PseudoIdList projectedLocalParent = null;
	protected int support = 0;

	/**
	 * A bitset to keep just the sequences where a pattern appears. Is the bitset
	 * representation of the keyset of the map sequence_ItemsetEntries
	 */
//    protected BitSet sequences = null;

	/**
	 * Standard Constructor. It creates an empty IdList
	 */
	public PseudoIdList() {
		sequence_ItemsetEntries = new IntArrayBuffer();
	}


	private PseudoIdList(IntArrayBuffer backboneIDList, IntArrayBuffer projectedIDlist) {
		this.backboneIDList = backboneIDList;
		this.sequence_ItemsetEntries = projectedIDlist;
	}

	private PseudoIdList(
			IntArrayBuffer backboneIDList,
			IntArrayBuffer projectedIDlist,
			int[] projectedLocalUcb,
			int projectedLocalUniverseSize,
			PseudoIdList projectedLocalParent) {
		this.backboneIDList = backboneIDList;
		this.sequence_ItemsetEntries = projectedIDlist;
		this.projectedLocalUcb = projectedLocalUcb;
		this.projectedLocalUniverseSize = projectedLocalUniverseSize;
		this.projectedLocalParent = projectedLocalParent;
	}

	/**
	 * RegisterBit here is a function that put information (sid, tid) to the
	 * backbone idlist, not the project one.
	 * 
	 * which you want to add the tid, it would be -1 if you dont want to use it.
	 * 
	 * @param sid The sequence identifier where the pattern appears
	 * @param tid The itemset timestamp where the pattern appears
	 */
	public void registerBit(int index, int tid, int sid) {
		if (backboneIDList == null) {
			backboneIDList = new IntArrayBuffer();
			this.sequence_ItemsetEntries.add(0); // Location of the first sequence id in backbone idlist.
			this.sequence_ItemsetEntries.add(1); // Location of the first tid in the sequence.
			this.backboneIDList.add(-sid);
			this.backboneIDList.add(tid);
			support++;
		} else {
			int lastSeqIndex = sequence_ItemsetEntries.get(sequence_ItemsetEntries.size() - 2);
			int lastSeqSid = backboneIDList.get(lastSeqIndex);
			if (-sid != lastSeqSid) {
				backboneIDList.add(-sid);
				sequence_ItemsetEntries.add(backboneIDList.size() - 1);
				sequence_ItemsetEntries.add(backboneIDList.size());
				support++;
			}
			backboneIDList.add(tid);
		}

	}

	/**
	 * // * It adds the appearances of the pattern in the itemsets contained in
	 * "tids" and sequence "sid" // * @param sid The sequence identifier wher the
	 * pattern appears // * @param tids The set of itemset timestamps where the
	 * pattern appears //
	 */
//    public void registerNBits(int sid, List<Integer> tids) {
//        BitSet bitmap = sequence_ItemsetEntries.get(sid);
//        if (bitmap == null) {
//            bitmap = new BitSet(BIT_PER_SECTION);
//            sequence_ItemsetEntries.put(sid, bitmap);
//            sequences.set(sid);
//        }
//        for (Integer tid : tids) {
//            int bitIndex = tid;
//            bitmap.set(bitIndex, true);
//        }
//    }

	/**
	 * It return the number of sequences where the IdList is active.
	 * 
	 * @return the number of sequences
	 */
	public int getSupport() {
		return this.support;
//        return sequences.cardinality();
	}

	/**
	 * Get the string representation of this kind of IdList
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < sequence_ItemsetEntries.size(); i++) {
			int anElem = sequence_ItemsetEntries.get(i);
			if (anElem < 0) {
				buffer.append("[sid=");
				buffer.append(anElem);
			} else {
				buffer.append(" tid=");
				buffer.append(anElem);
				buffer.append("]");
			}
		}
		return buffer.toString();
	}

	/***
	 * For compatibility with change in IDList (adjusting for weight)
	 */
	@Override
	public ProjectedIdList join(JoinableIdList idList, boolean equals, double minSupport) {
		// We create the result projected idlist
		IntArrayBuffer intersection = new IntArrayBuffer(this.sequence_ItemsetEntries.size());
		// List<SIDToBitmap> intersection = new ArrayList<SIDToBitmap>();
		// We create an empty bitset where we will keep the pattern appearances

		// Cast in the argument IdList
		PseudoIdList idStandard = (PseudoIdList) idList;

		// And we get the map of entries of bitsets
		IntArrayBuffer idListMap = idStandard.getSequenceItemsetEntries();
		// BitSet newSequences = new BitSet(idListMap.size()/2);
		// BitSet newSequences = null;
		// BitSet newSequences = new BitSet();
		// List<SIDToBitmap> entries = idListMap.entrySet();
		IntArrayBuffer projectedLocalIds = new IntArrayBuffer(this.getRowCount());
		// If flag equals is activated
		if (equals) {
			// We execute a join for equal relation
//			equalLoop(intersection, idListMap, null);
			IntArrayBuffer intersectionBackboneList = new IntArrayBuffer(this.backboneIDList.size());
			equalLoop(intersection, intersectionBackboneList, idListMap, idStandard.backboneIDList, projectedLocalIds);
			intersection.trim();
			intersectionBackboneList.trim();
			int[] localUcb = toTrimmedArray(projectedLocalIds);
			PseudoIdList output = new PseudoIdList(
					intersectionBackboneList,
					intersection,
					localUcb,
					this.getRowCount(),
					this);
			output.support = intersection.size() / 2;
			return output;
		}
//			else {
			// Otherwise we execute a join for an after relation
		laterLoop(intersection, idListMap, idStandard.backboneIDList, projectedLocalIds);
		intersection.trim();
		int[] localUcb = toTrimmedArray(projectedLocalIds);
		PseudoIdList output = new PseudoIdList(
				idStandard.backboneIDList,
				intersection,
				localUcb,
				this.getRowCount(),
				this);
		output.support = intersection.size() / 2;
		return output;
//		}
		// We create the new IdList from the resulting map and sequences bitset

//		output.sequences = newSequences;
//		return null;
	}

	/**
	 * It return the intersection IdList that results from the current object and
	 * the IdList given as an argument.
	 * 
	 * @param idList     IdList with which we join the current IdList.
	 * @param equals     Flag indicating if we want a intersection for equal
	 *                   relation, or, if it is false, an after relation.
	 * @param minSupport Minimum relative support.
	 * @return the resulting idlist
	 */
	public ProjectedIdList join(ProjectedIdList idList, boolean equals, int minSupport) {
		return this.join(idList, equals, (double) minSupport);
	}

	/**
	 * Method to do the join operation under equal relation. Joining with item
	 * extension results in backbone idlist (the real idlist)
	 * 
	 * @param sequenceItemsetEntries Map where we put the new elements resulting
	 *                               from the join method
	 * @param entries                Map with which we are going to join the current
	 *                               IdList.
	 * @param sequences              New bitset where we keep the sequences where
	 *                               the new IdList is active
	 * @throws Exception
	 */
	private void equalLoop(IntArrayBuffer intersection, IntArrayBuffer intersectionBackboneList, IntArrayBuffer entries,
			IntArrayBuffer otherBackboneIDList, IntArrayBuffer projectedLocalIds) {
		int i = 0, j = 0, aSid, bSid, aTidProjIndex, bTidProjIndex;
		int aTid, bTid;
		boolean firstElem = true;
		while (true) {
			if (i >= this.sequence_ItemsetEntries.size() || j >= entries.size())
				break;
			aSid = this.backboneIDList.get(this.sequence_ItemsetEntries.get(i)); // Note that sids are negative numbers
			bSid = otherBackboneIDList.get(entries.get(j));

			if (aSid == bSid) {
				aTidProjIndex = this.sequence_ItemsetEntries.get(i + 1);
				bTidProjIndex = entries.get(j + 1);
				aTid = this.backboneIDList.get(aTidProjIndex); // 1st Elem in projected idlist a
				bTid = otherBackboneIDList.get(bTidProjIndex); // 1nd Elem in projected idlist b
				firstElem = true;
				while (bTid >= 0 && aTid >= 0) // Still in the current sequence
				{
					if (aTid < bTid) {
//    					intersection.add(aTidProjIndex);
						aTidProjIndex++;
						if (aTidProjIndex >= this.backboneIDList.size())
							break;
						aTid = backboneIDList.get(aTidProjIndex);
					} else if (aTid == bTid) {
						if(firstElem) {
							firstElem = false;
							intersection.add(intersectionBackboneList.size());
							intersection.add(intersectionBackboneList.size() + 1);
							intersectionBackboneList.add(aSid);							
							projectedLocalIds.add(i / 2);
						}
						intersectionBackboneList.add(aTid);
						bTidProjIndex++;
						aTidProjIndex++;
						if (aTidProjIndex >= this.backboneIDList.size() || bTidProjIndex >= otherBackboneIDList.size())
							break;
						bTid = otherBackboneIDList.get(bTidProjIndex);
						aTid = backboneIDList.get(aTidProjIndex);
					} else { //aTid > bTid
						bTidProjIndex++;
						if (bTidProjIndex >= otherBackboneIDList.size())
							break;
						bTid = otherBackboneIDList.get(bTidProjIndex);
//						aTid = backboneIDList.get(aTidProjIndex);
					}
				}
				i += 2;
				j += 2;
			} else {
				if (aSid < bSid) // aSid and bSid is negative number, so it must be !(aSid > bSid) --> aSid <
									// bSid
					j += 2;
				else
					i += 2;
			}
		}
	}

	/**
	 * Method to do the join operation under after relation.
	 * 
	 * @param sequenceItemsetEntries Map where we put the new elements resulting
	 *                               from the join method
	 * @param entries                Map with which we are going to join the current
	 *                               IdList.
	 * @param sequences              New bitset where we keep the sequences where
	 *                               the new IdList is active
	 */
	private void laterLoop(IntArrayBuffer intersection, IntArrayBuffer entries, IntArrayBuffer otherBackboneIDList,
			IntArrayBuffer projectedLocalIds) {
		// For each entry
		int i = 0, j = 0, aSid, bSid, aTidProjIndex, bTidProjIndex;
		int aTid, bTid;
		while (true) {
			if (i >= this.sequence_ItemsetEntries.size() || j >= entries.size())
				break;
			aSid = this.backboneIDList.get(this.sequence_ItemsetEntries.get(i)); // Note that sids are negative numbers
			bSid = otherBackboneIDList.get(entries.get(j));

			if (aSid == bSid) {
				aTidProjIndex = this.sequence_ItemsetEntries.get(i + 1);
				bTidProjIndex = entries.get(j + 1);
				aTid = this.backboneIDList.get(aTidProjIndex); // 1st Elem in projected idlist a
				bTid = otherBackboneIDList.get(bTidProjIndex); // 1nd Elem in projected idlist b
				while (bTid >= 0) // Still in the current sequence
				{
					if (aTid < bTid) {
//    					intersection.add(aTidProjIndex);
						intersection.add(entries.get(j));
						intersection.add(bTidProjIndex);
						projectedLocalIds.add(i / 2);
						break;
					}
					bTidProjIndex++;
					if (bTidProjIndex >= otherBackboneIDList.size())
						break;
					bTid = otherBackboneIDList.get(bTidProjIndex);
				}
				i += 2;
				j += 2;
			} else {
				if (aSid < bSid) // aSid and bSid is negative number, so it must be !(aSid > bSid) --> aSid <
									// bSid
					j += 2;
				else
					i += 2;
			}
		}

	}

	/**
	 * Setter method to insert in the pattern given as parameter the set of sequence
	 * identifiers where the IdList appears, so the pattern does
	 * 
	 * @param pattern Pattern where we insert the sid list
	 */
	public void setAppearingSequences(IPattern pattern) {
//        pattern.setAppearingIn(sequences);
	}

	public void clear() {
	}

	/**
	 * // * It adds, for a particular sequence, all the apperarances given by the
	 * list // * of itemsets // * @param sid Sequence id where the itemsets will be
	 * inserted // * @param itemsets Set of itemsets to insert in a sequence //
	 */
//    public void addAppearancesInSequence(Integer sid, List<Integer> itemsets) {
//        registerNBits(sid, itemsets);
//    }

	/**
	 * Getter method for the map of entries
	 * 
	 * @return the map of entries <integer, bitset>
	 */
	public IntArrayBuffer getSequenceItemsetEntries() {
		return sequence_ItemsetEntries;
	}

	/**
	 * Set the map of entries
	 * 
	 * @param sequenceItemsetEntries the map of entries
	 */
	public void setSequenceItemsetEntries(IntArrayBuffer sequenceItemsetEntries) {
		this.sequence_ItemsetEntries = sequenceItemsetEntries;
	}

	public IntArrayBuffer getBackbone_idlist() {
		return backboneIDList;
	}

	/**
	 * Returns the number of sequence rows represented by this data/pseudo-IDList.
	 * Each row occupies two projection entries: the sequence-header pointer and
	 * the start-index pointer.
	 */
	public int getRowCount() {
		return sequence_ItemsetEntries.size() / 2;
	}

	/**
	 * Resolves the original sequence id represented by a local row.
	 *
	 * @param localId zero-based local row id in this IDList
	 * @return original sequence id (UCID/CID index used by the implementation)
	 */
	public int getSequenceIdAtRow(int localId) {
		if (localId < 0 || localId >= getRowCount()) {
			throw new IndexOutOfBoundsException("Local id: " + localId + ", rows: " + getRowCount());
		}
		int sequenceHeaderIndex = sequence_ItemsetEntries.get(localId * 2);
		return -backboneIDList.get(sequenceHeaderIndex);
	}

	public boolean hasProjectedLocalUcbFor(PseudoIdList parent) {
		return projectedLocalUcb != null && projectedLocalParent == parent;
	}

	public int[] getProjectedLocalUcb() {
		return projectedLocalUcb;
	}

	public int getProjectedLocalUniverseSize() {
		return projectedLocalUniverseSize;
	}

	public PseudoIdList getProjectedLocalParent() {
		return projectedLocalParent;
	}

	private static int[] toTrimmedArray(IntArrayBuffer buffer) {
		int[] values = new int[buffer.size()];
		for (int i = 0; i < values.length; i++) {
			values[i] = buffer.get(i);
		}
		return values;
	}

	public void setBackbone_idlist(IntArrayBuffer backbone_idlist) {
		this.backboneIDList = backbone_idlist;
	}
}
