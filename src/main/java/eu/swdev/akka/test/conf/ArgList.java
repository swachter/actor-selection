package eu.swdev.akka.test.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a list of arguments that can be supplied to a function.
 *
 * The basic idea is that so called CheckedValues are repeatedly added to argument lists
 * that collect the argument values and capture their types. The types are captured by
 * incrementally constructing a function type that corresponds to the types of the supplied
 * values.
 *
 * Example: An instance of type T is to be build using
 * arguments to which a CheckedValue of type String and a CheckedValue of type Boolean
 * was added.  The incrementally constructed function type is:
 * {@code Function<Boolean, Function<String, T>>}
 *
 * A lambda expression that can be used for that function type is:
 * {@code bool -> string -> <some code>} where the code may use the {@code bool}
 * and {@code string} parameters and must return a {@code T}.
 *
 * After all arguments have been collected they can be used as arguments
 * for a supplied function. Before the function evaluation can
 * take place however, it is checked if any argument was invalid. In that case
 * the supplied onError function is called.
 *
 * @param <T> the result type when the arguement list is finally supplied to a function
 * @param <E> the type of collected errors
 * @param <F> the captured function type that must be supplied to the apply method
 */
public abstract class ArgList<T, E, F> {

  public static <T, A, E> ArgList<T, E, Function<A, T>> create(CheckedValue<A, E> value) {
    return value.fold(a -> new FirstArg<>(a), es -> new FirstArg<>(es));
  }

  public static <T, A, E> ArgList<T, E, Function<A, T>> create(Supplier<CheckedValue<A, E>> supplier) {
    return create(supplier.get());
  }

  // Implementation note:
  //
  // The basic idea of argument handling is based on a technique called
  // CURRYING: every function which takes multiple arguments can be translated
  // into a sequence of function applications that all take only one argument. In
  // lambda expression syntax: (a, b, c) -> f(a,b,c) can be translated into
  // a -> b -> c -> f(a, b, c).


  /**
   * Returns a result based on the collected values and the supplied
   * functions.
   *
   * If all collected arguments are valid then the supplied function is evaluated
   * using the argument values. Otherwise the supplied onError function is evaluated.
   *
   * @param onValid required; a function whose signature matches the types of the collected
   * arguments
   * @return required; either the result from evaluating the supplied function
   * or the onError function.
   */
  public abstract T apply(F onValid, Function<List<E>, T> onError);

  public abstract <A> ArgList<T, E, Function<A, F>> add(CheckedValue<A, E> value);

  public <A> ArgList<T, E, Function<A, F>> add(Supplier<CheckedValue<A, E>> supplier) {
    return add(supplier.get());
  }

  /**
   * Represents either an argument list with exactly one argument or an argument list with one argument followed by another
   * argument list.
   *
   * @param <T>
   * @param <E>
   * @param <F>
   * @param <A>
   */
  private static abstract class ArgListImpl<T, E, F, A> extends ArgList<T, E, F> {

    // required; the errors of the complete arguemnts collection
    private final List<E> errors;

    // optional
    protected final A arg;

    public ArgListImpl(List<E> errors, A arg) {
      this.errors = errors;
      this.arg = arg;
    }

    @Override
    public T apply(F onValid, Function<List<E>, T> onError) {
      if (errors.isEmpty()) {
        // if there are no errors then recursively evaluate the supplied
        // curried function
        return doApply(onValid);
      } else {
        return onError.apply(errors);
      }
    }

    protected abstract T doApply(F f);

    @Override
    public <A> ArgList<T, E, Function<A, F>> add(CheckedValue<A, E> value) {
      return value.fold(
          a -> new NextArg<>(errors, a, this),
          e -> {
            // append the new errors to the already present errors
            List<E> errs = new ArrayList<>(errors);
            errs.addAll(e);
            return new NextArg<>(errs, null, this);
          }
      );
    }

  }

  /**
   * Represent an argument list with exactly one argument.
   *
   * Note the initial function type: {@code Function<A, T>}.
   *
   * @param <T> the result type when the arguement list is finally supplied to a function
   * @param <E> the type of collected errors
   * @param <A> the type of the argument value
   */
  private static class FirstArg<T, E, A> extends ArgListImpl<T, E, Function<A, T>, A> {

    public FirstArg(A arg) {
      super(Collections.emptyList(), arg);
    }

    public FirstArg(List<E> errors) {
      super(errors, null);
    }

    @Override
    protected T doApply(Function<A, T> f) {
      return f.apply(arg);
    }

  }

  /**
   * Represents an argument list with one argument followed by another argument list.
   *
   * @param <T> the result type when the arguement list is finally supplied to a function
   * @param <E> the type of collected errors
   * @param <A> the type of the argument value
   * @param <NF> the type of the function that is required by the nested argument list
   */
  private static class NextArg<T, E, A, NF> extends ArgListImpl<T, E, Function<A, NF>, A> {

    private final ArgListImpl<T, E, NF, ?> nestedArgs;

    public NextArg(List<E> errors, A arg, ArgListImpl<T, E, NF, ?> nestedArgs) {
      super(errors, arg);
      this.nestedArgs = nestedArgs;
    }

    @Override
    protected T doApply(Function<A, NF> f) {
      NF nestedFunction = f.apply(arg);
      return nestedArgs.doApply(nestedFunction);
    }

  }

}
