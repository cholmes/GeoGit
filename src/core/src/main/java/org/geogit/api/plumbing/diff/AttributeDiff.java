package org.geogit.api.plumbing.diff;

import com.google.common.base.Optional;

/**
 * An interface to implement by all classes storing differences of an attribute value between 2
 * version of a feature
 * 
 * @param <T>
 */
public interface AttributeDiff {

    public enum TYPE {
        MODIFIED, REMOVED, ADDED
    }

    /**
     * Returns the type of difference that this object represent
     * 
     * @return the type of difference that this object represent
     */
    public TYPE getType();

    /**
     * Returns a reversed version of the attribute difference
     * 
     * @return a reversed version of the attribute difference
     */
    public AttributeDiff reversed();

    /**
     * Returns true if the diff can be applied on the passed value. Return false if the passed value
     * does not represent the old state represented by this attribute difference
     * 
     * @param obj the object representing the original (old) state of the attribute. If the value is
     *        null, it represents that the attribute did not exist previously
     * @return true if the diff can be applied to the passed object
     */
    public boolean canBeAppliedOn(Optional<?> obj);

    /**
     * applies the difference on the passed object, if possible.
     * 
     * @param obj the object representing the original (old) state of the attribute. If the value is
     *        null, it represents that the attribute did not exist previously
     */
    public Optional<?> applyOn(Optional<?> obj);

    /**
     * serializes the difference as text
     */
    public String asText();

    /**
     * Return true if the changes represented by AttributeDiff are in conflict with changes
     * represented by the passed one
     * 
     * @param ad the AttributeDiff to check against
     * @return true if the changes represented by AttributeDiff are in conflict with changes
     *         represented by the passed one
     */
    public boolean conflicts(AttributeDiff otherAd);

}
