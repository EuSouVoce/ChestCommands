/*
 * Copyright (C) Jan Schultke SPDX-License-Identifier: MIT
 */
package me.filoghost.chestcommands.util.nbt;

import java.util.Arrays;

/**
 * The {@code TAG_Long_Array} tag.
 */
public final class NBTLongArray extends NBTTag {

    private final long[] value;

    /**
     * Creates the tag with an empty name.
     *
     * @param value the value of the tag
     */
    public NBTLongArray(final long... value) { this.value = value; }

    public NBTLongArray(final Number[] numbers) {
        this.value = new long[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            this.value[i] = numbers[i].longValue();
        }
    }

    /**
     * Returns the length of this array.
     *
     * @return the length of this array
     */
    public int length() { return this.value.length; }

    @Override
    public long[] getValue() { return this.value; }

    @Override
    public NBTType getType() { return NBTType.LONG_ARRAY; }

    // MISC

    @Override
    public boolean equals(final Object obj) { return obj instanceof NBTLongArray && this.equals((NBTLongArray) obj); }

    public boolean equals(final NBTLongArray tag) { return Arrays.equals(this.value, tag.value); }

    @Override
    public String toMSONString() {
        final StringBuilder stringbuilder = new StringBuilder("[I;");
        for (int i = 0; i < this.value.length; i++) {
            if (i != 0) {
                stringbuilder.append(',');
            }
            stringbuilder.append(this.value[i]);
        }
        return stringbuilder.append(']').toString();
    }

}
