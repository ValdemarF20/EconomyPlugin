package net.foster.testing.orbital.utils;

public class Errors {
    /**
     * Sneakily throw a checked exception as an unchecked exception
     *
     * @param e   the exception to throw
     * @param <E> the type of the exception
     * @throws E the exception
     */
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        //noinspection unchecked
        throw (E) e;
    }
}
