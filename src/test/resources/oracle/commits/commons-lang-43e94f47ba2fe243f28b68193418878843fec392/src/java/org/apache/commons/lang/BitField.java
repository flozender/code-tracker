/*
 * Copyright 2002-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.lang;

/**
 * <p>Operations on bit-mapped fields.</p>
 *
 * @author Apache Jakarta POI
 * @author Scott Sanders (sanders at apache dot org)
 * @author Marc Johnson (mjohnson at apache dot org)
 * @author Andrew C. Oliver (acoliver at apache dot org)
 * @author Stephen Colebourne
 * @author Pete Gieser
 * @author Gary Gregory
 * @since 2.0
 * @version $Id: BitField.java,v 1.5 2004/02/18 22:59:50 ggregory Exp $
 */
public class BitField {
    
    private final int _mask;
    private final int _shift_count;

    /**
     * <p>Creates a BitField instance.</p>
     *
     * @param mask the mask specifying which bits apply to this
     *  BitField. Bits that are set in this mask are the bits
     *  that this BitField operates on
     */
    public BitField(final int mask) {
        _mask = mask;
        int count = 0;
        int bit_pattern = mask;

        if (bit_pattern != 0) {
            while ((bit_pattern & 1) == 0) {
                count++;
                bit_pattern >>= 1;
            }
        }
        _shift_count = count;
    }

    /**
     * <p>Obtains the value for the specified BitField, appropriately
     * shifted right.</p>
     *
     * <p>Many users of a BitField will want to treat the specified
     * bits as an int value, and will not want to be aware that the
     * value is stored as a BitField (and so shifted left so many
     * bits).</p>
     *
     * @see #setValue(int,int)
     * @param holder the int data containing the bits we're interested
     *  in
     * @return the selected bits, shifted right appropriately
     */
    public int getValue(final int holder) {
        return getRawValue(holder) >> _shift_count;
    }

    /**
     * <p>Obtains the value for the specified BitField, appropriately
     * shifted right, as a short.</p>
     *
     * <p>Many users of a BitField will want to treat the specified
     * bits as an int value, and will not want to be aware that the
     * value is stored as a BitField (and so shifted left so many
     * bits).</p>
     *
     * @see #setShortValue(short,short)
     * @param holder the short data containing the bits we're
     *  interested in
     * @return the selected bits, shifted right appropriately
     */
    public short getShortValue(final short holder) {
        return (short) getValue(holder);
    }

    /**
     * <p>Obtains the value for the specified BitField, unshifted.</p>
     *
     * @param holder the int data containing the bits we're
     *  interested in
     * @return the selected bits
     */
    public int getRawValue(final int holder) {
        return (holder & _mask);
    }

    /**
     * <p>Obtains the value for the specified BitField, unshifted.</p>
     *
     * @param holder the short data containing the bits we're
     *  interested in
     * @return the selected bits
     */
    public short getShortRawValue(final short holder) {
        return (short) getRawValue(holder);
    }

    /**
     * <p>Returns whether the field is set or not.</p>
     *
     * <p>This is most commonly used for a single-bit field, which is
     * often used to represent a boolean value; the results of using
     * it for a multi-bit field is to determine whether *any* of its
     * bits are set.</p>
     *
     * @param holder the int data containing the bits we're interested
     *  in
     * @return <code>true</code> if any of the bits are set,
     *  else <code>false</code>
     */
    public boolean isSet(final int holder) {
        return (holder & _mask) != 0;
    }

    /**
     * <p>Returns whether all of the bits are set or not.</p>
     *
     * <p>This is a stricter test than {@link #isSet(int)},
     * in that all of the bits in a multi-bit set must be set
     * for this method to return <code>true</code>.</p>
     *
     * @param holder the int data containing the bits we're
     *  interested in
     * @return <code>true</code> if all of the bits are set,
     *  else <code>false</code>
     */
    public boolean isAllSet(final int holder) {
        return (holder & _mask) == _mask;
    }

    /**
     * <p>Replaces the bits with new values.</p>
     *
     * @see #getValue(int)
     * @param holder the int data containing the bits we're
     *  interested in
     * @param value the new value for the specified bits
     * @return the value of holder with the bits from the value
     *  parameter replacing the old bits
     */
    public int setValue(final int holder, final int value) {
        return (holder & ~_mask) | ((value << _shift_count) & _mask);
    }

    /**
     * <p>Replaces the bits with new values.</p>
     *
     * @see #getShortValue(short)
     * @param holder the short data containing the bits we're
     *  interested in
     * @param value the new value for the specified bits
     * @return the value of holder with the bits from the value
     *  parameter replacing the old bits
     */
    public short setShortValue(final short holder, final short value) {
        return (short) setValue(holder, value);
    }

    /**
     * <p>Clears the bits.</p>
     *
     * @param holder the int data containing the bits we're
     *  interested in
     * @return the value of holder with the specified bits cleared
     *  (set to <code>0</code>)
     */
    public int clear(final int holder) {
        return holder & ~_mask;
    }

    /**
     * <p>Clears the bits.</p>
     *
     * @param holder the short data containing the bits we're
     *  interested in
     * @return the value of holder with the specified bits cleared
     *  (set to <code>0</code>)
     */
    public short clearShort(final short holder) {
        return (short) clear(holder);
    }

    /**
     * <p>Clears the bits.</p>
     *
     * @param holder the byte data containing the bits we're
     *  interested in
     *
     * @return the value of holder with the specified bits cleared
     *  (set to <code>0</code>)
     */
    public byte clearByte(final byte holder) {
        return (byte) clear(holder);
    }

    /**
     * <p>Sets the bits.</p>
     *
     * @param holder the int data containing the bits we're
     *  interested in
     * @return the value of holder with the specified bits set
     *  to <code>1</code>
     */
    public int set(final int holder) {
        return holder | _mask;
    }

    /**
     * <p>Sets the bits.</p>
     *
     * @param holder the short data containing the bits we're
     *  interested in
     * @return the value of holder with the specified bits set
     *  to <code>1</code>
     */
    public short setShort(final short holder) {
        return (short) set(holder);
    }

    /**
     * <p>Sets the bits.</p>
     *
     * @param holder the byte data containing the bits we're
     *  interested in
     *
     * @return the value of holder with the specified bits set
     *  to <code>1</code>
     */
    public byte setByte(final byte holder) {
        return (byte) set(holder);
    }

    /**
     * <p>Sets a boolean BitField.</p>
     *
     * @param holder the int data containing the bits we're
     *  interested in
     * @param flag indicating whether to set or clear the bits
     * @return the value of holder with the specified bits set or
     *         cleared
     */
    public int setBoolean(final int holder, final boolean flag) {
        return flag ? set(holder) : clear(holder);
    }

    /**
     * <p>Sets a boolean BitField.</p>
     *
     * @param holder the short data containing the bits we're
     *  interested in
     * @param flag indicating whether to set or clear the bits
     * @return the value of holder with the specified bits set or
     *  cleared
     */
    public short setShortBoolean(final short holder, final boolean flag) {
        return flag ? setShort(holder) : clearShort(holder);
    }

    /**
     * <p>Sets a boolean BitField.</p>
     *
     * @param holder the byte data containing the bits we're
     *  interested in
     * @param flag indicating whether to set or clear the bits
     * @return the value of holder with the specified bits set or
     *  cleared
     */
    public byte setByteBoolean(final byte holder, final boolean flag) {
        return flag ? setByte(holder) : clearByte(holder);
    }

}
