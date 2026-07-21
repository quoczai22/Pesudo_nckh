package idlist;

/**
 * Memory-efficient DUB bitmap using sorted primitive integer arrays (bit compression/sparse representation).
 * Avoids the overhead of java.util.BitSet allocation and java.util.HashMap creation.
 */
public final class CompressedDUBBitmap {
    private final int[] activeIndices;
    private final int universeSize;
    private final PseudoIdList parent;

    private CompressedDUBBitmap(int[] activeIndices, int universeSize, PseudoIdList parent) {
        this.activeIndices = activeIndices;
        this.universeSize = universeSize;
        this.parent = parent;
    }

    /**
     * Reuses the projected local UCB/IUBC materialized during a pseudo-IDList join.
     * This mirrors the local bitmap carried by the author's projection IDList and
     * avoids rebuilding the same parent-to-child row mapping after every join.
     */
    public static CompressedDUBBitmap fromProjectedLocalUcb(
            PseudoIdList parent,
            PseudoIdList child) {
        if (!child.hasProjectedLocalUcbFor(parent)) {
            throw new IllegalArgumentException("Child does not carry a projected local UCB for the supplied parent");
        }
        return new CompressedDUBBitmap(
                child.getProjectedLocalUcb(),
                child.getProjectedLocalUniverseSize(),
                parent);
    }

    /**
     * Builds a child-presence bitmap in the local-id space of its parent.
     * Uses a two-pointer linear scan instead of a HashMap since sids are sorted.
     */
    public static CompressedDUBBitmap fromParentAndChild(
            PseudoIdList parent,
            PseudoIdList child) {
        int[] activeIndices = new int[child.getRowCount()];
        int parentLocalId = 0;
        int parentRowCount = parent.getRowCount();

        for (int childLocalId = 0; childLocalId < child.getRowCount(); childLocalId++) {
            int childSid = child.getSequenceIdAtRow(childLocalId);
            while (parentLocalId < parentRowCount && parent.getSequenceIdAtRow(parentLocalId) < childSid) {
                parentLocalId++;
            }
            if (parentLocalId >= parentRowCount || parent.getSequenceIdAtRow(parentLocalId) != childSid) {
                throw new IllegalStateException(
                        "Child sequence " + childSid + " does not occur in its parent pseudo-IDList");
            }
            activeIndices[childLocalId] = parentLocalId;
        }

        return new CompressedDUBBitmap(activeIndices, parentRowCount, parent);
    }

    /**
     * Returns the DUB intersection support of two siblings.
     * Uses a linear merge-intersection of two sorted arrays (O(N + M) complexity, zero allocations).
     */
    public int intersectionSupport(CompressedDUBBitmap other) {
        requireSameUniverse(other);
        int count = 0;
        int i = 0;
        int j = 0;
        int[] a = this.activeIndices;
        int[] b = other.activeIndices;
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                count++;
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }
        return count;
    }

    public int cardinality() {
        return activeIndices.length;
    }

    public int getUniverseSize() {
        return universeSize;
    }

    private void requireSameUniverse(CompressedDUBBitmap other) {
        if (other == null || parent != other.parent || universeSize != other.universeSize) {
            throw new IllegalArgumentException(
                    "DUB sibling bitmaps must share one parent local-id universe");
        }
    }
}
