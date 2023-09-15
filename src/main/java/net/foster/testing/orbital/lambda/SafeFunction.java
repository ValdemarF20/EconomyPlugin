package net.foster.testing.orbital.lambda;

import net.foster.testing.orbital.utils.Errors;

import java.util.function.Function;

/* Credit: https://github.com/knightzmc/mittenlib/tree/master/core/src/main/java/me/bristermitten/mittenlib/util/lambda */

/**
 * A {@link Function} that can throw a checked exception.
 *
 * @param <T> the type of the input
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface SafeFunction<T, R> {
    /**
     * A {@link SafeFunction} that always returns the same value, ignoring the input.
     *
     * @param r   the value to return
     * @param <T> the type of the input
     * @param <R> the type of the result
     * @return a {@link SafeFunction} that always returns the given value
     */
    static <T, R> SafeFunction<T, R> constant(R r) {
        return unused -> r;
    }

    /**
     * Wrap a {@link Function} in a {@link SafeFunction}
     *
     * @param function the function to wrap
     * @param <T>      the type of the input
     * @param <R>      the type of the result
     * @return a {@link SafeFunction} that delegates to the given function
     */
    static <T, R> SafeFunction<T, R> of(Function<T, R> function) {
        return function::apply;
    }

    /**
     * Apply the function, possibly throwing an exception.
     *
     * @param t the input
     * @return the result
     */
    R apply(T t) throws Exception;

    /**
     * Turn this {@link SafeFunction} into a {@link Function} that sneaky throws any exceptions.
     *
     * @return a {@link Function} that sneaky throws any exceptions
     */
    default Function<T, R> asFunction() {
        return t -> {
            try {
                return apply(t);
            } catch (Exception e) {
                Errors.sneakyThrow(e);
                return null;
            }
        };
    }
}
