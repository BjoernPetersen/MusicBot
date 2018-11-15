package com.github.bjoernpetersen.musicbot;

import javax.annotation.Nonnull;

public interface Testable<T> {

    @Nonnull
    T createValue();
}
