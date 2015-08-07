package eu.swdev.akka.test.conf;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a value that might be invalid.
 *
 * @param <A> the type of the contained value.
 */
public abstract class CheckedValue<A, E> {

  /**
   * Constructs a valid value.
   *
   * @param <A> the type of the contained value
   * @param a optional; the contained value
   * @return required
   */
  public static <A, E> CheckedValue<A, E> just(A a) {
    return new ValidValue<>(a);
  }

  /**
   * Constructs an invalid value.
   *
   * @param <A> the type of the invalid value
   * @param error describes the error
   * @return required
   */
  public static <A, E> CheckedValue<A, E> error(E error) {
    return new ErrorValue<>(Collections.singletonList(error));
  }

  //
  //
  //

  /**
   * Folds this CheckedValue into a result.
   *
   * (see: &quot;tell don't ask&quot; principle)
   *
   * @param <X> the result type
   * @param onValid process a valid value
   * @param onError process an invalid value
   * @return optional
   */
  public abstract <X> X fold(Function<A, X> onValid, Function<List<E>, X> onError);

  /**
   * Performs a check on this checked value and returns a corresponding
   * result.
   *
   * If this checked value is invalid then no additional check can be performed
   * and this checked value is returned itself. Otherwise this checked value
   * is valid and the additional check can be performed. Depending on the outcome
   * either this checked value or a new invalid checked value is returned.
   *
   * @param predicate required; the check to perform
   * @param error required; the error that describes the error
   * kind of error
   * @return required
   */
  public abstract CheckedValue<A, E> check(Predicate<A> predicate, E error);

  public abstract CheckedValue<A, E> withDefault(A def);

  //
  //
  //

  private static class ValidValue<A, E> extends CheckedValue<A, E> {
    private final A a;
    public ValidValue(A a) {
      this.a = a;
    }
    @Override
    public <X> X fold(Function<A, X> onValid, Function<List<E>, X> onError) {
      return onValid.apply(a);
    }

    @Override
    public CheckedValue<A, E> check(Predicate<A> predicate, E error) {
      if (predicate.test(a)) {
        return this;
      } else {
        return new ErrorValue<>(Collections.singletonList(error));
      }
    }

    @Override
    public CheckedValue<A, E> withDefault(A def) {
      if (a == null) {
        return new ValidValue<>(def);
      } else {
        return this;
      }
    }

  }

  private static class ErrorValue<A, E> extends CheckedValue<A, E> {
    private final List<E> errors;

    public ErrorValue(List<E> errors) {
      this.errors = errors;
    }

    @Override
    public <X> X fold(Function<A, X> onValid, Function<List<E>, X> onError) {
      return onError.apply(errors);
    }

    @Override
    public CheckedValue<A, E> check(Predicate<A> predicate, E error) {
      return this;
    }

    @Override
    public CheckedValue<A, E> withDefault(A def) { return this; }

  }

}
