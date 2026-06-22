package idlist;

import sequence_abstractions.patterns.IPattern;

public interface JoinableIdList {
    /**
     * It return the intersection IdList that results from the current object and
     * the IdList given as an argument.
     * @param idList IdList with which we join the current IdList.
     * @param equals Flag indicating if we want a intersection for equal relation,
     * or, if it is false, an after relation.
     * @param minSupport Minimum relative support.
     * @return the intersection
     */
    public JoinableIdList join(JoinableIdList idList, boolean equals, double minSupport);

    /**
     * Get the minimum relative support outlined by the IdList, i.e. the number
     * of sequences with any appearance on it.
     * @return the support
     */
    public int getSupport();

    /**
     * Get the string representation of this IdList.
     * @return the string representation of this idlist
     */
    @Override
    public String toString();

    /**
     * It moves to a pattern the sequences where the Idlist is active.
     * @param pattern the pattern
     */
    public void setAppearingSequences(IPattern pattern);

    /**
     * It clears the IdList.
     */
    public void clear();
}
