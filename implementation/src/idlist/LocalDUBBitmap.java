package idlist;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * DUB bitmap whose bit positions are local row ids of a shared parent
 * pseudo-IDList. As the parent support shrinks during DFS, the bitmap universe
 * shrinks with it instead of remaining proportional to the whole database.
 */
public final class LocalDUBBitmap {
    private final BitSet bits;
    private final int universeSize;
    private final PseudoIdList parent;

    private LocalDUBBitmap(BitSet bits, int universeSize, PseudoIdList parent) {
        this.bits = bits;
        this.universeSize = universeSize;
        this.parent = parent;
    }

    /**
     * Builds a child-presence bitmap in the local-id space of its parent.
     * Every child sequence must also occur in the parent by downward closure.
     */
    public static LocalDUBBitmap fromParentAndChild(
            PseudoIdList parent,
            PseudoIdList child) {
        Map<Integer, Integer> parentLocalIds = new HashMap<>();
        for (int localId = 0; localId < parent.getRowCount(); localId++) {
            int sequenceId = parent.getSequenceIdAtRow(localId);
            Integer previous = parentLocalIds.put(sequenceId, localId);
            if (previous != null) {
                throw new IllegalStateException("Parent contains duplicate sequence row: " + sequenceId);
            }
        }

        BitSet bits = new BitSet(parent.getRowCount());
        for (int childLocalId = 0; childLocalId < child.getRowCount(); childLocalId++) {
            int sequenceId = child.getSequenceIdAtRow(childLocalId);
            Integer parentLocalId = parentLocalIds.get(sequenceId);
            if (parentLocalId == null) {
                throw new IllegalStateException(
                        "Child sequence " + sequenceId + " does not occur in its parent pseudo-IDList");
            }
            bits.set(parentLocalId);
        }
        return new LocalDUBBitmap(bits, parent.getRowCount(), parent);
    }

    /** Returns the DUB intersection support of two siblings. */
    public int intersectionSupport(LocalDUBBitmap other) {
        requireSameUniverse(other);
        BitSet intersection = (BitSet) bits.clone();
        intersection.and(other.bits);
        return intersection.cardinality();
    }

    public int cardinality() {
        return bits.cardinality();
    }

    public int getUniverseSize() {
        return universeSize;
    }

    public BitSet toBitSetCopy() {
        return (BitSet) bits.clone();
    }

    private void requireSameUniverse(LocalDUBBitmap other) {
        if (other == null || parent != other.parent || universeSize != other.universeSize) {
            throw new IllegalArgumentException(
                    "DUB sibling bitmaps must share one parent local-id universe");
        }
    }
}
